plugins {
    id("common")
    application
    id("org.openapi.generator") version "7.12.0"
}

repositories {
    mavenCentral()
    maven("https://packages.confluent.io/maven/")
    maven("https://github-package-registry-mirror.gc.nav.no/cached/maven-release")
}

application {
    applicationName = "dp-oppslag-arbeidssoker"
    mainClass.set("no.nav.dagpenger.arbeidssoker.oppslag.AppKt")
}

dependencies {
    implementation(project(":arbeidssoker-hendelser"))
    implementation(kotlin("stdlib-jdk8"))

    // For arbeidssøkerregisteret sin lytter
    implementation("com.github.navikt.tbd-libs:kafka:2025.03.30-13.02-f7cb11ef")
    implementation("io.confluent:kafka-avro-serializer:7.9.0")
    implementation("io.confluent:kafka-schema-registry:7.9.0")
    implementation("io.confluent:kafka-streams-avro-serde:7.9.0")

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

    testImplementation("io.specmatic:specmatic-core:2.7.4")
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
        "https://raw.githubusercontent.com/navikt/paw-arbeidssoekerregisteret-monorepo-ekstern/refs/heads/main/apps/oppslag-api/src/main/resources/openapi/documentation.yaml",
    )
    outputDir.set("${layout.buildDirectory.get()}/generated/")
    packageName.set("no.nav.paw.arbeidssøkerregister.api")
    globalProperties.set(mapOf("models" to ""))
    modelNameSuffix.set("DTO")
    configOptions.set(
        mapOf("serializationLibrary" to "jackson"),
    )
}
