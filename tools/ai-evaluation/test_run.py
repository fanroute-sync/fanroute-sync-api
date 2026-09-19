#!/usr/bin/env python3
"""Focused tests for the offline evaluator and summary calculations."""
import importlib.util
import json
from pathlib import Path
import tempfile
import unittest


RUN_PATH = Path(__file__).with_name('run.py')
SPEC = importlib.util.spec_from_file_location('ai_evaluation_run', RUN_PATH)
run = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(run)


def input_data(**overrides):
    data = {
        'date': '2026-10-01',
        'arrivalAt': '2026-10-01T04:00:00Z',
        'departureAt': '2026-10-01T12:00:00Z',
        'travelIntensity': 'RELAXED',
        'companions': [],
        'travelMbti': '맛집탐방형',
        'preferences': [],
        'fixedItems': [],
        'placeCandidates': [
            {'id': 101, 'name': '공원', 'address': '부산', 'tags': ['PARK'],
             'latitude': 35.16, 'longitude': 129.16},
            {'id': 103, 'name': '식당', 'address': '부산', 'tags': ['FOOD'],
             'latitude': 35.164, 'longitude': 129.159},
        ],
        'accommodationAnchor': {'latitude': 35.16, 'longitude': 129.16},
    }
    data.update(overrides)
    return data


def item(time='13:00', duration=60, place_id=101, title='일정'):
    return {'time': time, 'durationMinutes': duration, 'placeId': place_id, 'title': title}


class EvaluateTest(unittest.TestCase):
    def test_korea_time_window_accepts_both_boundaries(self):
        data = input_data()
        # 04:00Z and 12:00Z are 13:00 and 21:00 in Asia/Seoul.
        problems, _, _ = run.evaluate(data, [item('13:00'), item('20:00', place_id=103)])
        self.assertEqual(problems, [])

        problems, _, _ = run.evaluate(data, [item('20:00', 61)])
        self.assertIn('travel_window', problems)

    def test_existing_and_generated_overlap_use_end_boundary_as_open(self):
        data = input_data(fixedItems=[
            {'scheduledTime': '14:00', 'durationMinutes': 60},
        ])
        problems, _, _ = run.evaluate(data, [item('15:00')])
        self.assertNotIn('existing_overlap', problems)

        problems, _, _ = run.evaluate(data, [item('13:30')])
        self.assertIn('existing_overlap', problems)

        problems, _, _ = run.evaluate(data, [item('13:00', 90), item('14:00', 60, 103)])
        self.assertIn('generated_overlap', problems)

    def test_unknown_duration_fixed_item_only_protects_its_start(self):
        for duration in (None, -10):
            with self.subTest(duration=duration):
                data = input_data(fixedItems=[
                    {'scheduledTime': '14:00', 'durationMinutes': duration},
                ])
                problems, _, _ = run.evaluate(data, [item('13:00')])
                self.assertNotIn('existing_overlap', problems)
                problems, _, _ = run.evaluate(data, [item('14:00')])
                self.assertIn('existing_overlap', problems)

    def test_fixed_time_keeps_seconds_at_end_boundary(self):
        data = input_data(fixedItems=[
            {'scheduledTime': '14:00:30', 'durationMinutes': None},
        ])
        problems, _, _ = run.evaluate(data, [item('13:00')])
        self.assertNotIn('existing_overlap', problems)
        problems, _, _ = run.evaluate(data, [item('13:01')])
        self.assertIn('existing_overlap', problems)

    def test_invalid_id_duration_and_relaxed_count_are_reported(self):
        data = input_data()
        problems, _, _ = run.evaluate(data, [item('13:00', 0, 999)])
        self.assertIn('unknown_place', problems)
        self.assertIn('duration', problems)

        problems, _, _ = run.evaluate(data, [item('13:00'), item('14:00', place_id=103),
                                             item('15:00'), item('16:00')])
        self.assertIn('item_count', problems)

    def test_baseline_reconstructs_old_prompt_and_schema_without_thinking_config(self):
        body = run.baseline(input_data())
        prompt = body['contents'][0]['parts'][0]['text']
        self.assertIn('고정 일정: []', prompt)
        self.assertIn('id=101, name=공원, address=부산', prompt)
        self.assertNotIn('thinkingConfig', body['generationConfig'])
        self.assertEqual(body['generationConfig']['responseFormat']['text']['mimeType'],
                         'APPLICATION_JSON')
        self.assertEqual(body['generationConfig']['responseFormat']['text']['schema']['properties']
                         ['items']['items']['properties']['durationMinutes']['type'], 'integer')


class SummaryTest(unittest.TestCase):
    def test_summary_uses_weighted_denominators_and_observed_scenarios(self):
        data = input_data()
        rows = [
            {'scenario': 'one', 'variant': 'baseline', 'httpStatus': 200,
             'elapsedMs': 10, 'usage': {'totalTokenCount': 5}, 'violations': [],
             'items': [item('13:00'), item('14:00', place_id=None)],
             'straightLineKm': 0.1, 'tagMatchRate': 0.0},
            {'scenario': 'two', 'variant': 'baseline', 'httpStatus': 200,
             'elapsedMs': 20, 'usage': {'totalTokenCount': 7},
             'violations': ['travel_window'], 'items': [item('13:00', place_id=103)],
             'straightLineKm': 0.2, 'tagMatchRate': 1.0},
            {'scenario': 'three', 'variant': 'baseline', 'httpStatus': 429,
             'elapsedMs': 30, 'usage': {}, 'violations': ['http_error'], 'items': None},
            {'scenario': 'four', 'variant': 'baseline', 'httpStatus': 200,
             'elapsedMs': 40, 'usage': {}, 'violations': ['transport_or_parse_error'],
             'items': None},
        ]
        with tempfile.TemporaryDirectory() as directory:
            output = Path(directory)
            requests = [
                {'scenario': row['scenario'], 'variant': row['variant'], 'input': data, 'body': {}}
                for row in rows
            ]
            (output / 'requests.json').write_text(json.dumps(requests))
            (output / 'results.jsonl').write_text('\n'.join(json.dumps(row) for row in rows) + '\n')
            table = run.summarize(output)

            self.assertEqual(len(table), 1)
            summary = table[0]
            self.assertEqual(summary['requests'], 4)
            self.assertEqual(summary['apiSuccess'], 3)
            self.assertEqual(summary['meanMs'], 25)
            self.assertEqual(summary['p50Ms'], 25)
            self.assertEqual(summary['p95Ms'], 38.5)
            self.assertEqual(summary['totalTokens'], 12)
            self.assertEqual(summary['constraintDenominator'], 2)
            self.assertEqual(summary['tagMatchDenominator'], 2)
            self.assertEqual(summary['tagMatchCount'], 1)
            self.assertEqual(summary['tagMatchRate'], 0.5)
            self.assertEqual(summary['customCount'], 1)
            self.assertEqual(summary['customDenominator'], 3)
            self.assertEqual(summary['customRate'], round(1 / 3, 4))
            self.assertEqual(summary['straightLineKmDenominator'], 2)
            self.assertEqual(summary['straightLineKm'], 0.15)
            self.assertEqual(summary['compliance_travel_window'], 0.5)
            markdown = (output / 'summary.md').read_text()
            self.assertIn('4개 실행 행과 4개 시나리오', markdown)
            self.assertNotIn('20개 입력', markdown)


if __name__ == '__main__':
    unittest.main()
