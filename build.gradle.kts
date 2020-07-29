import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

@Suppress("ClassName")
object versions {
    const val agp = "4.2.0-alpha06"
    const val detekt = "1.10.0"
    const val junit = "4.13"
    const val truth = "1.0.1"
}

plugins {
    `java-gradle-plugin`
    `kotlin-dsl`
    kotlin("jvm") version "1.4.0-rc"
    id("com.gradle.plugin-publish") version "0.11.0"
    id("com.vanniktech.maven.publish") version "0.12.0"
    id("io.gitlab.arturbosch.detekt") version "1.10.0"
}

kotlinDslPluginOptions {
    experimentalWarning.set(false)
}

repositories {
    mavenCentral()
    google()
    gradlePluginPortal()
}

group = property("GROUP") as String
version = property("VERSION_NAME") as String

pluginBundle {
    website = property("POM_URL") as String
    vcsUrl = property("POM_SCM_URL") as String
    tags = listOf("gradle", "gradle")
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

tasks.test {
    testLogging {
        events(TestLogEvent.PASSED, TestLogEvent.SKIPPED, TestLogEvent.FAILED)
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

tasks.withType<KotlinCompile>().configureEach {
    kotlinOptions {
        jvmTarget = "1.8"
        freeCompilerArgs = freeCompilerArgs + listOf("-Xinline-classes", "-Xopt-in=kotlin.Experimental")
    }
}

@Suppress("UnstableApiUsage")
val fixtureAgpVersion = providers
    .environmentVariable("AGP_VERSION")
    .forUseAtConfigurationTime()
    .getOrElse(versions.agp)

dependencies {
    compileOnly("com.android.tools.build:gradle:${versions.agp}")
    testImplementation("junit:junit:${versions.junit}")
    testImplementation("com.google.truth:truth:${versions.truth}")
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
