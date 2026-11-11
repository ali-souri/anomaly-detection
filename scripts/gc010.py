"""Count unique G1 stop-the-world summaries separately from concurrent-cycle elapsed time."""
import json
import re
from pathlib import Path
from datetime import datetime
from collections import Counter

LINE = re.compile(r'\[([^\]]+)\]\[([^\]]+)s\]\[[^\]]+\]\[([^\]]+)\]\s+GC\((\d+)\)\s+(.*?)\s+([\d.]+)ms$')


def gc_accounting(text, start_ms, end_ms):
    events = {}; concurrent = {}; unknown = []; duplicates = 0
    for line in text.splitlines():
        match=LINE.search(line)
        if not match or match[3].strip() != 'gc':
            continue
        epoch=datetime.fromisoformat(match[1].replace('Z','+00:00')).timestamp()*1000
        if not start_ms <= epoch < end_ms:
            continue
        node_match=re.match(r'\s*(app-\d+)\s*\|',line)
        node=node_match[1] if node_match else 'app-1'
        description=match[5]; duration=float(match[6]); key=(node,int(match[4]),description,match[2])
        family=None
        if description.startswith('Pause Young'):
            family='mixed' if '(Mixed)' in description else 'young'
        elif description.startswith('Pause Remark'): family='remark'
        elif description.startswith('Pause Cleanup'): family='cleanup'
        elif description.startswith('Pause Full'): family='full'
        if family:
            key=(node,int(match[4]),family,match[2])
            if key in events: duplicates+=1
            events[key]=(node,family,duration)
        elif re.fullmatch(r'Concurrent (Mark|Undo) Cycle',description):
            concurrent[key]=(node,description,duration)
        elif description.startswith('Pause '):
            unknown.append(line)
    per={}
    for node in sorted({x[0] for x in events.values()}|{x[0] for x in concurrent.values()}):
        pauses=[x for x in events.values() if x[0]==node]
        cycles=[x for x in concurrent.values() if x[0]==node]
        per[node]={'distinctStwPauses':len(pauses),'stwSumMs':sum(x[2] for x in pauses),'stwMaxMs':max([x[2] for x in pauses],default=0),'stwByType':{kind:{'count':sum(x[1]==kind for x in pauses),'sumMs':sum(x[2] for x in pauses if x[1]==kind)} for kind in ['young','mixed','remark','cleanup','full']},'concurrentCycleSummaries':len(cycles),'concurrentCycleElapsedSumMs':sum(x[2] for x in cycles)}
    return {'perJvm':per,'distinctStwPauses':len(events),'crossReplicaStwSumMs':sum(x[2] for x in events.values()),'maximumIndividualStwMs':max([x[2] for x in events.values()],default=0),'concurrentCycleElapsedSumMs':sum(x[2] for x in concurrent.values()),'ignoredDuplicatePauseSummaries':duplicates,'unknownPauseSummaries':unknown}


def audit009():
    raw=Path('docs/performance/009/raw');loads=json.loads((raw/'load-analysis.json').read_text());old=json.loads((raw/'resource-analysis.json').read_text());out={}
    for run,meta in old.items():
        window=loads[run]
        result=gc_accounting((Path('.cache/009')/f'{run}-server.log').read_text(encoding='utf-8-sig'),window['startedEpochMs'],window['steadyWindowEndEpochMs'])
        result['previousAggregate']=meta['steadyWindowGcPauses']
        result['sameCountAndSum']=result['distinctStwPauses']==meta['steadyWindowGcPauses']['count'] and abs(result['crossReplicaStwSumMs']-meta['steadyWindowGcPauses']['sumAcrossReplicasMs'])<.001
        out[run]=result
    Path('docs/performance/010/raw/gc-audit009.json').write_text(json.dumps(out,indent=2)+'\n',encoding='utf-8')
    print([(run,r['sameCountAndSum'],r['distinctStwPauses'],round(r['crossReplicaStwSumMs'],3),round(r['concurrentCycleElapsedSumMs'],3)) for run,r in out.items()])

if __name__=='__main__':
    audit009()
