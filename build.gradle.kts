plugins {
    id("com.github.ben-manes.versions") version "0.41.0"
    id("org.jetbrains.kotlin.jvm") version "1.5.21"
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
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("org.slf4j:slf4j-api:1.7.30")
    implementation("io.ktor:ktor-server-core:1.6.1")
    implementation("io.ktor:ktor-server-host-common:1.6.1")
    implementation("io.ktor:ktor-metrics:1.6.1")
    implementation("io.ktor:ktor-server-sessions:1.6.1")

    implementation("io.ktor:ktor-jackson:1.6.1") // needed for parameter parsing and multipart parsing
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.9.8") // needed for multipart parsing
    implementation("org.webjars:swagger-ui:3.25.0")

    implementation("org.reflections:reflections:0.9.11") // only used while initializing

    testImplementation("org.jetbrains.kotlin:kotlin-test")
    testImplementation("io.ktor:ktor-server-netty:1.6.1")
    testImplementation("io.ktor:ktor-server-test-host:1.6.1")
    testImplementation("ch.qos.logback:logback-classic:1.2.1")
    testImplementation("io.ktor:ktor-auth:1.6.1")
    testImplementation("io.ktor:ktor-auth-jwt:1.6.1")
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

tasks.named<Wrapper>("wrapper") {
    gradleVersion = "7.3.3"
}
