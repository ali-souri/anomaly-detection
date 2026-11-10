"""Validate the completed declared matrix and derive compact summaries from actual raw data."""

import csv
import itertools
import json
import math
from pathlib import Path
import statistics


def main():
    """Reject missing/partial measurements, then write CSV and print observed comparisons."""
    raw = Path("docs/benchmarks/v0.2/raw")
    results = json.loads((raw / "jmh.json").read_text(encoding="utf-8-sig"))
    expected = set(itertools.product(
        ("IQR", "Z_SCORE"), ("MIXED", "DUPLICATES", "DESCENDING"),
        (10000, 100000, 1000000, 2000000)))
    actual = set()
    rows = []
    for result in results:
        parameters = result["params"]
        key = (parameters["algorithm"], parameters["kind"], int(parameters["size"]))
        assert key not in actual, key
        actual.add(key)
        primary = result["primaryMetric"]
        assert result["forks"] == 2 and result["threads"] == 1
        assert result["warmupIterations"] == 2 and result["measurementIterations"] == 3
        assert primary["scoreUnit"] == "ms/op"
        assert len(primary["rawData"]) == 2 and all(len(fork) == 3 for fork in primary["rawData"])
        assert not any("javaagent" in option for option in result["jvmArgs"])
        allocation = result["secondaryMetrics"]["gc.alloc.rate.norm"]["score"]
        assert math.isfinite(primary["score"]) and primary["score"] > 0
        assert math.isfinite(allocation) and allocation > 0
        rows.append(dict(algorithm=key[0], kind=key[1], size=key[2],
                         ms_op=primary["score"], jmh_score_error_ms=primary["scoreError"],
                         allocation_B_op=allocation, allocation_MiB_op=allocation / 1048576,
                         gc_count_total=result["secondaryMetrics"]["gc.count"]["score"],
                         gc_time_ms_total=result["secondaryMetrics"]["gc.time"]["score"],
                         measured_iteration_means=6))
    assert actual == expected, ("Incomplete declared experiment", expected - actual)
    with (raw / "jmh-summary.csv").open("w", newline="", encoding="utf-8") as output:
        writer = csv.DictWriter(output, fieldnames=list(rows[0]))
        writer.writeheader()
        writer.writerows(rows)
    print("All 24 JMH cases have two forks and six measured iteration means.")
    for row in rows:
        if row["kind"] == "MIXED" or row["size"] == 2000000:
            print(row)
    api = json.loads((raw / "api.json").read_text(encoding="utf-8-sig"))
    api_rows = []
    for algorithm in ("IQR", "Z_SCORE"):
        samples = [row for row in api["rows"] if row["algorithm"] == algorithm]
        assert len(samples) == 3 and all(row["status"] == 200 and row["validated"] and row["size"] == 2000000 for row in samples)
        times = [row["durationMs"] for row in samples]
        summary = dict(algorithm=algorithm, size=2000000, samples=3,
                       median_ms=statistics.median(times), minimum_ms=min(times), maximum_ms=max(times),
                       request_bytes=samples[0]["requestBytes"], response_bytes=samples[0]["responseBytes"])
        api_rows.append(summary)
        print("API", summary)
    with (raw / "api-summary.csv").open("w", newline="", encoding="utf-8") as output:
        writer = csv.DictWriter(output, fieldnames=list(api_rows[0]))
        writer.writeheader()
        writer.writerows(api_rows)


if __name__ == "__main__":
    main()
