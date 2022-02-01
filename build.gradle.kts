@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    alias(libs.plugins.gradle.versionUpdate)
    alias(libs.plugins.gradle.versionCatalogUpdate)
    alias(libs.plugins.gradle.kotlin.jvm)
    id("maven-publish")
}

group = "com.github.papsign"
version = "0.2-beta.20-SNAPSHOT"

repositories {
    mavenCentral()
}

publishing {
    publications {
        create<MavenPublication>("jarModule") {
            artifactId = "Ktor-OpenAPI-Generator"
            from(components["java"])
        }
    }
}

dependencies {
    implementation(libs.kotlin.stdlib.jdk8)
    implementation(libs.slf4j.api)
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.host.common)
    implementation(libs.ktor.server.sessions)
    implementation(libs.ktor.metrics)

    implementation(libs.ktor.jackson) // needed for parameter parsing and multipart parsing
    implementation(libs.jackson.datatype.jsr310) // needed for multipart parsing

    implementation(libs.swagger.ui)

    implementation(libs.reflections)  // only used while initializing

    testImplementation(libs.kotlin.test)
    testImplementation(libs.ktor.server.netty)
    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.logback.classic)
    testImplementation(libs.ktor.auth)
    testImplementation(libs.ktor.auth.jwt)
}

tasks.withType(org.jetbrains.kotlin.gradle.tasks.KotlinCompile::class.java) {
    kotlinOptions {
        jvmTarget = "1.8"
        freeCompilerArgs = freeCompilerArgs + listOf(
            "-Xuse-experimental=kotlin.ExperimentalStdlibApi"
        )
    }
}

tasks.withType(org.jetbrains.kotlin.gradle.tasks.KotlinCompile::class.java) {
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

tasks.withType<com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask> {
    resolutionStrategy {
        componentSelection {
            all {
                if (isNonStable(candidate.version) && !isNonStable(currentVersion)) {
                    reject("Release candidate")
                }
            }
        }
    }
}

fun isNonStable(version: String): Boolean {
    val stableKeyword = listOf("RELEASE", "FINAL", "GA").any { version.toUpperCase().contains(it) }
    val regex = "^[0-9,.v-]+(-r)?$".toRegex()
    val isStable = stableKeyword || regex.matches(version)
    return isStable.not()
}

tasks.named<Wrapper>("wrapper") {
    gradleVersion = "7.3.3"
}
