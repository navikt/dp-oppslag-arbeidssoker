plugins {
    id("common")
    application
    id("org.openapi.generator") version "7.11.0"
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

    implementation("no.nav.dagpenger:oauth2-klient:2024.12.19-12.57.9d42f60a1165")
    implementation(libs.bundles.jackson)

    // mdc coroutine plugin
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-slf4j:1.10.1")

    // logging
    implementation(libs.kotlin.logging)
    implementation(libs.ktor.serialization.jackson)
    // miljøkonfig
    implementation(libs.konfig)

    // rapid rivers
    implementation(libs.rapids.and.rivers)

    // test
    testImplementation(libs.kotest.assertions.core)
    testImplementation(libs.rapids.and.rivers.test)
    testImplementation(libs.ktor.client.mock)
    testImplementation(libs.mockk)

    testImplementation("io.specmatic:specmatic-core:2.3.0")
}

sourceSets {
    main {
        java {
            srcDir("${layout.buildDirectory.get()}/generated/src/main/kotlin")
        }
    }
}

ktlint {
    filter {
        exclude { element -> element.file.path.contains("generated/") }
    }
}

tasks.compileKotlin {
    dependsOn("openApiGenerate")
}
tasks.runKtlintFormatOverMainSourceSet {
    dependsOn("openApiGenerate")
}
tasks.runKtlintCheckOverMainSourceSet {
    dependsOn("openApiGenerate")
}

@Suppress("ktlint:standard:max-line-length")
openApiGenerate {
    generatorName.set("kotlin")
    remoteInputSpec.set(
        "https://raw.githubusercontent.com/navikt/paw-arbeidssoekerregisteret-api-oppslag/main/src/main/resources/openapi/documentation.yaml",
    )
    outputDir.set("${layout.buildDirectory.get()}/generated/")
    packageName.set("no.nav.paw.arbeidssøkerregister.api")
    globalProperties.set(mapOf("models" to ""))
    modelNameSuffix.set("DTO")
    configOptions.set(
        mapOf("serializationLibrary" to "jackson"),
    )
}
