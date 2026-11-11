# One selected optimization — declared before production edits

The completed warmed IQR/active=1 recording contains **594 jdk.ExecutionSample events**.
Leaf-first exclusive classification assigns **273 / 594 (45.9596%)** to boxed-list sorting.
Top leaves include Double.compare (117), ComparableTimSort.mergeLo (53) and mergeHi (45).
JSON tree creation accounts for 19.0236%, tree-to-list conversion 9.9327%, serialization 14.8148%.
NativeMethodSample is kept separate: 583 events, mostly EPoll.wait / Net.accept; these are not
added to Java samples or interpreted as equivalent CPU time. Other concurrency profiles continue.

Selected smallest change: **only IqrDetector's temporary sort workspace** becomes a double array,
sorted by Arrays.sort. Convert the already validated defensive snapshot once, use the unchanged
median-of-halves indices and overflow-safe median arithmetic, then classify the original Double
references exactly as in 007. Z_SCORE, parsing, admission, response models and limits do not change.
This is local scratch storage, not a primitive model/API migration or statistical algorithm change.

Prediction: at 2M MIXED the paired unprofiled IQR core mean falls by at least 20% relative to fresh
007 measurements, with complete results inside detect. Z_SCORE is an unchanged control. B/op may
increase because double scratch storage replaces compressed reference storage and TimSort scratch;
measure that tradeoff rather than claiming another allocation saving. Accept only if readable code,
all numerical/order/immutability/HTTP gates and a clear measured IQR benefit remain.

Compatibility risks: signed zero ordering, median indexing for odd/even/singleton inputs,
overflow/subnormal median arithmetic, and preserved ordered immutable original-value partitions.
Add two mathematical/signed-zero unit regressions and one real HTTP/property-order regression,
run them GREEN against unchanged production, then refactor and retain GREEN. No invented functional
RED is appropriate for this representation-only change. Existing arithmetic guards stay intact.

References: [JDK Arrays.sort(double[])](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/Arrays.html#sort(double%5B%5D))
documents the same Double total order including signed zero;
[JFR print](https://docs.oracle.com/en/java/javase/21/docs/specs/man/jfr.html) explains stack-depth export.
Sampling percentages are estimates, not precise phase wall-clock durations.
