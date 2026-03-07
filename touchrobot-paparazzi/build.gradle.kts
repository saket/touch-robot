plugins {
  alias(libs.plugins.android.library)
  alias(libs.plugins.kotlin.android)
  alias(libs.plugins.compose.compiler)
  alias(libs.plugins.paparazzi)
  alias(libs.plugins.mavenPublish)
}

android {
  namespace = "me.saket.touchrobot.paparazzi"

  defaultConfig {
    minSdk = libs.versions.minSdk.get().toInt()
    compileSdk = libs.versions.compileSdk.get().toInt()
    lint.abortOnError = true
  }
  testOptions {
    unitTests {
      isIncludeAndroidResources = true
      isReturnDefaultValues = true
    }
  }
}

dependencies {
  api(projects.touchrobotCore)
  api(libs.androidx.compose.foundation)
  api(libs.androidx.compose.ui.test.junit)
  api(libs.paparazzi)

  lintChecks(libs.composeLintChecks)

  testImplementation(libs.junit)
  testImplementation(libs.telephoto)

  // TODO: this needs to be testImplementation, but paparazzi currently fails
  //  to resolve a dependency's resources unless its part of the main sources.
  debugImplementation(libs.androidx.compose.material3)
}
