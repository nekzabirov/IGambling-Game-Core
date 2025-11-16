rootProject.name = "IGambling"

include(":shared")
include(":syncGameJob")
include(":proto")
include(":client-api")
include(":webhook-api")

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}
