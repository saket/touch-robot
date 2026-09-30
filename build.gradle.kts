import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile
import com.android.build.api.dsl.CommonExtension
import com.android.build.gradle.BasePlugin as AndroidBasePlugin

buildscript {
  dependencies {
    classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:${libs.versions.kotlin.get()}")
  }
  repositories {
    google()
    mavenCentral()
  }
}

plugins {
  alias(libs.plugins.android.application) apply false
  alias(libs.plugins.android.library) apply false
  alias(libs.plugins.compose.compiler) apply false
  alias(libs.plugins.paparazzi) apply false
  alias(libs.plugins.dropshots) apply false
  alias(libs.plugins.dokka) apply false
  alias(libs.plugins.mavenPublish) apply false
}

allprojects {
  plugins.withType<AndroidBasePlugin>().configureEach {
    configure<CommonExtension> {
      compileOptions.sourceCompatibility = JavaVersion.VERSION_11
      compileOptions.targetCompatibility = JavaVersion.VERSION_11
    }
  }
  tasks.withType<KotlinJvmCompile>().configureEach {
    compilerOptions {
      jvmTarget.set(JvmTarget.JVM_11)
    }
  }
}
