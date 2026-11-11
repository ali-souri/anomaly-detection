"""Resource observations kept separate from successful latency and sampled JFR weights."""
import json
import re
from pathlib import Path
from datetime import datetime
from report008 import counters


def gc_pauses(text, start_ms, end_ms):
    pauses = []
    for line in text.splitlines():
        match = re.search(r'\[([0-9T:.+Z-]+)\].*Pause.* ([0-9.]+)ms$', line)
        if match:
            instant = datetime.fromisoformat(match[1].replace('Z', '+00:00')).timestamp() * 1000
            if start_ms <= instant < end_ms:
                pauses.append(float(match[2]))
    return {'count':len(pauses), 'sumAcrossReplicasMs':sum(pauses), 'maximumPauseMs':max(pauses,default=0)}


def main():
    raw=Path('docs/performance/009/raw')
    loads=json.loads((raw/'load-analysis.json').read_text(encoding='utf-8-sig'))
    results={}
    for run,load in loads.items():
        before_path=raw/f'{run}-before.json'
        after_path=raw/f'{run}-after.json'
        if not before_path.exists() or not after_path.exists():
            continue
        before=json.loads(before_path.read_text(encoding='utf-8-sig'))
        after=json.loads(after_path.read_text(encoding='utf-8-sig'))
        nodes=[]
        for a,b in zip(before['containers'],after['containers']):
            if a['name']!=b['name']:
                raise ValueError('Mismatched cgroup identity')
            first,last=counters(a['cpu']),counters(b['cpu'])
            nodes.append({'name':a['name'],'wholeLoadCallCpuStatDelta':{k:last[k]-v for k,v in first.items()},'cgroupCurrentAfterBytes':int(b['memoryCurrentBytes']),'cgroupPeakWholeContainerLifetimeBytes':int(b['memoryPeakBytes']),'memoryEvents':counters(b['memoryEvents'])})
        samples=[json.loads(line) for line in (raw/f'{run}-resources.jsonl').read_text(encoding='utf-8-sig').splitlines()]
        stats=[row['stats'] for row in samples if 'stats' in row]
        generator=[r for r in stats if r['Name']=='anomaly-009-generator']
        results[run]={'beforeUtc':before['utc'],'afterUtc':after['utc'],'containers':nodes,'minimumAvailableHostKiB':min([before['availableHostKiB'],after['availableHostKiB']]+[r['availableHostKiB'] for r in samples]),'safetyStops':sum(bool(r.get('safetyStop')) for r in samples),'generatorCpuPeakPercent':max([float(r['CPUPerc'].rstrip('%')) for r in generator],default=None),'generatorObservedMemoryUsage':sorted({r['MemUsage'] for r in generator}),'steadyWindowGcPauses':gc_pauses((Path('.cache/009')/f'{run}-server.log').read_text(encoding='utf-8-sig'),load['startedEpochMs'],load['steadyWindowEndEpochMs'])}
    (raw/'resource-analysis.json').write_text(json.dumps(results,indent=2)+'\n',encoding='utf-8')

if __name__=='__main__':
    main()
