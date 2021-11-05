
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent

plugins {
    application
    kotlin("jvm") version Kotlin.version
    id(Spotless.spotless) version Spotless.version
    id(Shadow.shadow) version Shadow.version
}
apply {
    plugin(Spotless.spotless)
}

repositories {
    mavenCentral()
    maven("https://packages.confluent.io/maven/")
    maven("https://jitpack.io")
}

application {
    applicationName = "dp-oppslag-arbeidssoker"
    mainClassName = "no.nav.dagpenger.arbeidssoker.oppslag.ApplicationKt"
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))

    implementation(Ulid.ulid)

    // ktor
    implementation(Dagpenger.Biblioteker.Ktor.Client.authBearer)
    implementation(Dagpenger.Biblioteker.Ktor.Client.metrics)
    implementation(Ktor.library("client-auth-jvm"))
    implementation(Ktor.library("client-core"))
    implementation(Ktor.library("client-jackson"))
    implementation(Ktor.library("client-cio"))
    implementation(Ktor.library("client-logging-jvm"))
    implementation(Jackson.jsr310)
    implementation(Ktor.serverNetty)
    implementation("com.github.navikt.dp-biblioteker:aad-klient:2021.10.22-12.25.95ff9731951b")

    // mdc coroutine plugin
    implementation(Kotlin.Coroutines.module("slf4j"))

    // logging
    implementation(Kotlin.Logging.kotlinLogging)

    // miljøkonfig
    implementation(Konfig.konfig)

    // rapid rivers
    implementation(RapidAndRivers)

    // test
    testRuntimeOnly(Junit5.engine)
    testImplementation(Junit5.api)
    testImplementation(KoTest.runner)
    testImplementation(KoTest.assertions)
    testImplementation(Ktor.library("client-mock-jvm"))
    testImplementation(Mockk.mockk)
}

spotless {
    kotlin {
        ktlint(Ktlint.version)
    }
    kotlinGradle {
        target("*.gradle.kts", "buildSrc/**/*.kt*")
        ktlint(Ktlint.version)
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

tasks.withType<Test> {
    useJUnitPlatform()
    testLogging {
        showExceptions = true
        showStackTraces = true
        exceptionFormat = TestExceptionFormat.FULL
        events = setOf(TestLogEvent.PASSED, TestLogEvent.SKIPPED, TestLogEvent.FAILED)
        showStandardStreams = true
    }
}

tasks.withType<Wrapper> {
    gradleVersion = "6.2.2"
}

tasks.named("shadowJar") {
    dependsOn("test")
}

tasks.named("compileKotlin") {
    dependsOn("spotlessCheck")
}
