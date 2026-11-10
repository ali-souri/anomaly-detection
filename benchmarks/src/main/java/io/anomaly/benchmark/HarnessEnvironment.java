package io.anomaly.benchmark;

import java.lang.management.ManagementFactory;

/** Prevents accidentally timing an instrumented test JVM. */
public final class HarnessEnvironment {
  private HarnessEnvironment() {}

  /**
   * Rejects active Java agents before any measurement.
   *
   * @throws IllegalStateException if a Java agent is present in actual VM arguments
   */
  public static void requireUninstrumented() {
    for (String argument : ManagementFactory.getRuntimeMXBean().getInputArguments()) {
      if (argument.contains("-javaagent")) {
        throw new IllegalStateException("Timed JVM must be uninstrumented.");
      }
    }
  }
}
