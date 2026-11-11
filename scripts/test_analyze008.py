import unittest
import tempfile
import json
from pathlib import Path
from analyze008 import frame_name, stack_groups, summarize_events, streamed_events


def frame(name, method):
    return {'method': {'type': {'name': name}, 'name': method}}


class ProfileAccountingTest(unittest.TestCase):
    def test_streamed_export_crosses_chunks_without_loading_the_whole_recording(self):
        with tempfile.TemporaryDirectory(dir=Path(__file__).resolve().parents[1] / '.cache/tmp') as directory:
            source = Path(directory) / 'events.json'
            event = {'type': 'test', 'values': {'text': 'x' * 70000 + '} quoted " bracket ]'}}
            source.write_text(json.dumps({'recording': {'events': [event, event]}}))
            self.assertEqual(list(streamed_events(source)), [event, event])

    def test_leaf_first_exclusive_preserves_inclusive_overlapping_ancestors(self):
        frames = [frame('java/util/ComparableTimSort', 'sort'), frame('io/anomaly/core/algorithm/IqrDetector', 'detect')]
        exclusive, inclusive = stack_groups(frames)
        self.assertEqual(exclusive, 'sort-workspace')
        self.assertEqual(inclusive, {'sort-workspace', 'detector-statistics-classification'})
        self.assertEqual(frame_name(frames[0]), 'java.util.ComparableTimSort.sort')

    def test_native_unknown_and_allocation_weights_remain_separate(self):
        frames = [frame('unclassified/NativeHelper', 'work')]
        events = [dict(type='jdk.ExecutionSample', values=dict(startTime='x', stackTrace={'frames': frames})),
                  dict(type='jdk.NativeMethodSample', values=dict(startTime='y', stackTrace={'frames': frames})),
                  dict(type='jdk.ObjectAllocationSample', values=dict(weight=4096, stackTrace={'frames': frames}, objectClass={'name': '[D'}))]
        result = summarize_events(events)
        self.assertEqual(result['cpuSamples']['jdk.ExecutionSample']['count'], 1)
        self.assertEqual(result['cpuSamples']['jdk.NativeMethodSample']['count'], 1)
        self.assertEqual(result['sampledAllocationWeightBytes'], 4096)
        self.assertEqual(result['exclusiveAllocationWeightBytes'], {'unclassified': 4096})


if __name__ == '__main__':
    unittest.main()
