import java.text.SimpleDateFormat
import java.util.Date
import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}

val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties()
if (keystorePropertiesFile.exists()) {
    keystoreProperties.load(keystorePropertiesFile.inputStream())
}

android {
    namespace = "com.mj.yaja"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.mj.yaja"
        minSdk = 26
        targetSdk = 36
        versionCode = 94
        versionName = "3.1.1"


        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    signingConfigs {
        if (keystorePropertiesFile.exists()) {
            create("release") {
                storeFile = file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
                storeType = keystoreProperties.getProperty("storeType")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            ndk {
                debugSymbolLevel = "FULL"
            }
            if (keystorePropertiesFile.exists()) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

// Stamped fresh into every build (not just release) so the About screen's
// build line reflects exactly when this APK was assembled, not just the day.
val generatedBuildInfoDir = layout.buildDirectory.dir("generated/buildInfo/main/kotlin")
val generateBuildInfo by tasks.registering {
    // Captured from this task-local val, not the outer script-level val — referencing
    // the outer val from inside doLast pulls the whole build script into config-cache
    // serialization, which Gradle rejects.
    val outputDir = layout.buildDirectory.dir("generated/buildInfo/main/kotlin")
    outputs.dir(outputDir)
    outputs.upToDateWhen { false }
    doLast {
        val timestamp = SimpleDateFormat("ddMMyyyyHHmm").format(Date())
        val packageDir = outputDir.get().asFile.resolve("com/mj/yaja")
        packageDir.mkdirs()
        packageDir.resolve("BuildInfo.kt").writeText(
            """
            package com.mj.yaja

            internal object BuildInfo {
                const val BUILD_DATE = "$timestamp"
            }
            """.trimIndent()
        )
    }
}
android.sourceSets.getByName("main").kotlin.srcDir(generatedBuildInfoDir.get().asFile)
// Name-matched, not withType<KotlinCompile>() — KSP's kspDebugKotlin/kspReleaseKotlin
// tasks also read this source dir and need the same explicit dependency, but they
// aren't KotlinCompile tasks.
tasks.matching { it.name.contains("Kotlin") }.configureEach {
    dependsOn(generateBuildInfo)
}

// Compose compiler stability/recomposition reports.
// Off by default (zero build overhead); enable with: ./gradlew assembleDebug -PcomposeMetrics=true
// Output lands in app/build/compose_compiler/*-composables.txt — inspect "unstable" params.
composeCompiler {
    if (project.findProperty("composeMetrics") == "true") {
        reportsDestination = layout.buildDirectory.dir("compose_compiler")
        metricsDestination = layout.buildDirectory.dir("compose_compiler")
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.core:core-remoteviews:1.0.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation(platform("androidx.compose:compose-bom:2024.10.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.navigation:navigation-compose:2.7.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("androidx.documentfile:documentfile:1.0.1")
    implementation("androidx.work:work-runtime-ktx:2.9.1")

    // For Material icons (extended)
    implementation("androidx.compose.material:material-icons-extended")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // Splash Screen
    implementation("androidx.core:core-splashscreen:1.0.1")

    // AppCompat — required for AppCompatActivity (provides FragmentActivity for BiometricPrompt)
    implementation("androidx.appcompat:appcompat:1.7.0")

    // Biometric authentication
    implementation("androidx.biometric:biometric:1.1.0")

    // Reorderable LazyColumn for drag-to-reorder
    implementation("sh.calvin.reorderable:reorderable:2.4.3")

    // ML Kit Language Identification (optional, toggled by user in Statistics)
    implementation("com.google.mlkit:language-id:17.0.6")

    implementation("com.joaomgcd:taskerpluginlibrary:0.4.10")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20240303")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.10.01"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    // Room
    val roomVersion = "2.6.1"
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    ksp("androidx.room:room-compiler:$roomVersion")
}
