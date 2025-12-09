plugins {
    id("common")
    id("io.github.androa.gradle.plugin.avro") version "0.0.12"
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
    api("org.apache.avro:avro:1.12.1")
    schema("no.nav.paw.arbeidssokerregisteret.api:main-avro-schema:1.13764081353.1-2")
}

generateAvro {
    schemas.from(zipTree(schema.singleFile))
}
