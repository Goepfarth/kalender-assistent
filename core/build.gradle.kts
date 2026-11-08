plugins {
    alias(libs.plugins.kotlin.multiplatform)
}

kotlin {
    jvmToolchain(17)

    // Die Android-App nutzt das JVM-Ziel. Die Apple-Ziele (iosArm64, iosSimulatorArm64, macosArm64)
    // kommen in Phase 1i dazu, sie lassen sich nur auf einem Mac bauen.
    jvm()

    sourceSets {
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}
