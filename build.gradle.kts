import com.vanniktech.maven.publish.SonatypeHost
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

@Suppress("ClassName")
object versions {
    const val agp = "7.4.0"
    const val agpCommon = "30.4.0"
    const val detekt = "1.22.0"
    const val junit = "4.13.2"
    const val truth = "1.1.3"
}

plugins {
    `java-gradle-plugin`
    `kotlin-dsl`
    kotlin("jvm") version "1.8.0"
    id("com.gradle.plugin-publish") version "0.12.0"
    id("com.vanniktech.maven.publish") version "0.23.2"
    id("io.gitlab.arturbosch.detekt") version "1.19.0"
    id("binary-compatibility-validator") version "0.12.1"
    id("com.autonomousapps.plugin-best-practices-plugin") version "0.2"
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

mavenPublishing {
    publishToMavenCentral(SonatypeHost.S01, automaticRelease = true)
    signAllPublications()
}

gradlePlugin {
    plugins.create("appVersioning") {
        id = "io.github.reactivecircus.app-versioning"
        displayName = "Android App Versioning Gradle Plugin."
        description = "Gradle plugin for lazily generating Android app's versionCode & versionName from Git tags."
        implementationClass = "io.github.reactivecircus.appversioning.AppVersioningPlugin"
    }
}

kotlin {
    jvmToolchain {
        languageVersion.set(JavaLanguageVersion.of(11))
        vendor.set(JvmVendorSpec.AZUL)
    }
}

tasks.withType<KotlinCompile>().configureEach {
    compilerOptions {
        languageVersion.set(KotlinVersion.KOTLIN_1_8)
        freeCompilerArgs.addAll(
            "-Xjvm-default=all",
            "-Xinline-classes",
            "-opt-in=kotlin.Experimental",
            "-opt-in=kotlin.RequiresOptIn",
            "-Xbackend-threads=0",
        )
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

tasks.detektMain {
    reports {
        html.outputLocation.set(file("build/reports/detekt/${project.name}.html"))
    }
}

dependencies.add("detektPlugins", "io.gitlab.arturbosch.detekt:detekt-formatting:${versions.detekt}")
