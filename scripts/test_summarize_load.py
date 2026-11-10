"""Contract tests for failure separation and finite-window accounting."""
import unittest
from summarize_load import summarize, percentile, rows_from_console


class SummaryTest(unittest.TestCase):
    def test_rejections_never_improve_success_percentiles_or_throughput(self):
        rows = [dict(iteration=0, status=200, started=1000, completed=2000),
                dict(iteration=1, status=503, started=1100, completed=1101)]
        actual = summarize(rows, {'successful_requests': {'count': 1}})
        self.assertEqual(actual['p99Ms'], 1000)
        self.assertEqual(actual['successful'], 1)
        self.assertEqual(actual['rejected503'], 1)
        self.assertFalse(actual['allScheduledSucceeded'])

    def test_boundary_drain_is_visible_and_duplicate_ids_are_rejected(self):
        rows = [dict(iteration=0, status=200, started=1000, completed=62000)]
        actual = summarize(rows, {'successful_requests': {'count': 1}})
        self.assertEqual(actual['drainMs'], 1000)
        self.assertEqual(actual['successfulCompletedInWindowRate'], 0)
        with self.assertRaises(ValueError):
            summarize(rows + rows, {'successful_requests': {'count': 2}})

    def test_percentile_and_real_console_decoding(self):
        self.assertEqual(percentile([10, 20, 30], .95), 29)
        self.assertIsNone(percentile([], .99))
        text = 'time="x" level=info msg="{\\"iteration\\":0,\\"status\\":200}" source=console'
        self.assertEqual(rows_from_console(text), [{'iteration': 0, 'status': 200}])


if __name__ == '__main__':
    unittest.main()
