plugins { kotlin("jvm") }

java { toolchain { languageVersion.set(JavaLanguageVersion.of(17)) } }

dependencies {
    api(project(":core"))
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
    testImplementation(kotlin("test"))
}
