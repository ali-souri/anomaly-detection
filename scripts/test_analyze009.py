import unittest
from analyze009 import category, summarize

class MatchedProfileTest(unittest.TestCase):
    def test_old_tree_conversion_and_new_token_frames_share_a_bucket(self):
        for name in ['com.fasterxml.jackson.databind.deser.std.JsonNodeDeserializer.deserialize', 'com.fasterxml.jackson.core.json.UTF8StreamJsonParser.nextToken', 'io.anomaly.api.parser.RequestParser.number']:
            self.assertEqual(category(name), 'input-parsing')
        self.assertEqual(category('org.springframework.http.converter.ByteArrayHttpMessageConverter.readInternal'), 'raw-body-buffer')

    def test_native_and_unknown_work_are_retained_and_classifier_is_restored(self):
        import analyze008
        before = analyze008.category
        result = summarize([{'type':'jdk.NativeMethodSample','values':{'startTime':'x'}}])
        self.assertEqual(result['cpuSamples']['jdk.NativeMethodSample']['exclusive'], {'unclassified':1})
        self.assertIs(analyze008.category, before)

if __name__ == '__main__':
    unittest.main()
