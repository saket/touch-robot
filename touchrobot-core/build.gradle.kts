plugins {
  alias(libs.plugins.android.library)
  alias(libs.plugins.compose.compiler)
  alias(libs.plugins.dropshots)
  alias(libs.plugins.mavenPublish)
}

android {
  namespace = "me.saket.touchrobot"

  compileSdk = libs.versions.compileSdk.get().toInt()
  lint.abortOnError = true
  testOptions {
    targetSdk = libs.versions.compileSdk.get().toInt()
    managedDevices.localDevices {
      create("pixel8api34") {
        device = "Pixel 8"
        apiLevel = 34
        systemImageSource = "google"
      }
    }
  }

  defaultConfig {
    minSdk = libs.versions.minSdk.get().toInt()
    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }
}

dependencies {
  api(libs.androidx.compose.foundation)
  lintChecks(libs.composeLintChecks)
  implementation(libs.androidx.ktx)
  implementation(libs.androidx.lifecycle)
  implementation(libs.androidx.savedstate)

  androidTestImplementation(libs.androidx.activityCompose)
  androidTestImplementation(libs.androidx.compose.ui.test.junit)
  androidTestImplementation(libs.androidx.test.core)
  androidTestImplementation(libs.androidx.test.espresso.core)
  androidTestImplementation(libs.androidx.test.ext.junit)
  androidTestImplementation(libs.androidx.test.runner)
  androidTestImplementation(libs.dropshots)
  androidTestImplementation(libs.junit)
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
