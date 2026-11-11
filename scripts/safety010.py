"""Safety/selection accounting for the fixed A/B/C runtime experiment."""
import json
from pathlib import Path
from summarize_load import rows_from_console


def overlap(rows):
    points=[(r['started'],1) for r in rows]+[(r['completed'],-1) for r in rows]
    active=maximum=0
    for _,delta in sorted(points):
        active+=delta;maximum=max(maximum,active)
    return maximum


def screen_safe(rows,metrics,samples,stopped=False):
    peak=max((a['peakBytes'] for s in samples for a in s['apps']),default=0)
    issues=[e for s in samples for a in s['apps'] for e in a['memoryEvents'] if int(e.split()[1])]
    return bool(rows) and not stopped and overlap(rows)>=3 and all(r['status']==200 for r in rows) and metrics.get('unexpected_requests',{}).get('count',0)==0 and peak>0 and peak<.95*3*2**30 and not issues and min(s['availableHostKiB'] for s in samples)>=1572864


def screen(run):
    raw=Path('docs/performance/010/raw');meta=json.loads((raw/f'{run}-summary.json').read_text(encoding='utf-8-sig'))['metrics']
    rows=rows_from_console((Path('.cache/010/load')/f'{run}-console.txt').read_text(encoding='utf-8-sig'))
    samples=[json.loads(x) for x in (raw/f'{run}-resources.jsonl').read_text(encoding='utf-8-sig').splitlines()]
    result={'run':run,'safe':screen_safe(rows,meta,samples,(raw/f'{run}-safety-stop.json').exists()),'maximumActualOverlappingRequests':overlap(rows),'successful':sum(r['status']==200 for r in rows),'maximumCgroupPeakBytes':max((a['peakBytes'] for s in samples for a in s['apps']),default=0),'minimumHostAvailableKiB':min(s['availableHostKiB'] for s in samples)}
    (raw/f'{run}-safety.json').write_text(json.dumps(result,indent=2)+'\n',encoding='utf-8');print(result)
    return result['safe']

if __name__=='__main__':
    import sys
    sys.exit(0 if screen(sys.argv[1]) else 2)
