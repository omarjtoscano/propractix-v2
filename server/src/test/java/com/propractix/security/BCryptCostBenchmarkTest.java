package com.propractix.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.stream.LongStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@EnabledIfSystemProperty(named = "bcrypt.benchmark", matches = "true")
class BCryptCostBenchmarkTest {

    @Test
    void measuresCostTwelveOnTheTargetEnvironment() {
        var encoder = new BCryptPasswordEncoder(12);
        long[] samples = new long[10];

        for (int index = 0; index < samples.length; index++) {
            long started = System.nanoTime();
            String encoded = encoder.encode("synthetic-benchmark-value-" + index);
            samples[index] = (System.nanoTime() - started) / 1_000_000;
            assertThat(encoder.matches("synthetic-benchmark-value-" + index, encoded)).isTrue();
        }

        long average = LongStream.of(samples).sum() / samples.length;
        long maximum = LongStream.of(samples).max().orElseThrow();
        System.out.printf("bcrypt cost=12 samples=%d average_ms=%d max_ms=%d%n",
                samples.length, average, maximum);
    }
}
