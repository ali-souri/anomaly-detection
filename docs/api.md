# HTTP contract

`POST /api/v1/anomaly-detection`, `Content-Type: application/json`. The request must be an object
with exactly the documented fields. `algorithm` is a required case-sensitive canonical identifier:
`IQR`. `data` is a required array of finite JSON numbers; `[]` is valid. `threshold` is an optional
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
| 415 | `UNSUPPORTED_MEDIA_TYPE` | Content type is not supported |
| 422 | `NUMERICAL_RANGE` | Finite inputs produce unsafe/unrepresentable computed arithmetic |
| 500 | `INTERNAL_ERROR` | Unexpected failure; internal details are not exposed |

Other framework HTTP errors retain their status and use `HTTP_<status>` codes.

```json
{"type":"urn:anomaly-detection:problem:invalid_request","title":"Bad Request","status":400,"detail":"threshold must be a finite non-negative JSON number.","instance":"/api/v1/anomaly-detection","code":"INVALID_REQUEST"}
```

Double arithmetic is checked at calculation boundaries, including intermediate overflow. The API
returns 422 instead of emitting NaN/Infinity or silently classifying with invalid statistics.
Stable median averaging avoids overflowing the sum of two equal large values. This is an in-memory
API with no streaming or configured data-size limit yet; deployment ingress limits are a future
operational decision informed by benchmarks.

## Interactive documentation

Both `/v3/api-docs` (including YAML) and Swagger UI are disabled in base configuration. The dev
profile enables them only when `SWAGGER_ENABLED=true`; dev without the flag defaults to false.
The same flag outside dev has no effect. The HTTP integration suite checks these modes using
actual servers and real HTTP requests. No authentication is implemented in these releases.
