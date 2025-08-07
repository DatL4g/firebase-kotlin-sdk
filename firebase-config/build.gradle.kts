import org.jetbrains.dokka.gradle.engine.parameters.VisibilityModifier
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmCompilerOptions
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSetTree

/*
 * Copyright (c) 2020 GitLive Ltd.  Use of this source code is governed by the Apache 2.0 license.
 */

val libVersion = project.property("firebase-config.version") as String
version = libVersion

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.native.cocoapods)
    alias(libs.plugins.multiplatform)
    alias(libs.plugins.dokka)
    alias(libs.plugins.publish)
    `maven-publish`
    signing
    id("testOptionsConvention")
}

dokka {
    dokkaSourceSets {
        configureEach {
            documentedVisibilities.set(setOf(VisibilityModifier.Public))
            includes.setFrom("documentation.md")

            sourceLink {
                localDirectory.set(file("src"))
                remoteUrl("https://github.com/DatL4g/firebase-kotlin-sdk/tree/master/${project.name}/src")
            }
            if (this.name == "jsMain") {
                perPackageOption {
                    // External files for JS should not be documented since they will not be available
                    matchingRegex.set(".*.externals.*")
                    suppress.set(true)
                }
            }
        }
    }
}

android {
    val minSdkVersion: Int by project
    val compileSdkVersion: Int by project

    compileSdk = compileSdkVersion
    namespace = "dev.gitlive.firebase.remoteconfig"

    defaultConfig {
        minSdk = minSdkVersion
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    testOptions.configureTestOptions(project)
    packaging {
        resources.pickFirsts.add("META-INF/kotlinx-serialization-core.kotlin_module")
        resources.pickFirsts.add("META-INF/AL2.0")
        resources.pickFirsts.add("META-INF/LGPL2.1")
    }
    lint {
        abortOnError = false
    }
}

val supportIosTarget = project.property("skipIosTarget") != "true"

kotlin {
    explicitApi()

    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }
    targets.configureEach {
        compilations.configureEach {
            compileTaskProvider.configure {
                compilerOptions {
                    if (this is KotlinJvmCompilerOptions) {
                        jvmTarget = JvmTarget.JVM_17
                    }
                    freeCompilerArgs.add("-Xexpect-actual-classes")
                }
            }
        }
    }

    @Suppress("OPT_IN_USAGE")
    androidTarget {
        instrumentedTestVariant.sourceSetTree.set(KotlinSourceSetTree.test)
        unitTestVariant.sourceSetTree.set(KotlinSourceSetTree.test)
        publishAllLibraryVariants()
    }

    jvm()

    if (supportIosTarget) {
        iosArm64()
        iosX64()
        iosSimulatorArm64()
        tvosArm64()
        tvosX64()
        tvosSimulatorArm64()
        macosArm64()
        macosX64()
        cocoapods {
            ios.deploymentTarget = libs.versions.ios.deploymentTarget.get()
            tvos.deploymentTarget = libs.versions.tvos.deploymentTarget.get()
            osx.deploymentTarget = libs.versions.macos.deploymentTarget.get()
            framework {
                baseName = "FirebaseConfig"
            }
            noPodspec()
            pod("FirebaseRemoteConfig") {
                version = libs.versions.firebase.cocoapods.get()
                extraOpts += listOf("-compiler-option", "-fmodules")
            }
        }
    }

    js(IR) {
        useCommonJs()
        browser {
            testTask {
                useKarma {
                    useChromeHeadless()
                }
            }
        }
    }

    sourceSets {
        all {
            languageSettings.apply {
                progressiveMode = true
                optIn("kotlinx.coroutines.ExperimentalCoroutinesApi")
                if (name.lowercase().contains("ios")
                    || name.lowercase().contains("apple")
                    || name.lowercase().contains("tvos")
                    || name.lowercase().contains("macos")
                ) {
                    optIn("kotlinx.cinterop.ExperimentalForeignApi")
                    optIn("kotlinx.cinterop.BetaInteropApi")
                }
            }
        }

        commonMain.dependencies {
            api(project(":firebase-app"))
            implementation(project(":firebase-common"))
            api(libs.kotlinx.datetime)
        }
        commonTest.dependencies {
            implementation(project(":test-utils"))
        }

        androidMain.dependencies {
            api(libs.google.firebase.config)
        }

        val jvmMain by getting {
            kotlin.srcDir("src/androidMain/kotlin")
        }
    }
}

if (project.property("firebase-config.skipIosTests") == "true") {
    tasks.forEach {
        if (it.name.contains("ios", true) && it.name.contains("test", true)) { it.enabled = false }
    }
}

if (project.property("firebase-config.skipJvmTests") == "true") {
    tasks.forEach {
        if (it.name.contains("jvm", true) && it.name.contains("test", true)) { it.enabled = false }
    }
}

if (project.property("firebase-config.skipJsTests") == "true") {
    tasks.forEach {
        if (it.name.contains("js", true) && it.name.contains("test", true)) { it.enabled = false }
    }
}

mavenPublishing {
    publishToMavenCentral(automaticRelease = true)
    signAllPublications()

    coordinates(
        groupId = "dev.datlag.firebase",
        artifactId = "firebase-config",
        version = libVersion
    )

    pom {
        name.set("firebase-kotlin-sdk")
        description.set("The Firebase Kotlin SDK is a Kotlin-first SDK for Firebase. It's API is similar to the Firebase Android SDK Kotlin Extensions but also supports multiplatform projects, enabling you to use Firebase directly from your common source targeting iOS, Android or JS.")
        url.set("https://github.com/DatL4g/firebase-kotlin-sdk")
        inceptionYear.set("2019")

        scm {
            url.set("https://github.com/DatL4g/firebase-kotlin-sdk")
            connection.set("scm:git:https://github.com/DatL4g/firebase-kotlin-sdk.git")
            developerConnection.set("scm:git:https://github.com/DatL4g/firebase-kotlin-sdk.git")
            tag.set("HEAD")
        }

        issueManagement {
            system.set("GitHub Issues")
            url.set("https://github.com/DatL4g/firebase-kotlin-sdk/issues")
        }

        developers {
            developer {
                name.set("Nicholas Bransby-Williams")
                email.set("nbransby@gmail.com")
            }
            developer {
                id.set("DatL4g")
                name.set("Jeff Retz")
                url.set("https://github.com/DatL4g")
            }
        }

        licenses {
            license {
                name.set("The Apache Software License, Version 2.0")
                url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                distribution.set("repo")
                comments.set("A business-friendly OSS license")
            }
        }
    }
}