"""New-namespace report using the unchanged, tested request-window accounting."""
from pathlib import Path
import report008

if __name__ == '__main__':
    # report008's implementation is preserved; only its Path roots are redirected.
    original_path = Path
    report008.Path = lambda value: original_path(str(value).replace('/008/', '/009/'))
    report008.main()
