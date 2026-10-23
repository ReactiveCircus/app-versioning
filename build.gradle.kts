import dev.detekt.gradle.Detekt
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.jetbrains.kotlin.gradle.dsl.JvmDefaultMode
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.abi.ExperimentalAbiValidation

plugins {
    `java-gradle-plugin`
    alias(libs.plugins.kotlin.gradle.samWithReceiver)
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.detekt)
    alias(libs.plugins.mavenPublish)
    alias(libs.plugins.burst)
    alias(libs.plugins.testkit)
}

group = property("GROUP") as String
version = property("VERSION_NAME") as String

mavenPublishing {
    publishToMavenCentral(automaticRelease = true)
    signAllPublications()
}

gradleTestKitSupport {
    disablePublication()
}

samWithReceiver {
    annotation(HasImplicitReceiver::class.qualifiedName!!)
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

kotlin {
    @OptIn(ExperimentalAbiValidation::class)
    abiValidation()
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
        jvmDefault.set(JvmDefaultMode.NO_COMPATIBILITY)
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

val fixtureClasspath = configurations.create("fixtureClasspath")
tasks.pluginUnderTestMetadata {
    pluginClasspath.from(fixtureClasspath)
}

tasks.withType<Test>().configureEach {
    testLogging {
        events(TestLogEvent.PASSED, TestLogEvent.SKIPPED, TestLogEvent.FAILED)
    }
}

tasks.functionalTest {
    failFast = true
}

tasks.test {
    maxParallelForks = (Runtime.getRuntime().availableProcessors() / 2).coerceAtLeast(1)
}

val fixtureAgpVersion: Provider<String> = providers
    .environmentVariable("AGP_VERSION")
    .orElse(providers.gradleProperty("AGP_VERSION"))
    .orElse(libs.versions.agp)

dependencies {
    compileOnly(libs.agp.build.api)
    testImplementation(kotlin("test-junit"))
    functionalTestImplementation(kotlin("test-junit"))
    functionalTestImplementation(libs.burst)
    fixtureClasspath(libs.agp.build.gradle.flatMap { dependency ->
        fixtureAgpVersion.map { version ->
            "${dependency.group}:${dependency.name}:$version"
        }
    })
    detektPlugins(libs.plugin.detektKtlintWrapper)
}

detekt {
    source.from(files("src/"))
    config.from(files("${project.rootDir}/detekt.yml"))
    buildUponDefaultConfig = true
}

tasks.withType<Detekt>().configureEach {
    jvmTarget = JvmTarget.JVM_17.target
    reports {
        checkstyle.required.set(false)
        sarif.required.set(false)
        markdown.required.set(false)
        html.outputLocation.set(file("build/reports/detekt/${project.name}.html"))
    }
}
