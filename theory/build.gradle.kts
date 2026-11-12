import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// Pure Kotlin music logic, deliberately without any Android dependency.
plugins {
    alias(libs.plugins.kotlin.jvm)
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    testImplementation(libs.junit)
}

// StrudelTest parses the app's lesson file; without this a cached test result would hide edits to it.
tasks.test {
    inputs.file("../app/src/main/assets/strudel.md")
}
