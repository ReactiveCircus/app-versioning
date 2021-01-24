import org.gradle.api.tasks.testing.logging.TestLogEvent

@Suppress("ClassName")
object versions {
    const val agp = "4.2.0-beta03"
    const val agpCommon = "27.2.0-beta03"
    const val detekt = "1.15.0"
    const val junit = "4.13.1"
    const val truth = "1.1.2"
}

plugins {
    `java-gradle-plugin`
    `kotlin-dsl`
    kotlin("jvm") version "1.4.30-RC"
    id("org.jetbrains.dokka") version "1.4.0"
    id("com.gradle.plugin-publish") version "0.12.0"
    id("com.vanniktech.maven.publish") version "0.13.0"
    id("io.gitlab.arturbosch.detekt") version "1.15.0"
}

kotlinDslPluginOptions {
    experimentalWarning.set(false)
}

repositories {
    mavenCentral()
    google()
    gradlePluginPortal()
    maven("https://dl.bintray.com/kotlin/kotlin-eap")
}

group = property("GROUP") as String
version = property("VERSION_NAME") as String

pluginBundle {
    website = property("POM_URL") as String
    vcsUrl = property("POM_SCM_URL") as String
    tags = listOf("gradle", "android", "versioning")
    mavenCoordinates {
        groupId = property("GROUP") as String
        artifactId = property("POM_ARTIFACT_ID") as String
    }
}

gradlePlugin {
    plugins.create("appVersioning") {
        id = "io.github.reactivecircus.app-versioning"
        displayName = "Android App Versioning Gradle Plugin."
        description = "Gradle plugin for lazily generating Android app's versionCode & versionName from Git tags."
        implementationClass = "io.github.reactivecircus.appversioning.AppVersioningPlugin"
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

kotlin {
    target {
        compilations.all {
            kotlinOptions {
                jvmTarget = "11"
                freeCompilerArgs = freeCompilerArgs + listOf(
                    "-Xjvm-default=all",
                    "-Xinline-classes",
                    "-Xopt-in=kotlin.Experimental"
                )
            }
        }
    }
}

val fixtureClasspath: Configuration by configurations.creating
tasks.pluginUnderTestMetadata {
    pluginClasspath.from(fixtureClasspath)
}

val functionalTestSourceSet = sourceSets.create("functionalTest") {
    compileClasspath += sourceSets["main"].output + configurations["testRuntimeClasspath"]
    runtimeClasspath += output + compileClasspath
}

val functionalTestImplementation = configurations.getByName("functionalTestImplementation")
    .extendsFrom(configurations.getByName("testImplementation"))

gradlePlugin.testSourceSets(functionalTestSourceSet)

val functionalTest by tasks.registering(Test::class) {
    failFast = true
    testLogging {
        events(TestLogEvent.PASSED, TestLogEvent.SKIPPED, TestLogEvent.FAILED)
    }
    testClassesDirs = functionalTestSourceSet.output.classesDirs
    classpath = functionalTestSourceSet.runtimeClasspath
}

val check by tasks.getting(Task::class) {
    dependsOn(functionalTest)
}

val test by tasks.getting(Test::class) {
    testLogging {
        events(TestLogEvent.PASSED, TestLogEvent.SKIPPED, TestLogEvent.FAILED)
    }
}

@Suppress("UnstableApiUsage")
val fixtureAgpVersion = providers
    .environmentVariable("AGP_VERSION")
    .forUseAtConfigurationTime()
    .orElse(providers.gradleProperty("AGP_VERSION").forUseAtConfigurationTime())
    .getOrElse(versions.agp)

dependencies {
    compileOnly("com.android.tools.build:gradle:${versions.agp}")
    compileOnly("com.android.tools:common:${versions.agpCommon}")
    testImplementation("junit:junit:${versions.junit}")
    testImplementation("com.google.truth:truth:${versions.truth}")
    functionalTestImplementation("com.android.tools.build:gradle:${fixtureAgpVersion}")
    fixtureClasspath("com.android.tools.build:gradle:${fixtureAgpVersion}")
}

detekt {
    input = files("src/")
    failFast = true
    config = files("${project.rootDir}/detekt.yml")
    buildUponDefaultConfig = true
    reports {
        html.destination = file("${project.buildDir}/reports/detekt/${project.name}.html")
    }
}

dependencies.add("detektPlugins", "io.gitlab.arturbosch.detekt:detekt-formatting:${versions.detekt}")
