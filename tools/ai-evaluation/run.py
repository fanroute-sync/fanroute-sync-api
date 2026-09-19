#!/usr/bin/env python3
"""Fixed-scenario Gemini experiment. Uses synthetic data and never persists credentials."""
import argparse
import copy
import csv
import datetime as dt
import json
import math
import os
from pathlib import Path
import random
import re
import statistics
import time
import urllib.error
import urllib.request

VARIANTS = ('baseline', 'minimal', 'low')
TAGS = {'맛집탐방형': {'FOOD'}, '감성사진형': {'PHOTO_SPOT', 'CAFE', 'OCEAN_VIEW'},
        '역사문화형': {'INDOOR'}, '자연힐링형': {'PARK', 'OCEAN_VIEW'},
        '쇼핑집중형': {'INDOOR'}, '카페투어형': {'CAFE'}, '액티비티형': {'PARK'},
        '로컬체험형': {'FOOD'}, '공연몰입형': {'INDOOR'}}
CONSTRAINTS = ('item_count', 'travel_window', 'existing_overlap', 'generated_overlap',
               'duration', 'title', 'unknown_place', 'duplicate_place', 'time_format',
               'invalid_item', 'empty_or_invalid_items', 'finish_reason')


def baseline(data):
    """Reconstruct the prompt/schema from 6186bd1; no thinking override in that version."""
    def java_list(values):
        return '[' + ', '.join(values) + ']'
    candidates = java_list([f"id={p['id']}, name={p['name']}, address={p['address'] or ''}"
                            for p in data['placeCandidates']])
    # Before the change only concert items were supplied; all fixtures use nonconcert days.
    prompt = ('너는 부산 공연 여행 일정을 돕는 전문가다.\n'
              '제공된 후보와 고정 일정만 근거로 판단하고, 사실을 추측하지 마라.\n'
              '응답은 제공된 JSON 스키마만 만족해야 한다.\n\n'
              f"대상 날짜: {data['date']}\n여행 기간: {data['arrivalAt']} ~ {data['departureAt']}\n"
              f"여행 강도: {data['travelIntensity']}\n동행: {java_list(data['companions'])}\n"
              f"여행 MBTI: {data['travelMbti'] or 'null'}\n선호: {java_list(data['preferences'])}\n"
              f'고정 일정: []\n장소 후보: {candidates}\n\n'
              '고정 일정과 겹치지 않는 일반 일정만 생성해라. 고정 일정은 결과에 포함하지 마라.\n'
              '이미 등록된 장소는 후보에서 제외되어 있으므로, 장소 후보를 중복해서 선택하지 마라.\n'
              '장소 후보 중 선택한 장소는 해당 placeId만 넣고, 후보 외 장소는 placeId를 null로 둬라.\n'
              f"일반 일정은 여행 강도에 맞춰 최대 {5 if data['travelIntensity'] == 'TIGHT' else 3}개만 생성해라. "
              'time은 HH:mm, durationMinutes는 1 이상의 정수로 반환해라.')
    item = dict(type='object', properties={'time': {'type': 'string'}, 'title': {'type': 'string'},
                'durationMinutes': {'type': 'integer'}, 'placeId': {'type': ['integer', 'null']}},
                required=['time', 'title', 'durationMinutes', 'placeId'])
    schema = dict(type='object', properties={'items': dict(type='array', items=item)}, required=['items'])
    return dict(contents=[dict(parts=[dict(text=prompt)])],
                generationConfig=dict(responseFormat=dict(text=dict(mimeType='APPLICATION_JSON', schema=schema))))


def minute(value):
    parts = value.split(':')
    hour, minutes = map(int, parts[:2])
    seconds = float(parts[2]) if len(parts) > 2 else 0
    return hour * 60 + minutes + seconds / 60


def haversine(a, b):
    lat1, lat2 = math.radians(a['latitude']), math.radians(b['latitude'])
    dlat = lat2 - lat1
    dlon = math.radians(b['longitude'] - a['longitude'])
    h = math.sin(dlat/2)**2 + math.cos(lat1)*math.cos(lat2)*math.sin(dlon/2)**2
    return 6371 * 2 * math.asin(min(1, math.sqrt(h)))


def evaluate(data, items):
    problems = set()
    if not isinstance(items, list) or not items:
        return ['empty_or_invalid_items'], None, None
    maximum = 5 if data['travelIntensity'] == 'TIGHT' else 3
    if len(items) > maximum:
        problems.add('item_count')
    day = dt.datetime.fromisoformat(data['date']).replace(tzinfo=dt.timezone(dt.timedelta(hours=9)))
    lower = max(0, (dt.datetime.fromisoformat(data['arrivalAt'].replace('Z', '+00:00')) - day).total_seconds()/60)
    upper = min(1440, (dt.datetime.fromisoformat(data['departureAt'].replace('Z', '+00:00')) - day).total_seconds()/60)
    candidates = {p['id']: p for p in data['placeCandidates']}
    intervals, ids, route = [], [], []
    if data['accommodationAnchor']:
        route.append(data['accommodationAnchor'])
    for item in items:
        if not isinstance(item, dict):
            problems.add('invalid_item'); continue
        stamp, duration, title, place_id = (item.get(k) for k in ('time', 'durationMinutes', 'title', 'placeId'))
        if not isinstance(title, str) or not title.strip() or len(title) > 200:
            problems.add('title')
        if place_id is not None:
            if type(place_id) is not int or place_id not in candidates:
                problems.add('unknown_place')
            else:
                ids.append(place_id)
        if not isinstance(stamp, str) or not re.fullmatch(r'(?:[01][0-9]|2[0-3]):[0-5][0-9]', stamp):
            problems.add('time_format'); continue
        if type(duration) is not int or not 1 <= duration <= 720:
            problems.add('duration'); continue
        start, end = minute(stamp), minute(stamp) + duration
        intervals.append((start, end, place_id))
        if start < lower or end > upper:
            problems.add('travel_window')
        for fixed in data['fixedItems']:
            if not fixed['scheduledTime']:
                continue
            fixed_start = minute(fixed['scheduledTime'])
            fixed_duration = fixed['durationMinutes']
            overlap = ((start <= fixed_start < end)
                       if fixed_duration is None or fixed_duration <= 0
                       else (start < fixed_start + fixed_duration and fixed_start < end))
            if overlap:
                problems.add('existing_overlap')
    if len(ids) != len(set(ids)):
        problems.add('duplicate_place')
    ordered = sorted(intervals, key=lambda x: x[0])
    if any(a[1] > b[0] for a, b in zip(ordered, ordered[1:])):
        problems.add('generated_overlap')
    route += [candidates[p] for _, _, p in ordered if type(p) is int and p in candidates]
    distance = sum(haversine(a, b) for a, b in zip(route, route[1:])) if len(route) > 1 else None
    relevant = TAGS.get(data['travelMbti'], set())
    tag_rate = sum(bool(set(candidates[p]['tags']) & relevant) for p in ids)/len(ids) if ids and relevant else None
    return sorted(problems), distance, tag_rate


def percentile(values, q):
    ordered = sorted(values)
    index = (len(ordered)-1)*q
    lo = math.floor(index)
    hi = math.ceil(index)
    return ordered[lo] + (ordered[hi]-ordered[lo])*(index-lo)


def _metric_values(row, input_data):
    """Return weighted metric counts for one successfully parsed response."""
    items = row.get('items')
    if not isinstance(items, list):
        return dict(tag_matches=0, tag_denominator=0, distance=None,
                    custom_count=0, item_denominator=0)
    candidates = {p['id']: p for p in input_data.get('placeCandidates', [])}
    relevant = TAGS.get(input_data.get('travelMbti'), set())
    tag_matches = tag_denominator = custom_count = 0
    route = []
    item_denominator = 0
    for item in items:
        if not isinstance(item, dict):
            continue
        if 'placeId' not in item:
            continue
        place_id = item.get('placeId')
        item_denominator += 1
        custom_count += place_id is None
        if type(place_id) is int and place_id in candidates:
            item_tags = set(candidates[place_id].get('tags') or [])
            if relevant:
                tag_denominator += 1
                tag_matches += bool(item_tags & relevant)
            route.append((item.get('time'), candidates[place_id]))
    route.sort(key=lambda pair: pair[0] if isinstance(pair[0], str) else '')
    distance = None
    if input_data.get('accommodationAnchor'):
        route_places = [input_data['accommodationAnchor']]
    else:
        route_places = []
    route_places += [place for _, place in route]
    if len(route_places) > 1:
        distance = sum(haversine(a, b) for a, b in zip(route_places, route_places[1:]))
    if isinstance(row.get('straightLineKm'), (int, float)):
        distance = row['straightLineKm']
    return dict(tag_matches=tag_matches, tag_denominator=tag_denominator,
                distance=distance, custom_count=custom_count,
                item_denominator=item_denominator)


def summarize(output):
    rows = [json.loads(line) for line in (output/'results.jsonl').read_text().splitlines()]
    requests_path = output/'requests.json'
    inputs = {}
    if requests_path.exists():
        for request in json.loads(requests_path.read_text()):
            inputs[(request.get('scenario'), request.get('variant'))] = request.get('input', {})
    observed_set = {r.get('variant') for r in rows if r.get('variant')}
    observed_variants = [variant for variant in VARIANTS if variant in observed_set]
    observed_variants += [variant for variant in dict.fromkeys(r.get('variant') for r in rows)
                          if variant and variant not in observed_set.intersection(VARIANTS)]
    table = []
    for variant in observed_variants:
        group = [r for r in rows if r['variant'] == variant]
        if not group:
            continue
        times = [r['elapsedMs'] for r in group]
        metric_values = [_metric_values(r, inputs.get((r.get('scenario'), variant), {}))
                         for r in group]
        tag_matches = sum(m['tag_matches'] for m in metric_values)
        tag_denominator = sum(m['tag_denominator'] for m in metric_values)
        distances = [m['distance'] for m in metric_values if m['distance'] is not None]
        custom_count = sum(m['custom_count'] for m in metric_values)
        item_denominator = sum(m['item_denominator'] for m in metric_values)
        constraint_denominator = sum(
            r.get('httpStatus') == 200
            and 'transport_or_parse_error' not in (r.get('violations') or [])
            for r in group)
        compliance = {f'compliance_{name}': round(
            (constraint_denominator - sum(name in (r.get('violations') or []) for r in group))
            / constraint_denominator, 4) if constraint_denominator else None
            for name in CONSTRAINTS}
        table.append(dict(variant=variant, requests=len(group), apiSuccess=sum(r['httpStatus']==200 for r in group),
            valid=sum(not r['violations'] for r in group), meanMs=round(statistics.mean(times),2),
            p50Ms=round(statistics.median(times),2), p95Ms=round(percentile(times,.95),2),
            promptTokens=sum(r['usage'].get('promptTokenCount',0) for r in group),
            outputTokens=sum(r['usage'].get('candidatesTokenCount',0) for r in group),
            thoughtsTokens=sum(r['usage'].get('thoughtsTokenCount',0) for r in group),
            totalTokens=sum(r['usage'].get('totalTokenCount',0) for r in group),
            tagMatchRate=round(tag_matches/tag_denominator, 4) if tag_denominator else None,
            tagMatchCount=tag_matches, tagMatchDenominator=tag_denominator,
            straightLineKm=round(statistics.mean(distances), 4) if distances else None,
            straightLineKmDenominator=len(distances),
            customRate=round(custom_count/item_denominator, 4) if item_denominator else None,
            customCount=custom_count, customDenominator=item_denominator,
            constraintDenominator=constraint_denominator, **compliance))
    with (output/'summary.csv').open('w') as f:
        writer=csv.DictWriter(f,fieldnames=list(table[0])); writer.writeheader(); writer.writerows(table)
    lines=['# AI 일정 생성 비교 실험', '',
           '동일 합성 시나리오를 고정 seed로 섞어 순차 호출한 결과다. 운영 서비스의 before/after 실측이 아니다.', '',
           '| 조건 | 요청 | HTTP 성공 | 전체 제약 준수 | 평균 ms | p50 ms | p95 ms | 총 토큰 |',
           '|---|---:|---:|---:|---:|---:|---:|---:|']
    for r in table:
        lines.append(f"| {r['variant']} | {r['requests']} | {r['apiSuccess']} | {r['valid']} | {r['meanMs']} | {r['p50Ms']} | {r['p95Ms']} | {r['totalTokens']} |")
    lines += ['', '지연은 HTTP 왕복과 응답 본문 수신 시간이며 앱 큐·DB·화면 반영은 제외한다. 실패 요청도 지연 집계에 포함한다.',
              '제약 준수는 시간 범위·겹침·개수·체류시간·제목·후보 ID·중복·정상 종료를 모두 통과한 요청 수다.',
              f'이번 요약에는 총 {len(rows)}개 실행 행과 '
              f'{len({r.get("scenario") for r in rows})}개 시나리오가 포함된다. 독립적인 실제 사용자 만족도 결과로 해석할 수 없다.',
              '태그 적합률·직선 경로·CUSTOM 비율은 각각 유효한 장소 선택·거리 계산·일정 항목을 분모로 한 보조 지표다. '
              '실제 도로 이동시간·영업시간·사용자 만족도는 검증하지 않았다.', '',
              '## 보조 지표 및 제약 준수율', '',
              '| 조건 | 태그 적합 | 태그 분모 | 직선 경로 평균 km | 거리 분모 | CUSTOM 비율 | CUSTOM 분모 | 제약 분모 |',
              '|---|---:|---:|---:|---:|---:|---:|---:|']
    for r in table:
        lines.append(f"| {r['variant']} | {r['tagMatchRate']} | {r['tagMatchDenominator']} | "
                     f"{r['straightLineKm']} | {r['straightLineKmDenominator']} | "
                     f"{r['customRate']} | {r['customDenominator']} | {r['constraintDenominator']} |")
    lines += ['', '제약별 준수율은 HTTP 200이면서 transport_or_parse_error가 없는 평가 가능 응답을 분모로 계산한다.', '',
              '| 조건 | 제약 | 준수율 |', '|---|---|---:|']
    for r in table:
        for name in CONSTRAINTS:
            lines.append(f"| {r['variant']} | {name} | {r[f'compliance_{name}']} |")
    lines += ['', '태그 적합률과 직선 경로는 보조 지표다. 실제 도로 이동시간·영업시간·사용자 만족도는 검증하지 않았다.', '',
              '## 위반 유형 (한 요청에 여러 유형 가능)', '', '| 조건 | 유형 | 건수 |', '|---|---|---:|']
    for variant in observed_variants:
        counts={}
        for row in rows:
            if row['variant']==variant:
                for v in row['violations']: counts[v]=counts.get(v,0)+1
        for name,n in sorted(counts.items()): lines.append(f'| {variant} | {name} | {n} |')
    (output/'summary.md').write_text('\n'.join(lines)+'\n')
    return table


def main():
    parser=argparse.ArgumentParser()
    parser.add_argument('--fixtures',default='build/ai-evaluation/requests.json')
    parser.add_argument('--output',required=True)
    parser.add_argument('--repeats',type=int,default=3)
    parser.add_argument('--seed',type=int,default=97)
    parser.add_argument('--delay-seconds',type=float,default=5.0)
    parser.add_argument('--env-file',type=Path)
    parser.add_argument('--summarize',action='store_true')
    args=parser.parse_args()
    if args.delay_seconds < 0:
        parser.error('--delay-seconds must be nonnegative')
    output=Path(args.output)
    if args.summarize:
        print(json.dumps(summarize(output),ensure_ascii=False)); return
    key=os.environ.get('GEMINI_API_KEY')
    if not key and args.env_file:
        for line in args.env_file.read_text().splitlines():
            if line.startswith('GEMINI_API_KEY='):
                key=line.split('=',1)[1].strip().strip('\"\'')
    if not key:
        raise SystemExit('GEMINI_API_KEY is required')
    if (output/'results.jsonl').exists():
        raise SystemExit('Choose a fresh output directory; existing results are never overwritten')
    fixtures=json.loads(Path(args.fixtures).read_text())
    model=os.environ.get('GEMINI_MODEL','gemini-3.5-flash-lite')
    output.mkdir(parents=True,exist_ok=True)
    requests=[]
    for fixture in fixtures:
        for variant in VARIANTS:
            body=baseline(fixture['input']) if variant=='baseline' else copy.deepcopy(fixture['request'])
            if variant!='baseline': body['generationConfig']['thinkingConfig']['thinkingLevel']=variant.upper()
            requests.append(dict(scenario=fixture['name'],variant=variant,input=fixture['input'],body=body))
    (output/'requests.json').write_text(json.dumps(requests,ensure_ascii=False,indent=2)+'\n')
    manifest=dict(model=model,baselineCommit='6186bd1',seed=args.seed,repeats=args.repeats,scenarios=len(fixtures),
                  plannedRequests=len(requests)*args.repeats,startedAt=dt.datetime.now(dt.timezone.utc).isoformat(),
                  timeoutSeconds=30,concurrency=1,delaySeconds=args.delay_seconds)
    (output/'manifest.json').write_text(json.dumps(manifest,indent=2)+'\n')
    jobs=[(request, repetition) for repetition in range(1,args.repeats+1) for request in requests]
    random.Random(args.seed).shuffle(jobs)
    endpoint=f'https://generativelanguage.googleapis.com/v1beta/models/{model}:generateContent'
    for index,(request,repetition) in enumerate(jobs,1):
        row=dict(index=index,scenario=request['scenario'],variant=request['variant'],repetition=repetition,
                 at=dt.datetime.now(dt.timezone.utc).isoformat(),httpStatus=0,usage={},items=None,violations=[])
        started=time.perf_counter()
        try:
            req=urllib.request.Request(endpoint,data=json.dumps(request['body']).encode(),
                headers={'Content-Type':'application/json','x-goog-api-key':key})
            with urllib.request.urlopen(req,timeout=30) as response:
                raw=response.read(); row['httpStatus']=response.status
            row['elapsedMs']=round((time.perf_counter()-started)*1000,2)
            payload=json.loads(raw)
            row['usage']=payload.get('usageMetadata',{})
            row['modelVersion']=payload.get('modelVersion')
            candidate=(payload.get('candidates') or [{}])[0]
            row['finishReason']=candidate.get('finishReason')
            text=''.join(p.get('text','') for p in candidate.get('content',{}).get('parts',[]))
            row['rawText']=text
            row['items']=json.loads(text).get('items')
            row['violations'],row['straightLineKm'],row['tagMatchRate']=evaluate(request['input'],row['items'])
            if row['finishReason']!='STOP': row['violations'].append('finish_reason')
        except urllib.error.HTTPError as error:
            row['httpStatus']=error.code; row['violations']=['http_error']
            # Upstream error bodies are untrusted and may echo request credentials.
            row['error']=f'HTTP {error.code}'
        except Exception as error:
            row['violations']=['transport_or_parse_error']; row['error']=type(error).__name__
        row.setdefault('elapsedMs',round((time.perf_counter()-started)*1000,2))
        with (output/'results.jsonl').open('a') as f: f.write(json.dumps(row,ensure_ascii=False)+'\n')
        if index%10==0 or row['httpStatus']!=200:
            print(f"{index}/{len(jobs)} {row['variant']} {row['scenario']} http={row['httpStatus']} ms={row['elapsedMs']} violations={row['violations']}",flush=True)
        if index%30==0: summarize(output)
        time.sleep(args.delay_seconds)
    manifest['finishedAt']=dt.datetime.now(dt.timezone.utc).isoformat()
    (output/'manifest.json').write_text(json.dumps(manifest,indent=2)+'\n')
    print(json.dumps(summarize(output),ensure_ascii=False),flush=True)


if __name__=='__main__':
    main()
