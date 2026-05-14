package com.orderprocessing.test;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Diagnostic class to verify Lombok annotation processing works.
 * 
 * ROOT CAUSE: Maven 3.9+ no longer auto-discovers annotation processors from the classpath.
 * FIX: Explicit annotationProcessorPaths in maven-compiler-plugin (see pom.xml).
 * 
 * If this compiles, @Data, @Builder, @Slf4j all work correctly.
 * Keep this file as a troubleshooting reference.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Slf4j
public class LombokTest {
    private String name;
    private int value;

    public static void main(String[] args) {
        // Test @Builder
        LombokTest test = LombokTest.builder().name("hello").value(42).build();
        // Test @Data (getters)
        System.out.println(test.getName() + " = " + test.getValue());
        // Test @Slf4j
        log.info("Lombok is working: {}", test);
    }
}
