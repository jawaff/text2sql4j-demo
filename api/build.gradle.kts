plugins {
    id("com.text2sql4j.kotlin-application-conventions")
}

dependencies {
    implementation("ch.qos.logback:logback-classic")
    implementation("org.slf4j:slf4j-api")
    implementation("org.jetbrains.kotlin:kotlin-stdlib")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core")

    implementation("io.vertx:vertx-core")
    implementation("io.vertx:vertx-lang-kotlin")
    implementation("io.vertx:vertx-lang-kotlin-coroutines")
    implementation("io.vertx:vertx-web")
    implementation("io.vertx:vertx-web-openapi")
    implementation("io.vertx:vertx-web-validation")
    implementation("io.vertx:vertx-pg-client")
    implementation("io.vertx:vertx-sql-client")
    implementation("com.fasterxml.jackson.core:jackson-core")
    implementation("com.fasterxml.jackson.core:jackson-databind")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")

    implementation(project(":translator"))
}

application {
    // Define the main class for the application.
    mainClass.set("com.text2sql4j.api.MainKt")
}
