import unittest
from report008 import counters


class CpuCounterTest(unittest.TestCase):
    def test_cgroup_microseconds_and_periods_preserve_their_original_units(self):
        before = counters(['usage_usec 1000', 'nr_periods 5', 'nr_throttled 2'])
        after = counters(['usage_usec 1400', 'nr_periods 8', 'nr_throttled 3'])
        self.assertEqual({key: after[key]-value for key,value in before.items()},
                         {'usage_usec': 400, 'nr_periods': 3, 'nr_throttled': 1})


if __name__ == '__main__':
    unittest.main()
