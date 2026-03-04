package com.mhsolution.grading;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main entry point for the MH Solution Grading System.
 * The @SpringBootApplication annotation enables:
 *   - @Configuration  : marks this as a source of bean definitions
 *   - @EnableAutoConfiguration : tells Spring Boot to auto-configure based on classpath
 *   - @ComponentScan  : scans this package and sub-packages for components
 */
@SpringBootApplication
public class GradingSystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(GradingSystemApplication.class, args);
    }
}
