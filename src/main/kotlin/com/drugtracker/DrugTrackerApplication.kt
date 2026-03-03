package com.drugtracker

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

/**
 * Main entry point for the DrugTracker Spring Boot application.
 *
 * The @SpringBootApplication annotation is a convenience annotation that combines:
 *
 * - @Configuration → Marks this class as a source of bean definitions
 * - @EnableAutoConfiguration → Enables Spring Boot’s auto-configuration mechanism
 * - @ComponentScan → Scans for components (controllers, services, repositories)
 *                    in the current package and sub-packages
 *
 * When the application starts:
 * - Spring Boot initializes the embedded web server (e.g., Tomcat)
 * - All controllers, services, and repositories are registered as beans
 * - The application context is created
 *
 * This class serves as the root configuration class of the system.
 */
@SpringBootApplication
class DrugTrackerApplication
/**
 * Main function that launches the Spring Boot application.
 *
 * This function delegates to Spring Boot's runApplication function,
 * which bootstraps the entire application context and starts
 * the embedded web server.
 *
 * @param args Command-line arguments passed during application startup
 */
fun main(args: Array<String>) {
    // Bootstraps and starts the Spring Boot application
    runApplication<DrugTrackerApplication>(*args)
}