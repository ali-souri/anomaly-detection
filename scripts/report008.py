"""Compact task-008 JFR/load evidence without modifying any accepted 007 file."""
import csv
import json
from pathlib import Path
from datetime import datetime
from summarize_load import rows_from_console, rows_from_csv, summarize


def counters(lines):
    return {key: int(value) for key, value in (line.split() for line in lines)}


def main():
    raw = Path('docs/performance/008/raw')
    profile_rows = []
    for source in sorted(raw.glob('*-profile.json')):
        metadata = json.loads(source.read_text(encoding='utf-8-sig'))
        run = metadata['run']
        stack = json.loads((raw / f'{run}-stack-analysis.json').read_text())
        before = counters(metadata['cpuBefore'])
        after = counters(metadata['cpuAfter'])
        delta = {key: after[key] - value for key, value in before.items()}
        start = datetime.fromisoformat(metadata['containerStart'].replace('Z', '+00:00'))
        first = datetime.fromisoformat(stack['firstSampleUtc'].replace('Z', '+00:00'))
        last = datetime.fromisoformat(stack['lastSampleUtc'].replace('Z', '+00:00'))
        cpu = stack['cpuSamples']['jdk.ExecutionSample']
        profile_rows.append({'run': run, 'javaSamples': cpu['count'], 'nativeSamples': stack['cpuSamples'].get('jdk.NativeMethodSample', {}).get('count', 0),
                             'exclusiveJavaPercent': cpu['exclusivePercent'], 'inclusiveJavaPercent': cpu['inclusivePercent'],
                             'cpuStatDeltaWholeLoadCall': delta, 'sampleStartAfterContainerSeconds': (first-start).total_seconds(),
                             'sampleEndAfterContainerSeconds': (last-start).total_seconds(), 'recordingBytes': metadata['recordingBytes'],
                             'recordingSha256': metadata['recordingSha256'], 'sampledAllocationWeightBytes': stack['sampledAllocationWeightBytes'],
                             'maximumObservedGcHeapBytes': stack['maximumObservedHeapBytes'], 'wholeContainerMemoryPeakBytes': int(metadata['memoryPeakBytes'])})
    (raw / 'profile-overview.json').write_text(json.dumps(profile_rows, indent=2) + '\n')
    loads = {}
    for source in sorted(raw.glob('*-summary.json')):
        run = source.name.removesuffix('-summary.json')
        meta_path = raw / f'{run}-run.json'
        if not meta_path.exists():
            continue
        meta = json.loads(meta_path.read_text(encoding='utf-8-sig'))
        console = Path('.cache/008/load') / f'{run}-console.txt'
        csv_file = raw / f'{run}-requests.csv'
        rows = rows_from_console(console.read_text(encoding='utf-8-sig')) if console.exists() else rows_from_csv(csv_file)
        metrics = json.loads(source.read_text(encoding='utf-8-sig'))['metrics']
        seconds = int(meta['duration'].removesuffix('s'))
        loads[run] = summarize(rows, metrics, seconds)
        loads[run]['diagnosticProfileRun'] = run.startswith(('before-', 'diagnostic-'))
        with csv_file.open('w', newline='') as output:
            writer = csv.DictWriter(output, fieldnames=['iteration','algorithm','started','completed','status','errorCode','sampled','responseBodyBytes'])
            writer.writeheader(); writer.writerows(rows)
    (raw / 'load-analysis.json').write_text(json.dumps(loads, indent=2) + '\n')
    for name, result in loads.items():
        print(name, result['successful'], result['rejected503'], result['unexpected'], result['successfulScheduledRate'])


if __name__ == '__main__':
    main()
