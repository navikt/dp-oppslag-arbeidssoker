plugins {
    id("common")
    application
    id("org.openapi.generator") version "7.26.0"
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

@Suppress("ktlint:standard:max-line-length")
val pawOpenApiSpec =
    resources.text.fromUri(
        "https://raw.githubusercontent.com/navikt/paw-arbeidssoekerregisteret-monorepo-ekstern/refs/heads/main/apps/oppslag-api-v2/src/main/resources/openapi/v1-spec.yaml",
    )

dependencies {
    implementation(project(":arbeidssoker-hendelser"))
    implementation(kotlin("stdlib-jdk8"))

    // For arbeidssøkerregisteret sin lytter
    implementation("com.github.navikt.tbd-libs:kafka:2026.04.14-17.02-3f7000fd")
    implementation("io.confluent:kafka-avro-serializer:8.4.0")

    // ktor
    implementation(libs.bundles.ktor.client)

    implementation("no.nav.dagpenger:oauth2-klient:2025.12.19-08.15.2e150cd55270")
    implementation(libs.bundles.jackson)

    // mdc coroutine plugin
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-slf4j:1.11.0")

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

    testImplementation(libs.testcontainer)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
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

tasks.processTestResources {
    from(provider { pawOpenApiSpec.asFile() }) {
        rename { "paw-openapi.yaml" }
    }
}

openApiGenerate {
    generatorName.set("kotlin")
    inputSpec.fileProvider(provider { pawOpenApiSpec.asFile() })
    outputDir.set("${layout.buildDirectory.get()}/generated/")
    packageName.set("no.nav.paw.arbeidssøkerregister.api")
    globalProperties.set(mapOf("models" to ""))
    modelNameSuffix.set("DTO")
    configOptions.set(
        mapOf("serializationLibrary" to "jackson"),
    )
}
