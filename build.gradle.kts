import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("multiplatform")
    //kotlin("jvm") version "1.9.0"
   kotlin("plugin.serialization") version "1.6.0"
    id("org.jetbrains.compose")
}

group = "com.bondgraph"
version = "1.0-SNAPSHOT"

repositories {
    google()
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
}

kotlin {
    jvm {
        jvmToolchain(11)
        withJava()
    }
    sourceSets {
        val jvmMain by getting {
            dependencies {
                implementation(compose.desktop.currentOs)
            }
        }

        val commonMain by getting{
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-cbor:1.6.0")
            }
        }
        val jvmTest by getting
    }
}



compose.desktop {
    application {
        mainClass = "MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "Bond_Graph"
            packageVersion = "1.0.0"
        }
    }
}
