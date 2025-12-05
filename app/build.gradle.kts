plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.amobear.freevpn"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.amobear.freevpn"
        minSdk = 24
        targetSdk = 36
        versionCode = 605147600 // Matching ProtonVPN
        versionName = "5.14.76.0" // Matching ProtonVPN

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        
        // BuildConfig fields matching ProtonVPN Android app exactly
        buildConfigField("String", "STORE_SUFFIX", "\"+play\"")
        buildConfigField("String", "ACCOUNT_SENTRY_DSN", "\"\"")
        buildConfigField("Boolean", "ALLOW_LOGCAT", "false")
        buildConfigField("Boolean", "ALT_ROUTING_CERT_FOR_MAIN_ROUTE", "false")
        buildConfigField("String[]", "API_ALT_TLS_PINS", "null")
        buildConfigField("String[]", "API_TLS_PINS", "null")
        buildConfigField("String", "CI_BRANCH_NAME", "\"null\"")
        buildConfigField("String", "CI_COMMIT_MESSAGE", "\"null\"")
        buildConfigField("String[]", "DOH_SERVICES_URLS", "null")
        buildConfigField("String", "PREF_KEY", "\"Key\"")
        buildConfigField("String", "PREF_SALT", "\"Salt\"")
        buildConfigField("String", "SPECIAL_CHAR_PASSWORD", "\"null\"")
        buildConfigField("String", "Sentry_DSN", "\"\"")
        buildConfigField("String", "TEST_ACCOUNT_PASSWORD", "\"Pass\"")
        buildConfigField("String", "TEST_ASSET_OVERRIDE_KEY", "\"241b080fb0f1216e\"")
        buildConfigField("String", "TEST_ASSET_OVERRIDE_SHA", "\"db95210d57b90063bfa1cac9881c7a8df326e6e2\"")
        buildConfigField("String", "TEST_SUITE_ASSET_OVERRIDE_KEY", "\"3ea60f6a2fbfc015\"")
        buildConfigField("String", "VPN_SERVER_ROOT_CERT", "null")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
        buildConfig = true // Enable BuildConfig generation
    }

    lint {
        disable += setOf("ProtectedPermissions")
    }

    packaging {
        jniLibs {
            useLegacyPackaging = true
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)

    // OpenVPN module
    implementation(project(":openvpn"))

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.android.compiler)
    implementation(libs.hilt.navigation.compose)

    // Room
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // Retrofit
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging.interceptor)

    // Coroutines
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)

    // Navigation
    implementation(libs.navigation.compose)

    // ViewModel
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.lifecycle.runtime.compose)

    // Serialization
    implementation(libs.kotlinx.serialization.json)

    // DataStore
    implementation(libs.datastore.preferences)

    // Accompanist Permissions
    implementation(libs.accompanist.permissions)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}