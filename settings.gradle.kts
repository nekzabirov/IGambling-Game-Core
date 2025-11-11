rootProject.name = "IGambling"

//include(":shared")
include(":aggregators")
include(":catalog")
include(":syncGameJob")

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}
