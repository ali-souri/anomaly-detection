"""Summarize sampled JFR stacks without treating samples as wall-clock phase timers."""
import json
import sys
import re
from collections import Counter
from pathlib import Path


def frame_name(frame):
    method = frame.get('method') or {}
    return (method.get('type') or {}).get('name', '<unknown>').replace('/', '.') + '.' + method.get('name', '<unknown>')


def category(name):
    if 'BoundedRequestBodyAdvice.' in name or 'java.io.InputStream.readNBytes' in name:
        return 'raw-body-buffer'
    if 'JsonNodeDeserializer' in name or 'UTF8StreamJsonParser' in name or 'ReaderBasedJsonParser' in name:
        return 'json-tree-creation'
    if 'io.anomaly.api.parser.RequestParser.' in name:
        return 'tree-to-number-list'
    if 'io.anomaly.core.validation.Inputs.copy' in name:
        return 'detector-validation-copy'
    if any(text in name for text in ['TimSort.', 'DualPivotQuicksort.', 'java.util.Arrays.sort', 'java.util.Collections.sort']):
        return 'sort-workspace'
    if 'io.anomaly.core.model.' in name or 'io.anomaly.api.mapper.ResponseMapper.' in name:
        return 'result-construction'
    if 'io.anomaly.core.algorithm.' in name:
        return 'detector-statistics-classification'
    if 'com.fasterxml.jackson.databind.ser.' in name or 'UTF8JsonGenerator.' in name or 'WriterBasedJsonGenerator.' in name:
        return 'json-serialization'
    if any(text in name for text in ['sun.nio.ch.', 'java.net.', 'org.apache.tomcat.util.net.', 'org.apache.coyote.http11.Http11InputBuffer.', 'org.apache.coyote.http11.Http11OutputBuffer.']):
        return 'transport-native-io'
    return None


def stack_groups(frames):
    groups = [category(frame_name(frame)) for frame in frames]
    matched = [group for group in groups if group is not None]
    return (matched[0] if matched else 'unclassified'), set(matched)


def summarize_events(events):
    cpu = {}
    allocation = Counter()
    allocation_classes = Counter()
    allocation_threads = Counter()
    allocation_total = 0
    gc = []
    heap = []
    blocked = Counter()
    active = []
    times = []
    sample_settings = []
    for event in events:
        kind = event['type']
        value = event['values']
        frames = (value.get('stackTrace') or {}).get('frames') or []
        exclusive, inclusive = stack_groups(frames)
        if kind in ['jdk.ExecutionSample', 'jdk.NativeMethodSample']:
            bucket = cpu.setdefault(kind, {'count': 0, 'exclusive': Counter(), 'inclusive': Counter(), 'leafFrames': Counter(), 'threads': Counter()})
            bucket['count'] += 1
            bucket['exclusive'][exclusive] += 1
            bucket['inclusive'].update(inclusive)
            bucket['leafFrames'][frame_name(frames[0]) if frames else '<no stack>'] += 1
            bucket['threads'][(value.get('sampledThread') or {}).get('javaName', '<unknown>')] += 1
            times.append(value['startTime'])
        elif kind == 'jdk.ObjectAllocationSample':
            weight = value['weight']
            allocation_total += weight
            allocation[exclusive] += weight
            allocation_classes[(value.get('objectClass') or {}).get('name', '<unknown>')] += weight
            allocation_threads[(value.get('eventThread') or {}).get('javaName', '<unknown>')] += weight
        elif kind == 'jdk.GarbageCollection':
            gc.append({key: value.get(key) for key in ['startTime', 'name', 'cause', 'duration', 'sumOfPauses', 'longestPause']})
        elif kind == 'jdk.GCHeapSummary':
            heap.append(value['heapUsed'])
        elif kind in ['jdk.ThreadPark', 'jdk.SocketRead', 'jdk.SocketWrite', 'jdk.JavaMonitorEnter']:
            blocked[kind] += 1
        elif kind == 'jdk.ActiveRecording':
            active.append(value)
        elif kind == 'jdk.ActiveSetting' and value.get('name') in ['period', 'throttle', 'stackTrace', 'enabled']:
            sample_settings.append({key: value.get(key) for key in ['id', 'name', 'value']})
    for data in cpu.values():
        data['exclusivePercent'] = {key: 100 * count / data['count'] for key, count in data['exclusive'].items()}
        data['inclusivePercent'] = {key: 100 * count / data['count'] for key, count in data['inclusive'].items()}
        data['leafFrames'] = data['leafFrames'].most_common(15)
        data['threads'] = data['threads'].most_common(12)
    return {'cpuSamples': cpu, 'firstSampleUtc': min(times) if times else None, 'lastSampleUtc': max(times) if times else None,
            'sampledAllocationWeightBytes': allocation_total, 'exclusiveAllocationWeightBytes': dict(allocation),
            'allocationClassesTop': allocation_classes.most_common(12), 'gcEvents': gc,
            'allocationWeightByThread': dict(allocation_threads),
            'minimumObservedHeapBytes': min(heap) if heap else None, 'maximumObservedHeapBytes': max(heap) if heap else None,
            'blockedEventCounts': dict(blocked), 'recordingEvents': active, 'effectiveSampleSettings': sample_settings}


def streamed_events(source):
    """Decode one exported event at a time; verbose JFR metadata never fills host memory."""
    with source.open(encoding='utf-8-sig') as data:
        buffer = data.read(1048576)
        start = re.search(r'"events"\s*:\s*\[', buffer)
        if start is None:
            raise ValueError('Missing JFR event array')
        buffer = buffer[start.end():]
        decoder = json.JSONDecoder()
        while True:
            buffer = buffer.lstrip(' \t\r\n,')
            if buffer.startswith(']'):
                return
            try:
                event, end = decoder.raw_decode(buffer)
            except json.JSONDecodeError:
                more = data.read(1048576)
                if not more:
                    raise ValueError('Incomplete JFR export')
                buffer += more
                if len(buffer) > 16 * 1024 * 1024:
                    raise ValueError('Single JFR event exceeds the bounded analysis buffer')
                continue
            yield event
            buffer = buffer[end:]


def main():
    for source in Path('.cache/008/profiles').glob('*-events.json'):
        output = Path('docs/performance/008/raw') / source.name.replace('-events.json', '-stack-analysis.json')
        if output.exists() and '--force' not in sys.argv:
            continue
        try:
            result = summarize_events(streamed_events(source))
        except ValueError:
            print('Export still incomplete:', source.name)
            continue
        output.write_text(json.dumps(result, indent=2) + '\n', encoding='utf-8')
        print(source.name, {name: (data['count'], data['exclusivePercent']) for name, data in result['cpuSamples'].items()})


if __name__ == '__main__':
    main()
