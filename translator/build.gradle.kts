plugins {
    id("com.text2sql4j.kotlin-library-conventions")
}

dependencies {
    implementation("ch.qos.logback:logback-classic")
    implementation("org.slf4j:slf4j-api")
    implementation("org.jetbrains.kotlin:kotlin-stdlib")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core")

    implementation(platform("ai.djl:bom:0.21.0"))
    implementation("ai.djl:api")
    implementation("ai.djl.pytorch:pytorch-engine")
    implementation("ai.djl.huggingface:tokenizers")
    implementation("ai.djl.serving:serving:0.21.0")

    implementation("io.vertx:vertx-core")
    implementation("io.vertx:vertx-lang-kotlin")
    implementation("io.vertx:vertx-lang-kotlin-coroutines")
    implementation("com.fasterxml.jackson.core:jackson-core")
    implementation("com.fasterxml.jackson.core:jackson-databind")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
}
