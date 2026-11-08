plugins {
    alias(libs.plugins.kotlin.multiplatform)
}

kotlin {
    jvmToolchain(17)

    // Die Android-App nutzt das JVM-Ziel. Die Apple-Ziele (iosArm64, iosSimulatorArm64, macosArm64)
    // kommen in Phase 1i dazu, sie lassen sich nur auf einem Mac bauen.
    jvm()

    sourceSets {
        all {
            // kotlinx-datetime 0.7 nutzt kotlin.time.Instant, das in Kotlin 2.2 noch experimentell ist.
            languageSettings.optIn("kotlin.time.ExperimentalTime")
        }
        commonMain.dependencies {
            api(libs.kotlinx.datetime)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}
