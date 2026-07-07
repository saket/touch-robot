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
  testOptions {
    unitTests {
      isIncludeAndroidResources = true
    }
  }
}

dependencies {
  api(libs.androidx.compose.foundation)
  lintChecks(libs.composeLintChecks)
  implementation(libs.androidx.ktx)
  implementation(libs.androidx.lifecycle)
  implementation(libs.androidx.savedstate)

  testImplementation(libs.junit)
  testImplementation(libs.robolectric)
  testImplementation(libs.androidx.test.ext.junit)
  testImplementation(libs.androidx.compose.ui.test.junit)
  debugImplementation(libs.androidx.compose.ui.test.manifest)
}

// The Compose UI test manifest (which supplies the launcher activity used by createComposeRule)
// is intentionally debug-only so it never ships in the published release artifact. That leaves the
// release unit test variant without an activity to launch, so skip it — debug covers these tests.
tasks.configureEach {
  if (name.contains("Release") && name.contains("UnitTest")) {
    enabled = false
  }
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
