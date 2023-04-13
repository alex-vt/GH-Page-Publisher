import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.io.FileInputStream
import java.util.*

plugins {
    id("com.android.application")
    kotlin("multiplatform")
    id("org.jetbrains.compose")
    kotlin("plugin.serialization")
    id("com.google.devtools.ksp") version "1.8.0-1.0.8"
}

repositories {
    google()
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
}

kotlin {
    android()
    jvm("desktop")

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(compose.ui)
                implementation(compose.foundation)
                implementation(compose.animation)
                implementation(compose.material)
                implementation(compose.materialIconsExtended)
                implementation(compose.runtime)
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.4.1")

                implementation("me.tatarka.inject:kotlin-inject-runtime:0.6.1")
                configurations["ksp"].dependencies.add(
                    project.dependencies.create(
                        "me.tatarka.inject:kotlin-inject-compiler-ksp:0.6.1"
                    )
                )

                api("moe.tlaster:precompose:1.3.15")
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(kotlin("test-common"))
                implementation(kotlin("test-annotations-common"))

                implementation("org.jetbrains.kotlin:kotlin-test:1.6.0")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.6.4")
            }
        }

        val androidMain by getting {
            dependsOn(commonMain)
            kotlin.srcDirs(
                "build/generated/ksp/android/androidDebug/kotlin",
                "build/generated/ksp/android/androidRelease/kotlin",
            )
            dependencies {
                api("androidx.appcompat:appcompat:1.6.1")
                api("androidx.core:core-ktx:1.10.0")
                implementation("androidx.activity:activity-compose:1.7.0")

                implementation("androidx.security:security-crypto:1.0.0")

                // See https://github.com/yappy/ajgit/blob/master/ajgit-lib/build.gradle
                implementation("com.jcraft:jsch:0.1.50")
                implementation("com.googlecode.javaewah:JavaEWAH:0.7.9")
                implementation("org.slf4j:slf4j-log4j12:1.7.2")
            }
        }

        val desktopMain by getting {
            dependsOn(commonMain)
            kotlin.srcDirs(
                "build/generated/ksp/desktop/desktopMain/kotlin",
            )
            dependencies {
                implementation(compose.desktop.currentOs) {
                    // prevent kotlinx.coroutines.internal.FastServiceLoader.loadProviders()
                    // from loading Android version of Dispatchers.Main with higher priority than desktop
                    exclude("org.jetbrains.kotlinx", "kotlinx-coroutines-android")
                }
                implementation("org.eclipse.jgit:org.eclipse.jgit:6.5.0.202303070854-r")
            }
        }

    }
}

compose.desktop {
    application {
        mainClass = "com.alexvt.publisher.Main_desktopKt"
    }
}

tasks.withType<ProcessResources> {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "17"
}

android {
    compileSdk = 33

    defaultConfig {
        minSdk = 30
        targetSdk = 33
    }

    val signingProperties = Properties()
    val signingPropertiesFile = file("signing.properties")
    val signingPropertiesExist = signingPropertiesFile.exists()
    if (signingPropertiesExist) signingProperties.load(FileInputStream(signingPropertiesFile))

    signingConfigs {
        create("release") {
            storeFile =
                if (signingPropertiesExist) file(signingProperties["signingStoreLocation"] as String) else null
            storePassword = signingProperties["signingStorePassword"] as String
            keyAlias = signingProperties["signingKeyAlias"] as String
            keyPassword = signingProperties["signingKeyPassword"] as String
        }
    }

    buildTypes {
        getByName("release") {
            signingConfig = signingConfigs.getByName("release")
            isDebuggable = false
        }
        getByName("debug") {
            signingConfig = signingConfigs.getByName("debug")
            isDebuggable = true
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    packagingOptions {
        exclude("META-INF/DEPENDENCIES")
        exclude("META-INF/LICENSE")
        exclude("META-INF/LICENSE.txt")
        exclude("META-INF/license.txt")
        exclude("META-INF/NOTICE")
        exclude("META-INF/NOTICE.txt")
        exclude("META-INF/notice.txt")
        exclude("META-INF/ASL2.0")
        pickFirst("META-INF/*")
    }

    sourceSets {
        named("main") {
            manifest.srcFile("src/androidMain/AndroidManifest.xml")
            res.srcDirs("src/androidMain/res", "src/androidMain/resources", "src/commonMain/resources")
            java.srcDirs("src/androidMain/kotlin")
        }
    }
}
