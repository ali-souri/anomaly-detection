import unittest
from gc010 import gc_accounting
from datetime import datetime

class GcAccountingTest(unittest.TestCase):
    def test_summaries_are_deduplicated_and_start_phase_and_concurrent_elapsed_are_separate(self):
        base='app-1 | [2026-11-11T05:00:01.000+0000][1.000s][info]'
        young=base+'[gc] GC(1) Pause Young (Concurrent Start) (G1 Evacuation Pause) 80M->20M(512M) 10.000ms'
        lines=[young,young,base+'[gc,start] GC(1) Pause Young (Concurrent Start) 10.000ms',base+'[gc] GC(2) Concurrent Mark Cycle 300.000ms',base+'[gc] GC(2) Pause Remark 30M->20M(512M) 2.000ms',base+'[gc] GC(2) Pause Cleanup 20M->20M(512M) 1.000ms']
        r=gc_accounting('\n'.join(lines),0,2e12)
        self.assertEqual((r['distinctStwPauses'],r['crossReplicaStwSumMs'],r['concurrentCycleElapsedSumMs'],r['ignoredDuplicatePauseSummaries']),(3,13,300,1))

    def test_per_jvm_identical_ids_mixed_full_and_end_boundary_stay_distinct(self):
        line='app-1 | [2026-11-11T05:00:01.000+0000][1.000s][info][gc] GC(1) Pause Young (Mixed) 20M->10M(512M) 5.000ms'
        full=line.replace('GC(1) Pause Young (Mixed)','GC(2) Pause Full (G1 Compaction Pause)')
        r=gc_accounting('\n'.join([line,line.replace('app-1','app-2'),full]),0,2e12)
        self.assertEqual((r['distinctStwPauses'],r['perJvm']['app-1']['stwSumMs'],r['perJvm']['app-2']['stwSumMs']),(3,10,5))
        self.assertEqual(r['perJvm']['app-1']['stwByType']['full']['count'],1)
        end=datetime.fromisoformat('2026-11-11T05:00:01+00:00').timestamp()*1000
        self.assertEqual(gc_accounting(line,0,end)['distinctStwPauses'],0)

if __name__=='__main__':unittest.main()
