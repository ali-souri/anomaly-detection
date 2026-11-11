import unittest
from datetime import datetime
from resources009 import gc_pauses

class GcWindowTest(unittest.TestCase):
    def test_gc_pauses_use_the_declared_window_and_retain_cross_replica_sum(self):
        text='''app-1 | [2026-11-11T04:00:00.000+0000][2s][info][gc] GC(1) Pause Young 100M->20M(512M) 8.000ms
app-2 | [2026-11-11T04:00:00.100+0000][2s][info][gc] GC(2) Pause Remark 3.500ms
app-1 | [2026-11-11T04:01:00.000+0000][62s][info][gc] GC(3) Pause Young 900.000ms'''
        start=datetime.fromisoformat('2026-11-11T04:00:00+00:00').timestamp()*1000
        self.assertEqual(gc_pauses(text,start,start+60000),{'count':2,'sumAcrossReplicasMs':11.5,'maximumPauseMs':8.0})

if __name__=='__main__':
    unittest.main()
