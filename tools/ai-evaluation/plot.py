#!/usr/bin/env python3
"""Plot a completed experiment; requires matplotlib, makes no API calls."""
import argparse
import csv
from pathlib import Path

import matplotlib

matplotlib.use('Agg')
import matplotlib.pyplot as plt


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('output', type=Path)
    args = parser.parse_args()
    with (args.output / 'summary.csv').open() as source:
        rows = list(csv.DictReader(source))
    labels = [row['variant'].upper() for row in rows]
    colors = ['#7b8794', '#007f86', '#4769c8']
    fig, axes = plt.subplots(1, 3, figsize=(12, 4.6), layout='constrained')
    fig.suptitle('AI itinerary: constraints, latency and token usage', fontsize=15)
    quality = [100 * int(row['valid']) / int(row['requests']) for row in rows]
    bars = axes[0].bar(labels, quality, color=colors)
    axes[0].bar_label(bars, labels=[f"{q:.1f}%\n({r['valid']}/{r['requests']})"
                                   for q, r in zip(quality, rows)], padding=4)
    axes[0].set(title='All constraints passed', ylabel='Percent of requests', ylim=(0, 120))
    axes[0].set_yticks([0, 25, 50, 75, 100])
    x = list(range(len(rows)))
    for offset, field, label, color in [(-.18, 'p50Ms', 'p50', '#007f86'),
                                       (.18, 'p95Ms', 'p95', '#83c3c7')]:
        bars = axes[1].bar([i + offset for i in x],
                           [float(r[field]) / 1000 for r in rows], .36,
                           label=label, color=color)
        axes[1].bar_label(bars, fmt='%.2f', padding=3)
    axes[1].set_xticks(x, labels)
    axes[1].set(title='HTTP round-trip latency', ylabel='Seconds')
    axes[1].margins(y=.25)
    axes[1].legend(frameon=False)
    bars = axes[2].bar(labels, [int(r['totalTokens']) / int(r['requests']) for r in rows],
                       color=colors)
    axes[2].bar_label(bars, fmt='%.0f', padding=4)
    axes[2].set(title='Mean tokens per request', ylabel='Reported total tokens')
    axes[2].margins(y=.2)
    for axis in axes:
        axis.spines[['top', 'right']].set_visible(False)
        axis.set_axisbelow(True)
        axis.grid(axis='y', alpha=.2)
    fig.supxlabel('Synthetic inputs; repeated observations. HTTP only; excludes queue and DB time.',
                  fontsize=9)
    fig.savefig(args.output / 'comparison.png', dpi=180)
    plt.close(fig)


if __name__ == '__main__':
    main()
