import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent

plugins {
    application
    kotlin("jvm") version Kotlin.version
    id(Spotless.spotless) version Spotless.version
    id(Shadow.shadow) version Shadow.version
}

buildscript {
    repositories {
        jcenter()
    }
}

apply {
    plugin(Spotless.spotless)
}

repositories {
    mavenCentral()
    jcenter()
    maven("http://packages.confluent.io/maven/")
    maven("https://jitpack.io")
}

application {
    applicationName = "dp-arbeidssoker-oppslag"
    mainClassName = "no.nav.dagpenger.arbeidssoker.oppslag.ApplicationKt"
}

java {
    sourceCompatibility = JavaVersion.VERSION_12
    targetCompatibility = JavaVersion.VERSION_12
}

val tjenestespesifikasjonerVersion = "1.2019.09.25-00.21-49b69f0625e0"
fun tjenestespesifikasjon(name: String) = "no.nav.tjenestespesifikasjoner:$name:$tjenestespesifikasjonerVersion"
val cxfVersion = "3.3.4"

dependencies {
    implementation(kotlin("stdlib-jdk8"))

    // kafka
    implementation(Kafka.clients)
    implementation(Kafka.streams)

    // http
    implementation(Fuel.fuel)
    implementation(Fuel.library("moshi"))
    implementation(Moshi.moshi)
    implementation(Moshi.moshiKotlin)
    implementation(Moshi.moshiAdapters)

    // json
    implementation(Json.library)
    implementation(Ulid.ulid)

    // ktor
    implementation(Ktor.serverNetty)

    // logging
    implementation(Kotlin.Logging.kotlinLogging)

    // miljøkonfig
    implementation(Konfig.konfig)

    // rapid rivers
    implementation("com.github.navikt:rapids-and-rivers:master-SNAPSHOT")

    // test
    testRuntimeOnly(Junit5.engine)
    testImplementation(Junit5.api)
    testImplementation(Junit5.kotlinRunner)

    testImplementation(Kafka.streamTestUtils)
    testImplementation("org.awaitility:awaitility:4.0.1")
    testImplementation("no.nav:kafka-embedded-env:2.3.0")
    testImplementation(Mockk.mockk)
    testImplementation(Wiremock.standalone)

    // Soap stuff
    implementation("javax.xml.ws:jaxws-api:2.3.1")
    implementation("com.sun.xml.ws:jaxws-tools:2.3.0.2")

    implementation(tjenestespesifikasjon("nav-oppfoelgingsstatus-v2-tjenestespesifikasjon"))

    implementation("org.apache.cxf:cxf-rt-features-logging:$cxfVersion")
    implementation("org.apache.cxf:cxf-rt-frontend-jaxws:$cxfVersion")
    implementation("org.apache.cxf:cxf-rt-ws-policy:$cxfVersion")
    implementation("org.apache.cxf:cxf-rt-ws-security:$cxfVersion")
    implementation("org.apache.cxf:cxf-rt-transports-http:$cxfVersion")
    implementation("javax.activation:activation:1.1.1")
    implementation("no.nav.helse:cxf-prometheus-metrics:dd7d125")
    testImplementation("org.apache.cxf:cxf-rt-transports-http:$cxfVersion")
    // Soap stuff end
}

spotless {
    kotlin {
        ktlint(Klint.version)
    }
    kotlinGradle {
        target("*.gradle.kts", "additionalScripts/*.gradle.kts")
        ktlint(Klint.version)
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
    }
}

tasks.withType<Wrapper> {
    gradleVersion = "6.2.2"
}

tasks.named("shadowJar") {
    dependsOn("test")
}

tasks.named("jar") {
    dependsOn("test")
}

tasks.named("compileKotlin") {
    dependsOn("spotlessCheck")
}
