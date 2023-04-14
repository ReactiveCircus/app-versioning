@file:Suppress("UnstableApiUsage")

import com.vanniktech.maven.publish.SonatypeHost
import io.gitlab.arturbosch.detekt.Detekt
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

@Suppress("ClassName")
object versions {
    const val agp = "8.0.0"
    const val agpCommon = "31.0.0"
    const val detekt = "1.22.0"
    const val junit = "4.13.2"
    const val truth = "1.1.3"
}

plugins {
    `java-gradle-plugin`
    `kotlin-dsl`
    kotlin("jvm") version "1.8.20"
    id("com.gradle.plugin-publish") version "1.2.0"
    id("com.vanniktech.maven.publish") version "0.25.1"
    id("io.gitlab.arturbosch.detekt") version "1.22.0"
    id("binary-compatibility-validator") version "0.13.0"
}

group = property("GROUP") as String
version = property("VERSION_NAME") as String

mavenPublishing {
    publishToMavenCentral(SonatypeHost.S01, automaticRelease = true)
    signAllPublications()
}

gradlePlugin {
    website.set(property("POM_URL") as String)
    vcsUrl.set(property("POM_SCM_URL") as String)
    plugins.create("appVersioning") {
        id = "io.github.reactivecircus.app-versioning"
        displayName = "Android App Versioning Gradle Plugin."
        description = "Gradle plugin for lazily generating Android app's versionCode & versionName from Git tags."
        tags.set(listOf("gradle", "android", "versioning"))
        implementationClass = "io.github.reactivecircus.appversioning.AppVersioningPlugin"
    }
}

kotlin {
    jvmToolchain {
        languageVersion.set(JavaLanguageVersion.of(19))
        vendor.set(JvmVendorSpec.AZUL)
    }
}

tasks.withType<KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_11)
        languageVersion.set(KotlinVersion.KOTLIN_1_8)
        freeCompilerArgs.addAll(
            "-Xjvm-default=all",
            "-opt-in=kotlin.RequiresOptIn",
            "-Xbackend-threads=0",
        )
    }
}

tasks.withType<JavaCompile>().configureEach {
    sourceCompatibility = JavaVersion.VERSION_11.toString()
    targetCompatibility = JavaVersion.VERSION_11.toString()
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
    maxParallelForks = (Runtime.getRuntime().availableProcessors() / 2).coerceAtLeast(1)
    testLogging {
        events(TestLogEvent.PASSED, TestLogEvent.SKIPPED, TestLogEvent.FAILED)
    }
}

val fixtureAgpVersion = providers
    .environmentVariable("AGP_VERSION")
    .orElse(providers.gradleProperty("AGP_VERSION"))
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
    source = files("src/")
    config = files("${project.rootDir}/detekt.yml")
    buildUponDefaultConfig = true
    allRules = true
}

tasks.withType<Detekt>().configureEach {
    jvmTarget = JvmTarget.JVM_11.target
    reports {
        html.outputLocation.set(file("build/reports/detekt/${project.name}.html"))
    }
}

dependencies.add("detektPlugins", "io.gitlab.arturbosch.detekt:detekt-formatting:${versions.detekt}")
