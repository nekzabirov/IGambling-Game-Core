rootProject.name = "IGambling"

include(":core")
include(":game-sync-job")
include(":proto")
include(":grpc-api")
include(":aggregator-api")
include(":event-worker")

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}
