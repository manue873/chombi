plugins {
    kotlin("jvm") // usa la misma versión de Kotlin que tu app
}

java {
    toolchain { languageVersion.set(JavaLanguageVersion.of(17)) }
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
    testImplementation(kotlin("test"))
}
