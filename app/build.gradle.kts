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
        versionName = "1.5.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables { useSupportLibrary = true }
    }

    // ── 签名配置 ──────────────────────────────────────────────────────────────
    // release 签名从环境变量读取（由 GitHub Actions 的 build.yml 注入）。
    // 本地开发时若未设置环境变量，自动回退到 debug 签名，不影响本地调试。
    signingConfigs {
        create("release") {
            val keystorePath  = System.getenv("KEYSTORE_PATH")
            val storePassword = System.getenv("KEYSTORE_STORE_PASSWORD")
            val keyAlias      = System.getenv("KEYSTORE_KEY_ALIAS")
            val keyPassword   = System.getenv("KEYSTORE_KEY_PASSWORD")

            if (keystorePath != null && storePassword != null &&
                keyAlias != null    && keyPassword != null) {
                storeFile     = file(keystorePath)
                this.storePassword = storePassword
                this.keyAlias      = keyAlias
                this.keyPassword   = keyPassword
                println("✅ Release signing: using $keystorePath")
            } else {
                // 本地或 CI 未配置 key 时回退 debug 签名（可正常编译，但与正式签名不同）
                println("⚠️  KEYSTORE env vars not set — falling back to debug signing config")
                val debugKs = signingConfigs.getByName("debug")
                storeFile     = debugKs.storeFile
                this.storePassword = debugKs.storePassword
                this.keyAlias      = debugKs.keyAlias
                this.keyPassword   = debugKs.keyPassword
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("release")   // ← 使用正式签名
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
