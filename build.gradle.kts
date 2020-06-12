import org.gradle.api.tasks.testing.logging.TestLogEvent

val agpVersion = "4.1.0-beta01"

plugins {
    `java-gradle-plugin`
    `kotlin-dsl`
    kotlin("jvm") version "1.3.72"
    id("com.gradle.plugin-publish") version "0.11.0"
    id("com.vanniktech.maven.publish") version "0.11.1"
    id("io.gitlab.arturbosch.detekt") version "1.9.1"
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
    val appVersioning by plugins.creating {
        id = "io.github.reactivecircus.appversioning"
        displayName = "Android App Versioning Gradle Plugin."
        description = "Gradle plugin for lazily generating Android app's versionCode & versionName from Git tags."
        implementationClass = "io.github.reactivecircus.appversioning.AppVersioningPlugin"
    }
}

val fixtureClasspath by configurations.creating

tasks.withType<PluginUnderTestMetadata> {
    pluginClasspath.from(configurations.getByName("fixtureClasspath"))
}

val functionalTestSourceSet = sourceSets.create("functionalTest") {}

gradlePlugin.testSourceSets(functionalTestSourceSet)
configurations.getByName("functionalTestImplementation").extendsFrom(configurations.getByName("testImplementation"))

val functionalTest by tasks.creating(Test::class) {
    testClassesDirs = functionalTestSourceSet.output.classesDirs
    classpath = functionalTestSourceSet.runtimeClasspath
}

val check by tasks.getting(Task::class) {
    dependsOn(functionalTest)
}

tasks.withType<Test> {
    maxParallelForks = Runtime.getRuntime().availableProcessors() * 2
    testLogging {
        events(org.gradle.api.tasks.testing.logging.TestLogEvent.PASSED, TestLogEvent.SKIPPED, TestLogEvent.FAILED)
    }
}

dependencies {
    implementation(platform("org.jetbrains.kotlin:kotlin-bom"))
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    compileOnly("com.android.tools.build:gradle:$agpVersion")

    testImplementation("org.jetbrains.kotlin:kotlin-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit")
    testImplementation("com.android.tools.build:gradle:$agpVersion")
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
