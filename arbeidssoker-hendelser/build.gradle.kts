import com.github.davidmc24.gradle.plugin.avro.GenerateAvroProtocolTask

plugins {
    id("common")
    id("com.github.davidmc24.gradle.plugin.avro") version "1.9.1"
}

repositories {
    mavenCentral()
    maven("https://github-package-registry-mirror.gc.nav.no/cached/maven-release")
}

val schema by configurations.creating {
    isTransitive = false
}

dependencies {
    // Lese hendelser fra arbeidssøkerregistrering
    api("org.apache.avro:avro:1.11.0")
    schema("no.nav.paw.arbeidssokerregisteret.api:main-avro-schema:1.13764081353.1-2")
}

tasks.named("generateAvroProtocol", GenerateAvroProtocolTask::class.java) {
    source(zipTree(schema.singleFile))
}

tasks.compileKotlin {
    dependsOn("generateAvroProtocol")
}
