# 009 hypothesis — before production edits

Remove intermediate per-number JsonNode objects and the subsequent tree traversal by using the
configured Jackson token factory to produce the validated ordinary numeric request directly.
This may reduce input allocation/CPU; it does not promise ten RPS or a speedup obtained by adding
008's sampled phase percentages. The core tree must remain identical to 008.

Characterize first: property permutations, duplicates/last-wins policy, ignored trailing content,
syntax-versus-semantic precedence, token types/large integers/signed zero, encoding, strict fields,
byte/count limits, real HTTP error mapping and permit recovery. Keep these GREEN before/after.
Defer final semantic checks until the first root value is syntactically complete where needed to
retain baseline precedence. Duplicate fields must be compatible; complexity is a rejection reason.

Keep the existing bounded raw-body advice and pre-binding admission. A byte[] controller bridge
is permitted and may copy the already bounded buffer; measure and disclose that cost. Numeric
lists and raw buffers stay O(n). No serializer/core/output/resource/permit optimization.

Primary comparator: 008 publication 8d7d41c34c9e2d10e955092239fd87bb91a8acf7, pinned container
sha256:26a1bf88cfee5aebb907539488227f083aae105509de4c53a38bfc3a77251df5 and JAR
f23766f4d65e8764b0021f87ead2752aa2607715e985be8b4968f87250d2cfbe. Both versions use the same
runtime image/JDK21.0.11, G1/2GiB heap, 3 replicas × 3.75CPU/3GiB/active3, nginx1CPU/128MiB,
k6 2CPU/2GiB/GOMAXPROCS2/max20VUs: 14.25CPU/11.125GiB ceiling. Never run both deployments together.

Declare order **A-IQR, B-IQR, A-Z_SCORE, B-Z_SCORE, A-MIXED, B-MIXED**, fresh deployments each
switch, matched two full warmups, 60-second ten-RPS open-arrival windows. Window starts at first
actual scheduled request timestamp; report exact-window completions separately from total successes
and drain, retaining all timestamps, drops/rejections/errors/bytes. No post-result window change.
An optional final A-MIXED repetition exposes drift if time allows. Exhaustive FULL validation for
both algorithms/implementations precedes any target timing. Final load has no agent/JFR/build/JMH.

One matched 15-second JFR diagnostic per version at controlled active3/client3, after warmup,
same runtime/resources, profile/depth128/maxsize128MiB. Group old tree+conversion and new token
reading consistently as input parsing; retain native/unknown/blocked work and sampled weights.
No unchanged core JMH rerun is needed. Stop after the increment; no automatic next optimization.
