plugins {
    id("common")
    application
}

repositories {
    mavenCentral()
    maven("https://github-package-registry-mirror.gc.nav.no/cached/maven-release")
}

application {
    applicationName = "dp-oppslag-arbeidssoker"
    mainClass.set("no.nav.dagpenger.arbeidssoker.oppslag.AppKt")
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))

    implementation("de.huxhorn.sulky:de.huxhorn.sulky.ulid:8.3.0")

    // ktor
    implementation(libs.bundles.ktor.client)

    implementation(libs.dp.biblioteker.oauth2.klient)
    implementation(libs.bundles.jackson)

    // mdc coroutine plugin
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-slf4j:1.8.0")

    // logging
    implementation(libs.kotlin.logging)
    implementation(libs.ktor.serialization.jackson)
    // miljøkonfig
    implementation(libs.konfig)

    // rapid rivers
    implementation(libs.rapids.and.rivers)

    // test
    testImplementation(libs.kotest.assertions.core)
    testImplementation(libs.ktor.client.mock)
    testImplementation(libs.mockk)
}
