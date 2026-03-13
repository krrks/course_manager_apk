plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.school.manager"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.school.manager"
        minSdk = 26
        targetSdk = 35
        versionCode = 5
        versionName = "1.5.2"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables { useSupportLibrary = true }
    }

    // ── 签名配置 ──────────────────────────────────────────────────────────────
    // 四个环境变量全部存在 AND keys/release.jks 文件存在时，才使用正式签名。
    // 否则自动降级到 debug 签名，确保 CI 不会因签名问题 FAILED。
    signingConfigs {
        create("release") {
            val keystorePath  = System.getenv("KEYSTORE_PATH")
            val storePassword = System.getenv("KEYSTORE_STORE_PASSWORD")
            val keyAlias      = System.getenv("KEYSTORE_KEY_ALIAS")
            val keyPassword   = System.getenv("KEYSTORE_KEY_PASSWORD")

            val ksFile = if (keystorePath != null) file(keystorePath) else null
            val hasAll = keystorePath != null && storePassword != null &&
                         keyAlias    != null && keyPassword   != null &&
                         ksFile      != null && ksFile.exists()

            if (hasAll) {
                storeFile          = ksFile
                this.storePassword = storePassword
                this.keyAlias      = keyAlias
                this.keyPassword   = keyPassword
                println("✅ Release signing: $keystorePath  alias=$keyAlias")
            } else {
                // 降级：使用 debug keystore（保证编译成功，APK 可安装但与 debug 同证书）
                println("⚠️  Release signing: env vars missing or jks not found — falling back to debug keystore")
                val debug = signingConfigs.getByName("debug")
                storeFile          = debug.storeFile
                this.storePassword = debug.storePassword
                this.keyAlias      = debug.keyAlias
                this.keyPassword   = debug.keyPassword
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("release")
        }
        debug {
            applicationIdSuffix = ".debug"
            isDebuggable = true
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs += listOf(
            "-opt-in=androidx.compose.foundation.layout.ExperimentalLayoutApi",
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
            "-opt-in=androidx.compose.foundation.ExperimentalFoundationApi",
            "-opt-in=androidx.compose.animation.ExperimentalAnimationApi"
        )
    }

    buildFeatures { compose = true }

    packaging { resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" } }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.gson)
    implementation(libs.coil.compose)
    debugImplementation(libs.androidx.ui.tooling)
}
