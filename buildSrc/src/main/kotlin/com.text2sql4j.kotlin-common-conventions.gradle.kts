/*
 * This file was generated by the Gradle 'init' task.
 */

import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    // Apply the org.jetbrains.kotlin.jvm Plugin to add support for Kotlin.
    id("org.jetbrains.kotlin.jvm")
}

repositories {
    // Use Maven Central for resolving dependencies.
    mavenCentral()
}

val logbackVersion = "1.2.3"
val slf4jVersion = "1.7.30"
val junitVersion = "5.9.1"
val junitPlatformVersion = "1.9.1"
val kotlinVersion = "1.8.10"

dependencies {
    constraints {
        implementation("ch.qos.logback:logback-classic:$logbackVersion")
        implementation("org.slf4j:slf4j-api:$slf4jVersion")
        implementation("org.jetbrains.kotlin:kotlin-stdlib:$kotlinVersion")
        implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$kotlinVersion")
    }

    // Use JUnit Jupiter for testing.
    testImplementation("org.junit.jupiter:junit-jupiter-api:$junitVersion")
    testImplementation("org.junit.jupiter:junit-jupiter-params:$junitVersion")
    testImplementation("org.junit.platform:junit-platform-launcher:$junitVersion")
}

// JVM target applied to all Kotlin tasks across all subprojects
tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = JavaVersion.VERSION_11.toString()
}

tasks.named<Test>("test") {
    // Use JUnit Platform for unit tests.
    useJUnitPlatform()
}
