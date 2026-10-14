#!/usr/bin/env python3
"""Compare measured audit artifacts; never substitutes timing for legality."""
import argparse
import csv
import json
import math
import statistics
from pathlib import Path


def read(root):
    rows = list(csv.DictReader((root / 'samples.csv').open()))
    summary = {}
    for line in (root / 'summary.txt').read_text().splitlines():
        if '=' in line:
            key, value = line.split('=', 1)
            if value.isdigit():
                summary[key] = int(value)
    return rows, summary


def timings(rows, kind, mode):
    values = sorted(int(row['operation_ns']) / 1_000_000 for row in rows
                    if row['type'] == kind and row['mode'] == mode and row['operation_ns'])
    return {'n': len(values), 'p50_ms': statistics.median(values),
            'p95_ms': values[math.ceil(len(values) * .95) - 1],
            'p99_ms': values[math.ceil(len(values) * .99) - 1]}


def depth_comparison(before_rows, after_rows, after_root):
    historical_file = after_root / 'historical-quality.csv'
    if not historical_file.exists() or not after_rows or 'named_chain_depth' not in after_rows[0]:
        return None
    historical = {r['scenario']: r for r in csv.DictReader(historical_file.open())}
    def key(row):
        return '-'.join(row[k] for k in ('type', 'archetype', 'mode', 'seed')) + '.json'
    original = {key(r): r for r in before_rows}
    groups = []
    for kind in sorted({r['type'] for r in after_rows}):
        for legal_only in (False, True):
            pairs = []
            for row in after_rows:
                if row['type'] != kind or key(row) not in historical:
                    continue
                old = historical[key(row)]
                if legal_only and (int(old['blocked']) or int(original[key(row)]['audit_errors'])):
                    continue
                pairs.append((int(old['named_chain_depth']), int(row['named_chain_depth'])))
            if pairs:
                groups.append({'type': kind, 'historically_legal_only': legal_only, 'n': len(pairs),
                    'before_mean': statistics.mean(a for a, b in pairs),
                    'after_mean': statistics.mean(b for a, b in pairs),
                    'improved': sum(b > a for a, b in pairs),
                    'equal': sum(b == a for a, b in pairs),
                    'decreased': sum(b < a for a, b in pairs)})
    return {'groups': groups,
            'limit': 'Longest named prerequisite chain at final ratings; not a combat effectiveness score.'}


def compare(before, after):
    old, old_summary = read(before)
    new, new_summary = read(after)
    groups = sorted({(r['type'], r['mode']) for r in old})
    measures = []
    for kind, mode in groups:
        a, b = timings(old, kind, mode), timings(new, kind, mode)
        measures.append({'type': kind, 'mode': mode, 'before': a, 'after': b,
                         'p50_change_percent': 100 * (b['p50_ms'] / a['p50_ms'] - 1)})
    changes = {}
    for source in sorted(before.glob('*.json')):
        target = after / source.name
        if not target.exists():
            continue
        a, b = json.loads(source.read_text()), json.loads(target.read_text())
        fields = sorted(k for k in a.keys() | b.keys() if a.get(k) != b.get(k))
        if fields:
            changes[source.name] = fields
    return {'before': old_summary, 'after': new_summary, 'measurements': measures,
            'changed_snapshot_fields': changes,
            'named_chain_depth': depth_comparison(old, new, after),
            'legal_gate_passed': new_summary.get('scenarios') == 540 and all(
                new_summary.get(k) == 0 for k in ('generation_failures', 'audit_errors', 'nondeterministic')),
            'limits': 'Runner wall-clock timings include scheduling/JIT noise; audit is not a complete rules oracle.'}


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('before', type=Path)
    parser.add_argument('after', type=Path)
    parser.add_argument('--output', type=Path, required=True)
    args = parser.parse_args()
    result = compare(args.before, args.after)
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(result, ensure_ascii=False, indent=2) + '\n')
    print(json.dumps({k: result[k] for k in ('before', 'after', 'legal_gate_passed')}, indent=2))
