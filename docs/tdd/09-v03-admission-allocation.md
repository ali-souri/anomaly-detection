# v0.3 actual RED/GREEN evidence

Task: AD-PRM-20261111-007, superseding unexecuted 006. Earlier issued files/results remain intact.

## Input policies

The first compiling real HTTP tests ran against unchanged production source:

```powershell
.\mvnw.cmd -B test-compile failsafe:integration-test failsafe:verify '-Dit.test=RequestLimitsIT'
```

`.cache/logs/007-limits-red.log`: four tests, **three assertion failures**, zero errors/skips.
Number count 4 over configured 3, declared body 129 over 128 bytes and unknown-length/chunked
129-byte body each returned **200 instead of 413**. Exactly-at-boundary requests were accepted.
The initial sandbox loopback denial was an environment failure, not RED; the recorded RED reran
with real loopback access. Implementation: positive startup configuration, strict count check and
Spring before-body advice using JDK readNBytes(limit + 1), all under early filter admission.
Same command GREEN: four tests, no failures/errors/skips (`007-limits-green.log`).

## Admission

The deterministic occupied-slot test uses a **test-only filter** after production admission,
coordinated by latches, then runs the genuine parser/registry/detector/serializer. No sleep-based
coordination and no mocked statistical flow. A second future must resolve within two seconds
while the first remains held, with 503/stable code/Retry-After; after release real detection succeeds.

The isolated immutable v0.2.1 archive was tested with that new test:

```powershell
# Maven 3.9.16 executable from the project's wrapper distribution, -f isolated baseline POM
mvn -B -f .cache/v0.3/baseline-source/pom.xml -Dmaven.repo.local=F:/projects/anomaly-detection/.cache/repository test-compile failsafe:integration-test failsafe:verify -Dit.test=AdmissionIT
```

`.cache/logs/007-admission-baseline-red.log`: **one assertion failure**, zero errors/skips;
second request returned 200 instead of 503. This is a comparative RED captured after the first
candidate implementation, not a claim that every admission line was written test-first. The
first archive run lacked its tmp directory; that environment error is not RED. Candidate GREEN
is in full verification logs. The subsequently added 10 MB occupied-slot variant also passes.

## Measured overload regression and refactoring

Initial actual-container closed-client overload produced 3 unexpected transport errors at two
clients and 349 at four clients, alongside explicit 503s. Tomcat's small default rejected-body
discard limit was a source-supported cause. Its bounded discard allowance is now 64 MiB (same
default byte policy), while JSON is still rejected before conversion. Corrected 1/2/4-client
checks have zero unexpected errors. Closed clients now include an explicit 100 ms think time;
these two runs are policy/recovery evidence, not a controlled performance comparison.

Existing four-client isolation tests were preserved with max-active=4 instead of assuming every
concurrent request is accepted under a one-slot policy. New unit checks cover invalid positive
configuration, binding failure, synchronous response ownership, servlet/client-write failures
and unrelated routes. Real HTTP checks cover recovery after success, malformed JSON, numerical
422 and an injected failing strategy's safe 500. All ordinary statistical/Swagger regressions stay.

## Allocation and separate tooling

Classification iterates Double references, computes primitive arithmetic and appends existing
immutable objects to eager result lists. Public contracts test exact order, duplicate multiplicity,
signed zero, unchanged input, defensive snapshots and immutable partitions; no identity-only test.
Existing mathematical tests and independent full-result JMH/API validators remain.
The paired profiler, rather than an object-layout assumption, establishes the allocation claim.

Separate tooling has nine JUnit tests (seven existing fixture tests plus request generation and
full-response validator rejection), two Node checks for accepted/rejected mathematical summaries,
and four Python checks for successful-only percentiles, exact window/drain accounting, real
console decoding and compact-CSV preservation. Root application coverage does not claim to cover this separate tooling.
Strict formatting/lint/JavaDoc and the exact zero-missed application coverage gate remain enabled.
