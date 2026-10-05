// Performance investigations only, kept for historical reasons. See README.md.
// Not part of the game or the service, and not run by `./gradlew test`.

plugins {
    alias(libs.plugins.kotlin.jvm)

    // JetBrains' benchmarking toolkit. On the JVM it runs JMH (the OpenJDK
    // microbenchmark harness), which handles JIT warm-up, forking and
    // dead-code elimination so the timings can be trusted.
    alias(libs.plugins.kotlinx.benchmark)
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":fruit-machine-core"))
    implementation(libs.kotlinx.benchmark.runtime)
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

benchmark {
    targets {
        register("main")
    }
    configurations {
        named("main") {
            // Kept short so a full run takes minutes rather than hours.
            warmups = 3
            iterations = 5
            iterationTime = 1
            iterationTimeUnit = "s"
            mode = "avgt"
            outputTimeUnit = "ns"
        }
    }
}
