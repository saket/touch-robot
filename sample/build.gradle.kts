plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.compose.compiler)
}

android {
  namespace = "me.saket.touchrobot.sample"
  compileSdk = libs.versions.compileSdk.get().toInt()

  defaultConfig {
    applicationId = namespace
    minSdk = 31
    targetSdk = libs.versions.compileSdk.get().toInt()
    versionCode = 1
    versionName = "1.0"
  }
  lint {
    abortOnError = true
  }
}

dependencies {
  implementation(projects.touchrobotCore)

  lintChecks(libs.composeLintChecks)
  implementation(libs.androidx.appcompat)
  implementation(libs.androidx.activityCompose)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.material3)
}
