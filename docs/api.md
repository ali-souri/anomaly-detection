# HTTP contract

`POST /api/v1/anomaly-detection`, `Content-Type: application/json`. The request must be an object
with exactly the documented fields. `algorithm` is a required case-sensitive canonical identifier:
`IQR` or `Z_SCORE`. `data` is a required array of finite JSON numbers; `[]` is valid. `threshold` is an optional
finite, non-negative JSON number. Omission selects the algorithm default; explicit null is invalid.
Null elements, strings (including numeric strings), booleans, nested objects/arrays, unknown fields,
missing/null data and wrong shapes return 400. JSON has no valid NaN/Infinity literals.
Values are interpreted as IEEE 754 double precision; integer precision beyond 2^53 is not promised.

## IQR

Default threshold **1.5** (dimensionless IQR multiplier). Sort a copy. Q1 and Q3 are medians
of lower and upper halves, excluding the overall middle only when n is odd. An even-sized half
uses the arithmetic mean of its two middle values. At n=1 both quartiles equal that value;
at n=2/3 use the same halves convention. Every original value is still classified.

`iqr = q3 - q1`, `lowerBound = q1 - threshold * iqr`, `upperBound = q3 + threshold * iqr`.
Anomalous means strictly below/above the bounds. Equality is normal. No rounding precedes
classification. Zero threshold and zero IQR use the same formula with no artificial widening.

```json
{"algorithm":"IQR","threshold":1.5,"data":[10,12,11,13,12,14,11,13,12,100]}
```

```json
{"algorithm":"IQR","threshold":1.5,"status":"COMPLETED","warnings":[],"anomalies":[100.0],"normalValues":[10.0,12.0,11.0,13.0,12.0,14.0,11.0,13.0,12.0],"q1":11.0,"q3":13.0,"iqr":2.0,"lowerBound":8.0,"upperBound":16.0}
```

Empty input, after validating algorithm and parameters:

```json
{"algorithm":"IQR","threshold":1.5,"status":"NO_DATA","warnings":[],"anomalies":[],"normalValues":[],"q1":null,"q3":null,"iqr":null,"lowerBound":null,"upperBound":null}
```

For `[5]`, status is `COMPLETED`, warnings are `["SMALL_DATASET"]`, Q1=Q3=both bounds=5,
IQR=0, normalValues=[5], anomalies=[]. Sizes two and three have the same warning. Any nonempty
successful result is `COMPLETED`, even with no anomalies. Partitions preserve original order and
duplicates; input lists are never mutated. Common lists and all published results are immutable.

## Z_SCORE

Default threshold **3.0**, in population standard deviations. Welford's running mean and sum of
squared deviations avoid the cancellation of `mean(x*x) - mean(x)^2`. Population variance is the
sum of squared deviations divided by **n**, not n-1. A second pass classifies original values with
`abs((value - mean) / standardDeviation) > threshold`. Equality is normal; no rounding occurs
before comparison. A zero threshold keeps only values whose score is exactly zero.

```json
{"algorithm":"Z_SCORE","threshold":1.5,"data":[2,4,4,4,5,5,7,9]}
```

```json
{"algorithm":"Z_SCORE","threshold":1.5,"status":"COMPLETED","warnings":[],"anomalies":[9.0],"normalValues":[2.0,4.0,4.0,4.0,5.0,5.0,7.0],"mean":5.0,"standardDeviation":2.0}
```

The response has the same common fields as IQR, followed only by `mean` and `standardDeviation`.
It contains no IQR statistics. The original ten-value IQR sample has no Z_SCORE anomaly at the
default threshold 3.0; the score for 100 is below 3. Different algorithms answer different questions.

Empty data returns 200:

```json
{"algorithm":"Z_SCORE","threshold":3.0,"status":"NO_DATA","warnings":[],"anomalies":[],"normalValues":[],"mean":null,"standardDeviation":null}
```

Zero variance returns all values as normal, standardDeviation=0 and `ZERO_VARIANCE`, without
division. At n=1..3, `SMALL_DATASET` also applies; `[7]` has mean=7, standardDeviation=0 and
warnings `["SMALL_DATASET","ZERO_VARIANCE"]`. Four equal values have only ZERO_VARIANCE.
Nonempty successful calculations always use COMPLETED.

Intermediate Welford arithmetic is checked for overflow; variance underflow must not silently
turn distinct values into a zero-variance result. Such cases return 422. This is deliberately
conservative: arbitrary-precision/scaled methods could handle some rejected extreme inputs.
Ordinary floating-point last-bit error remains possible; tests use tight tolerances for statistics
and exactly representable values for exact-boundary checks.

OpenAPI describes the success response as `DetectionResponse.oneOf` with `IqrResponse` and
`ZScoreResponse`. The `algorithm` discriminator maps IQR and Z_SCORE explicitly, and each variant
requires its unique algorithm enum value. This prevents an ambiguous all-statistics response shape.

## Errors

Errors use RFC 9457 `application/problem+json`, with `type`, `title`, numeric `status`, safe
`detail`, request-path `instance` and stable extension `code`. They never contain stack traces.

| HTTP | Code | Meaning |
| --- | --- | --- |
| 200 | — | Successful calculation, including small or empty data |
| 400 | `INVALID_REQUEST` | Missing field, unknown field, wrong shape/type or invalid parameter |
| 400 | `MALFORMED_JSON` | JSON cannot be parsed |
| 400 | `UNSUPPORTED_ALGORITHM` | Unknown or non-canonical identifier, even for empty input |
| 405 | `METHOD_NOT_ALLOWED` | Unsupported method; framework Allow header preserved |
| 413 | `INPUT_TOO_LARGE` | Configured number-count or raw body-byte limit exceeded |
| 415 | `UNSUPPORTED_MEDIA_TYPE` | Content type is not supported |
| 422 | `NUMERICAL_RANGE` | Finite inputs produce unsafe/unrepresentable computed arithmetic |
| 503 | `ADMISSION_FULL` | Per-instance admission full; Retry-After: 1 second |
| 500 | `INTERNAL_ERROR` | Unexpected failure; internal details are not exposed |

Other framework HTTP errors retain their status and use `HTTP_<status>` codes.

```json
{"type":"urn:anomaly-detection:problem:invalid_request","title":"Bad Request","status":400,"detail":"threshold must be a finite non-negative JSON number.","instance":"/api/v1/anomaly-detection","code":"INVALID_REQUEST"}
```

Double arithmetic is checked at calculation boundaries, including intermediate overflow. The API
returns 422 instead of emitting NaN/Infinity or silently classifying with invalid statistics.
Stable median averaging avoids overflowing the sum of two equal large values. This is an in-memory
API. v0.3 adds positive configurable per-instance demo policies: 1 active detection request,
2,000,000 numbers and 64 MiB raw body, both size bounds inclusive. See README for exact property
and environment names. These are operational limits, not numerical errors or a measured latency SLO.
Oversize declared bodies are rejected before reading; a bounded JDK read checks actual bytes,
including unknown/chunked length and trailing whitespace, before JSON parsing. The existing advice buffers raw bytes; the byte[] message converter makes
an additional raw copy. The direct token parser builds the ordinary numeric list without a
JsonNode tree and checks the inclusive count before appending beyond it. It does not allocate
the maximum capacity up front. Raw buffers and numeric snapshots remain O(n) memory. Admission
is acquired before conversion, with no waiting queue,
and released in finally after the synchronous filter chain returns (including response writing
and failure paths). A per-instance bound is not a cluster-wide bound. HTTP 503/413 do not count
toward the owner's ten successful complete requests/second target. Clients should honor the
one-second Retry-After and apply their own bounded retry policy.

## Interactive documentation

Both `/v3/api-docs` (including YAML) and Swagger UI are disabled in base configuration. The dev
profile enables them only when `SWAGGER_ENABLED=true`; dev without the flag defaults to false.
The same flag outside dev has no effect. The HTTP integration suite checks these modes using
actual servers and real HTTP requests. No authentication is implemented in these releases.
Full default-port URLs: http://localhost:8080/swagger-ui/index.html and
http://localhost:8080/v3/api-docs. `PORT` or `--server.port` changes 8080; update both URLs.
The API-only base configuration also disables general static-resource mappings, blocking direct
Swagger WebJar URLs. Dev enables those mappings only with the same explicit flag.

## Input compatibility in 009

Fields may appear in any order. As observed on 008, duplicate known members use their final
value; semantic checks wait until that first root object is syntactically complete. Later malformed
syntax therefore wins over an earlier semantic error. Final oversize arrays return 413 before
element validation. Unknown fields remain invalid. Content after a complete first root is ignored
by JSON parsing, while all bytes still count toward the body limit. This is retained compatibility,
not a recommendation for clients to send duplicate members or extra roots. Integer -0 becomes
positive zero, while floating -0.0 retains its sign. Declared encodings, finite bounds and configured
Jackson read constraints remain enforced; syntax/encoding/constraint failures map to MALFORMED_JSON.
The documented request/response object schemas and examples are unchanged.

## Input compatibility in 009

Fields may appear in any order. As observed on 008, duplicate known members use their final
value; semantic checks wait until that first root object is syntactically complete. Later malformed
syntax therefore wins over an earlier semantic error. Final oversize arrays return 413 before
element validation. Unknown fields remain invalid. Content after a complete first root is ignored
by JSON parsing, while all bytes still count toward the body limit. This is retained compatibility,
not a recommendation for clients to send duplicate members or extra roots. Integer -0 becomes
positive zero, while floating -0.0 retains its sign. Declared encodings, finite bounds and configured
Jackson read constraints remain enforced; syntax/encoding/constraint failures map to MALFORMED_JSON.
The documented request/response object schemas and examples are unchanged.
