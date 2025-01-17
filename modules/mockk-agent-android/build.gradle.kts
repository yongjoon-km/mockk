import buildsrc.config.androidClassesDexAttributes
import buildsrc.config.asConsumer

plugins {
    buildsrc.convention.`android-library`

    buildsrc.convention.`mockk-publishing`
}

description = "Android instrumented testing MockK inline mocking agent"

val mavenName: String by extra("MockK Android Agent")
val mavenDescription: String by extra("${project.description}")

@Suppress("UnstableApiUsage")
android {
    externalNativeBuild {
        cmake {
            path = file("CMakeLists.txt")
        }
    }

    sourceSets {
        named("main").configure {
            resources {
                srcDirs(dispatcherJarResPath)
            }
        }
    }

    defaultConfig {
        testInstrumentationRunner = "android.support.test.runner.AndroidJUnitRunner"
        testInstrumentationRunnerArguments["notAnnotation"] = "io.mockk.test.SkipInstrumentedAndroidTest"
        ndk {
            abiFilters += setOf("armeabi-v7a", "x86", "x86_64", "arm64-v8a")
        }
    }
}

val androidClassesDex: Configuration by configurations.creating {
    description = "Fetch Android classes.dex files"
    asConsumer()
    androidClassesDexAttributes()
}

dependencies {
    api(projects.modules.mockkAgentApi)
    api(projects.modules.mockkAgent)

    implementation(kotlin("reflect"))
    implementation("com.linkedin.dexmaker:dexmaker:${buildsrc.config.Deps.Versions.dexmaker}")
    implementation("org.objenesis:objenesis:${buildsrc.config.Deps.Versions.objenesis}")

    androidTestImplementation("androidx.test.espresso:espresso-core:${buildsrc.config.Deps.Versions.androidxEspresso}") {
        exclude("com.android.support:support-annotations")
    }

    androidTestImplementation(kotlin("test"))

    androidClassesDex(projects.modules.mockkAgentAndroidDispatcher)
}

val dispatcherJarResPath: Provider<Directory> = layout.buildDirectory.dir("generated/dispatcher-jar")

val packageDispatcherJar by tasks.registering(Jar::class) {
    group = LifecycleBasePlugin.BUILD_GROUP
    from(androidClassesDex.asFileTree)
    archiveFileName.set("dispatcher.jar")
    destinationDirectory.set(dispatcherJarResPath)
}

tasks.preBuild {
    dependsOn(packageDispatcherJar)
}
