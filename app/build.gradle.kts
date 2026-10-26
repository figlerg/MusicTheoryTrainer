import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

// Optional own signing key, see README. Without it the release build uses the debug key.
val keystoreProperties: Properties? = rootProject.file("keystore.properties")
    .takeIf { it.exists() }
    ?.let { file -> Properties().apply { file.inputStream().use(::load) } }

// CI has no keystore.properties and passes the same values through the environment instead.
fun signingValue(variable: String, property: String): String? =
    System.getenv(variable)?.takeIf { it.isNotBlank() } ?: keystoreProperties?.getProperty(property)

val signingStore: String? = signingValue("KEYSTORE_FILE", "storeFile")
val signingStorePassword: String? = signingValue("KEYSTORE_PASSWORD", "storePassword")
val signingAlias: String? = signingValue("KEY_ALIAS", "keyAlias")
val signingKeyPassword: String? = signingValue("KEY_PASSWORD", "keyPassword") ?: signingStorePassword

android {
    namespace = "at.gigler.musictheorytrainer"
    // The Compose BOM needs at least API 37 to compile against; targetSdk stays at 36.
    compileSdk = 37

    defaultConfig {
        applicationId = "at.gigler.musictheorytrainer"
        minSdk = 26
        targetSdk = 36
        versionCode = 6
        versionName = "0.4.1"
    }

    signingConfigs {
        if (signingStore != null) {
            create("release") {
                storeFile = rootProject.file(signingStore)
                storePassword = signingStorePassword
                keyAlias = signingAlias
                keyPassword = signingKeyPassword
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.findByName("release") ?: signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }
}

dependencies {
    implementation(project(":theory"))
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.datastore.preferences)

    testImplementation(libs.junit)
}
