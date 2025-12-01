import com.google.protobuf.gradle.id

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.protobuf)
    application
}

group = "com.nekgamebling"
version = "1.0.0"

kotlin {
    jvmToolchain(21)
}

application {
    mainClass.set("com.nekgamebling.MainKt")
}

dependencies {
    // Ktor Server
    implementation(libs.bundles.ktor.server)

    // Ktor Client
    implementation(libs.bundles.ktor.client)

    // Serialization
    implementation(libs.ktor.serialization.json)

    // Database - Exposed ORM
    implementation(libs.bundles.exposed)
    implementation(libs.h2)
    implementation(libs.postgresql)

    // Dependency Injection - Koin
    implementation(libs.bundles.koin)

    // Logging
    implementation(libs.logback)

    // DateTime
    implementation(libs.kotlinx.datetime)

    // Messaging - RabbitMQ
    implementation(libs.rabbitmq)

    // gRPC
    implementation(libs.bundles.grpc)
    implementation(libs.protobuf.kotlin)

    // Testing
    testImplementation(libs.bundles.testing)
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:4.29.2"
    }
    plugins {
        id("grpc") {
            artifact = "io.grpc:protoc-gen-grpc-java:1.68.2"
        }
        id("grpckt") {
            artifact = "io.grpc:protoc-gen-grpc-kotlin:1.4.1:jdk8@jar"
        }
    }
    generateProtoTasks {
        all().forEach {
            it.plugins {
                id("grpc")
                id("grpckt")
            }
            it.builtins {
                id("kotlin")
            }
        }
    }
}