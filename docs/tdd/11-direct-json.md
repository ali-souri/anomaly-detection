# 009 compatibility and verification order

All timestamps and complete logs remain ignored under .cache/logs on F:. Compact hashes/counters
are in performance/009/raw/local-quality.json. This is a representation experiment, with no new
HTTP behavior and no invented functional RED for the existing contract.

1. Wrote performance/009/hypothesis.md before production edits; probed the pinned 008 container
   and saved baseline-policy-probe.json. Stopped that probe before subsequent deployments.
2. `./mvnw.cmd -B clean verify`: unchanged baseline GREEN, 61 unit / 56 HTTP, LINE296/0,
   BRANCH134/0, format/lint/strict JavaDoc. Log: 009-starting-green.log.
3. Added 43 real HTTP characterizations, then `./mvnw.cmd -B clean verify` while production
   remained unchanged: GREEN 61 unit / 99 HTTP, same exact coverage. Log: 009-characterization-green.log.
   Covers all six property orders, duplicate last-wins (including earlier oversized/invalid
   values), first-root/trailing policy, syntax precedence, null/defaults/wrong types, finite
   bounds, signed zero/large integers, Unicode/declared charsets, inclusive byte/count and chunked
   limits, public error categories and successful recovery. Existing deterministic occupied-
   admission/media-type/numerical/Swagger/safe500 tests remain.
4. Replaced the production JsonNode path. First direct `spotless:apply clean verify` retained
   all 160 passing tests; the exact coverage gate rejected unexercised new paths. This is not
   a functional RED. Log: 009-direct-first-verify.log.
5. Added direct-reader tests. An actual failed assertion exposed an ignored unknown string
   escaping configured maxStringLength when token completion did not access its text. The parser
   consumed text/numeric tokens even inside skipped structures; a baseline configured ObjectMapper
   comparison confirms the same read constraints. This is a genuine regression found during the
   refactor, not a claim of new behavior or a before-implementation RED.
6. Restored GREEN constraints/HTTP behavior. A normal framework-body-read-failure unit regression
   preserves generic Spring 400 mapping after most malformed bodies moved to our explicit handler.
   `./mvnw.cmd -B spotless:apply clean verify`: 67 unit / 99 HTTP, zero failure/error/skip,
   LINE362/0, BRANCH172/0; format/lint and strict JavaDoc pass. Log: 009-green.log.

The production core source diff versus 008 is empty. No excluded handwritten code, thresholds,
record markers, fork agents/tmpdirs or coverage guards changed. Separate harness, Node/Python
checks and actual CI are reported with the final 009 evidence. Core JMH was not rerun.

7. Later converter review found the US-ASCII header selects Jackson's byte reader in Spring.
   A real HTTP assertion genuinely failed: an UTF-8 unknown field name was replaced by the
   prototype's ASCII Reader. The pinned 008 HTTP probe confirms the original name is retained.
   The first generic-UTF16 assertion incorrectly expected rejection; the baseline probe and
   complete converter bytecode showed autodetection, so it was corrected to success. That wrong
   expectation is NOT counted as a functional regression. Logs: 009-charset-red.log and
   baseline-charset-probe.json. Keep the exact eight Spring Unicode/ASCII byte-encoding names;
   all other declared charsets use a Reader.
8. Final `spotless:apply clean verify`: **67 unit / 101 HTTP**, LINE364/0, BRANCH172/0,
   INSTRUCTION1600/0, METHOD77/0, CLASS29/0, COMPLEXITY166/0; strict JavaDoc/format/lint pass.
   Source ebf432f4a72325aa0504a183b571dabf782d47c0. Separate nine-test Java harness, Node2,
   existing Python8 and new Python4 tests pass. No unchanged core JMH ran.

The initial measurements on 416ea7c were stopped when reviewing the charset edge and retained
under performance/009/preliminary, marked ineligible for final optimization/capacity conclusions.
The corrected image received fresh exhaustive API validation, matched JFR and all six fresh
interleaved target runs. It is the only candidate used in the final comparison.
