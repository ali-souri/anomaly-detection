import unittest
import tempfile
import json
import csv
from pathlib import Path
from unittest.mock import patch
import report009

class NewEvidenceReportTest(unittest.TestCase):
    def test_new_namespace_preserves_rejections_and_does_not_write_accepted_evidence(self):
        root = Path(__file__).resolve().parents[1]
        with tempfile.TemporaryDirectory(dir=root / '.cache/tmp') as directory:
            base = Path(directory)
            raw = base / 'docs/performance/009/raw'
            raw.mkdir(parents=True)
            old = base / 'docs/performance/008/raw'
            old.mkdir(parents=True)
            sentinel = old / 'load-analysis.json'
            sentinel.write_text('accepted')
            (raw/'A-IQR-run.json').write_text(json.dumps({'duration':'60s'}))
            (raw/'A-IQR-summary.json').write_text(json.dumps({'metrics':{'successful_requests':{'count':1}}}))
            with (raw/'A-IQR-requests.csv').open('w',newline='') as output:
                writer=csv.DictWriter(output, fieldnames=['iteration','algorithm','started','completed','status','errorCode','sampled','responseBodyBytes'])
                writer.writeheader()
                writer.writerows([dict(iteration=0,algorithm='IQR',started=1000,completed=1200,status=200,errorCode=0,sampled=False,responseBodyBytes=None),dict(iteration=1,algorithm='IQR',started=1100,completed=1105,status=503,errorCode=1503,sampled=False,responseBodyBytes=None)])
            with patch.object(report009, 'Path', lambda value: base/value):
                report009.main()
            result=json.loads((raw/'load-analysis.json').read_text())['A-IQR']
            self.assertEqual((result['successful'],result['rejected503']),(1,1))
            self.assertFalse(result['allScheduledSucceeded'])
            self.assertFalse(result['diagnosticProfileRun'])
            self.assertEqual(sentinel.read_text(),'accepted')

if __name__=='__main__':
    unittest.main()
