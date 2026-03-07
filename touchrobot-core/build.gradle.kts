plugins {
  alias(libs.plugins.android.library)
  alias(libs.plugins.kotlin.android)
  alias(libs.plugins.compose.compiler)
  alias(libs.plugins.mavenPublish)
}

android {
  namespace = "me.saket.touchrobot"

  defaultConfig {
    minSdk = libs.versions.minSdk.get().toInt()
    compileSdk = libs.versions.compileSdk.get().toInt()
    lint.abortOnError = true
  }
}

dependencies {
  api(libs.androidx.compose.foundation)
  lintChecks(libs.composeLintChecks)
  implementation(libs.androidx.ktx)
  implementation(libs.androidx.lifecycle)
  implementation(libs.androidx.savedstate)
}

// Used on CI to prevent publishing of non-snapshot versions.
tasks.register("throwIfVersionIsNotSnapshot") {
  doLast {
    val libraryVersion = properties["VERSION_NAME"] as String
    check(libraryVersion.endsWith("SNAPSHOT")) {
      "Project isn't using a snapshot version = $libraryVersion"
    }
  }
}
