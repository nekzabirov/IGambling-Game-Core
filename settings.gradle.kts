rootProject.name = "IGambling"

include(":shared")
include(":syncGameJob")
include(":proto")
include(":client-api")
include(":webhook-api")
include(":event-worker")

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}
