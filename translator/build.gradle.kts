plugins {
    id("com.text2sql4j.kotlin-library-conventions")
}

dependencies {
    implementation("ch.qos.logback:logback-classic")
    implementation("org.slf4j:slf4j-api")
    implementation("org.jetbrains.kotlin:kotlin-stdlib")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core")

    implementation("ai.djl:api")
    implementation("ai.djl.pytorch:pytorch-engine")
    implementation("ai.djl.huggingface:tokenizers")
    implementation("ai.djl.serving:serving")

    testImplementation("com.fasterxml.jackson.core:jackson-core")
    testImplementation("com.fasterxml.jackson.core:jackson-databind")
    testImplementation("com.fasterxml.jackson.module:jackson-module-kotlin")
}
