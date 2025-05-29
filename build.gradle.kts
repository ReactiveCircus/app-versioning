@file:Suppress("UnstableApiUsage")

import com.vanniktech.maven.publish.SonatypeHost
import io.gitlab.arturbosch.detekt.Detekt
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    `kotlin-dsl`
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.binaryCompatibilityValidator)
    alias(libs.plugins.detekt)
    alias(libs.plugins.mavenPublish)
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
        tags.set(listOf("android", "versioning"))
        implementationClass = "io.github.reactivecircus.appversioning.AppVersioningPlugin"
    }
}

tasks.withType<KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
        freeCompilerArgs.addAll(
            "-Xjvm-default=all",
        )
    }
}

tasks.withType<JavaCompile>().configureEach {
    sourceCompatibility = JavaVersion.VERSION_17.toString()
    targetCompatibility = JavaVersion.VERSION_17.toString()
}

val fixtureClasspath: Configuration by configurations.creating
tasks.pluginUnderTestMetadata {
    pluginClasspath.from(fixtureClasspath)
}

val functionalTestSourceSet: SourceSet = sourceSets.create("functionalTest") {
    compileClasspath += sourceSets["main"].output + configurations["testRuntimeClasspath"]
    runtimeClasspath += output + compileClasspath
}

val functionalTestImplementation: Configuration = configurations.getByName("functionalTestImplementation")
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

val fixtureAgpVersion: Provider<String> = providers
    .environmentVariable("AGP_VERSION")
    .orElse(providers.gradleProperty("AGP_VERSION"))
    .orElse(libs.versions.agp)

dependencies {
    compileOnly(libs.agp.build)
    testImplementation(libs.junit)
    testImplementation(libs.truth)
    testImplementation(libs.testParameterInjector)
    fixtureClasspath(libs.agp.build.flatMap { dependency ->
        fixtureAgpVersion.map { version ->
            "${dependency.group}:${dependency.name}:$version"
        }
    })
}

detekt {
    source.from(files("src/"))
    config.from(files("${project.rootDir}/detekt.yml"))
    buildUponDefaultConfig = true
    allRules = true
}

tasks.withType<Detekt>().configureEach {
    jvmTarget = JvmTarget.JVM_22.target
    reports {
        html.outputLocation.set(file("build/reports/detekt/${project.name}.html"))
    }
}

val detektFormatting = libs.plugin.detektFormatting.get()

dependencies.add("detektPlugins", detektFormatting)
