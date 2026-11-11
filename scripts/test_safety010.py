import unittest
from safety010 import overlap, screen_safe

class SafetyTest(unittest.TestCase):
 def test_boundary_touching_requests_do_not_prove_three_active(self):
  self.assertEqual(overlap([dict(started=0,completed=2),dict(started=1,completed=3),dict(started=2,completed=4)]),2)
 def test_safety_requires_actual_overlap_headroom_and_no_memory_event(self):
  rows=[dict(started=0,completed=5,status=200)]*3
  samples=[dict(availableHostKiB=2000000,apps=[dict(peakBytes=2*2**30,memoryEvents=['oom 0','max 0'])])]
  self.assertTrue(screen_safe(rows,{},samples))
  self.assertFalse(screen_safe(rows,{},samples,True))
  samples[0]['apps'][0]['memoryEvents']=['oom 1']
  self.assertFalse(screen_safe(rows,{},samples))
  samples[0]['apps'][0]['memoryEvents']=['oom 0'];samples[0]['apps'][0]['peakBytes']=3*2**30
  self.assertFalse(screen_safe(rows,{},samples))

if __name__=='__main__':unittest.main()
