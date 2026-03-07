pluginManagement {
  repositories {
    google()
    mavenCentral()
    gradlePluginPortal()
  }
}

dependencyResolutionManagement {
  @Suppress("UnstableApiUsage")
  repositories {
    google()
    mavenCentral()
  }
}

include(":touchrobot-core")
include(":touchrobot-paparazzi")
include(":sample")

rootProject.name = "touch-robot"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
