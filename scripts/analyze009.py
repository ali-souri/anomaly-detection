"""Reuse bounded JFR decoding with one consistent old/new input-parsing bucket."""
import json
from pathlib import Path
import analyze008

_original_category = analyze008.category

def category(name):
    if _original_category(name) in ('json-tree-creation', 'tree-to-number-list'):
        return 'input-parsing'
    if 'ByteArrayHttpMessageConverter.' in name:
        return 'raw-body-buffer'
    return _original_category(name)

def summarize(events):
    original = analyze008.category
    try:
        analyze008.category = category
        return analyze008.summarize_events(events)
    finally:
        analyze008.category = original

if __name__ == '__main__':
    for source in Path('.cache/009/profiles').glob('*-events.json'):
        result = summarize(analyze008.streamed_events(source))
        output = Path('docs/performance/009/raw') / source.name.replace('-events.json', '-stack-analysis.json')
        output.write_text(json.dumps(result, indent=2) + '\n')
        print(source.name, {k: (v['count'], v['exclusivePercent']) for k,v in result['cpuSamples'].items()})
