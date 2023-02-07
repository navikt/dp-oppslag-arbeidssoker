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
    mainClass.set("no.nav.dagpenger.arbeidssoker.oppslag.ApplicationKt")
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))

    implementation(Ulid.ulid)

    // ktor
    implementation(Ktor2.Client.library("cio"))
    implementation(Ktor2.Client.library("content-negotiation"))
    implementation(Ktor2.Client.library("logging"))
    implementation("io.ktor:ktor-serialization-jackson:${Ktor2.version}")
    implementation("com.github.navikt.dp-biblioteker:oauth2-klient:${Dagpenger.Biblioteker.version}")
    implementation(Jackson.jsr310)

    // mdc coroutine plugin
    implementation(Kotlin.Coroutines.module("slf4j"))

    // logging
    implementation(Kotlin.Logging.kotlinLogging)

    // miljøkonfig
    implementation(Konfig.konfig)

    // rapid rivers
    implementation(RapidAndRiversKtor2)

    // test
    testRuntimeOnly(Junit5.engine)
    testImplementation(Junit5.api)
    testImplementation(KoTest.runner)
    testImplementation(KoTest.assertions)
    testImplementation(Ktor2.Client.library("mock"))
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
    gradleVersion = "7.6"
}

tasks.named("shadowJar") {
    dependsOn("test")
}

tasks.named("compileKotlin") {
    dependsOn("spotlessCheck")
}
