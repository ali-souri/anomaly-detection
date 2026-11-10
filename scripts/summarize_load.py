"""Compact actual k6 timestamps; keep huge bodies and verbose metrics out of Git."""
import csv
import json
import math
import re
from pathlib import Path


def percentile(values, quantile):
    """Linear interpolation of sorted observed successful-request latencies."""
    ordered = sorted(values)
    if not ordered:
        return None
    position = (len(ordered) - 1) * quantile
    lower = math.floor(position)
    upper = math.ceil(position)
    return ordered[lower] + (ordered[upper] - ordered[lower]) * (position - lower)


def rows_from_console(text):
    rows = []
    for line in text.splitlines():
        match = re.search(r'msg=("(?:\\.|[^"\\])*")', line)
        if match:
            message = json.loads(match[1])
            if message.startswith('{"iteration":'):
                rows.append(json.loads(message))
    return rows


def summarize(rows, metrics, duration_seconds=60):
    if not rows:
        raise ValueError("No actual request timestamps")
    if len({row['iteration'] for row in rows}) != len(rows):
        raise ValueError("Duplicate iteration identity")
    start = min(row['started'] for row in rows)
    end = start + duration_seconds * 1000
    successful = [row for row in rows if row['status'] == 200]
    reported = metrics.get('successful_requests', {}).get('count', 0)
    if reported != len(successful):
        raise ValueError("HTTP 200 and validated-success counters disagree")
    latency = [row['completed'] - row['started'] for row in successful]
    count = len(successful)
    rejects = sum(row['status'] == 503 for row in rows)
    errors = len(rows) - count - rejects
    dropped = metrics.get('dropped_iterations', {}).get('count', 0)
    return {
        'startedEpochMs': start, 'steadyWindowEndEpochMs': end,
        'firstCompletionEpochMs': min(row['completed'] for row in rows),
        'lastCompletionEpochMs': max(row['completed'] for row in rows),
        'durationSeconds': duration_seconds, 'scheduledAndCompleted': len(rows),
        'successful': count, 'rejected503': rejects, 'unexpected': errors,
        'timeouts': sum(row.get('errorCode') == 1050 for row in rows), 'droppedIterations': dropped,
        'successfulScheduledRate': count / duration_seconds,
        'successfulCompletedInWindowRate': sum(row['completed'] < end for row in successful) / duration_seconds,
        'drainMs': max(0, max(row['completed'] for row in rows) - end),
        'averageMs': sum(latency) / count if count else None,
        'p50Ms': percentile(latency, .5), 'p95Ms': percentile(latency, .95), 'p99Ms': percentile(latency, .99),
        'completedSuccessfulPer10s': [sum(start + offset * 1000 <= row['completed'] < start + (offset + 10) * 1000 for row in successful) for offset in range(0, duration_seconds, 10)],
        'inFlightAt10sBoundaries': [sum(row['started'] <= start + offset * 1000 < row['completed'] for row in rows) for offset in range(0, duration_seconds + 1, 10)],
        'actualK6DataSentBytesIncludingWarmupAndHeaders': metrics.get('data_sent', {}).get('count', 0),
        'actualK6DataReceivedBytesIncludingWarmupAndHeaders': metrics.get('data_received', {}).get('count', 0),
        'successfulRequestBodyBytes': metrics.get('successful_request_body_bytes', {}).get('count', 0),
        'sampledFullChecks': metrics.get('sampled_full_correctness', {}).get('passes', 0),
        'sampledFullCheckFailures': metrics.get('sampled_full_correctness', {}).get('fails', 0),
        'sampledActualResponseBodyBytes': sorted({row['responseBodyBytes'] for row in rows if row.get('sampled') and row.get('responseBodyBytes') is not None}),
        'allScheduledSucceeded': count >= 10 * duration_seconds and rejects == 0 and errors == 0 and dropped == 0,
    }


def main():
    raw = Path('docs/load-tests/raw')
    reports = {}
    for summary_path in sorted(raw.glob('*-summary.json')):
        name = summary_path.name.removesuffix('-summary.json')
        console = raw / f'{name}-console.txt'
        if not console.exists():
            continue
        rows = rows_from_console(console.read_text(encoding='utf-8-sig'))
        if not rows:
            continue
        metrics = json.loads(summary_path.read_text(encoding='utf-8-sig'))['metrics']
        seconds = 60 if '-arrival-' in name else 10
        reports[name] = summarize(rows, metrics, seconds)
        with (raw / f'{name}-requests.csv').open('w', newline='', encoding='utf-8') as output:
            fields = ['iteration', 'algorithm', 'started', 'completed', 'status', 'errorCode', 'sampled', 'responseBodyBytes']
            writer = csv.DictWriter(output, fieldnames=fields)
            writer.writeheader()
            writer.writerows(rows)
    (raw / 'analysis.json').write_text(json.dumps(reports, indent=2) + '\n', encoding='utf-8')
    for name, result in reports.items():
        print(name, result['successful'], result['rejected503'], result['unexpected'], result['successfulScheduledRate'], result['allScheduledSucceeded'])


if __name__ == '__main__':
    main()
