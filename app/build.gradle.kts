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
        versionCode = 18
        versionName = "1.018"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables { useSupportLibrary = true }
    }

    // ── 签名配置 ──────────────────────────────────────────────────────────────
    // 只有四个环境变量全齐且 jks 文件真实存在时，才创建 release signingConfig。
    // 否则 release buildType 不指定 signingConfig（= null），Gradle 自动用 debug key。
    // 重要：不在 signingConfigs.create 块里引用 signingConfigs.getByName("debug")，
    //       避免 Gradle 配置阶段的循环初始化异常。
    val keystorePath  = System.getenv("KEYSTORE_PATH")
    val storePassword = System.getenv("KEYSTORE_STORE_PASSWORD")
    val keyAlias      = System.getenv("KEYSTORE_KEY_ALIAS")
    val keyPassword   = System.getenv("KEYSTORE_KEY_PASSWORD")
    val ksFile        = if (keystorePath != null) file(keystorePath) else null
    val useReleaseKey = keystorePath  != null && storePassword != null &&
                        keyAlias      != null && keyPassword   != null &&
                        ksFile        != null && ksFile.exists()

    if (useReleaseKey) {
        signingConfigs {
            create("release") {
                storeFile          = ksFile
                this.storePassword = storePassword
                this.keyAlias      = keyAlias
                this.keyPassword   = keyPassword
            }
        }
        println("✅ Release signing: using $keystorePath (alias=$keyAlias)")
    } else {
        println("⚠️  Release signing: env vars missing or jks not found — APK will be signed with debug key")
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            // 有 release key 就用，没有就让 Gradle 用默认的 debug key（不会 FAILED）
            signingConfig = if (useReleaseKey)
                signingConfigs.getByName("release")
            else
                signingConfigs.getByName("debug")
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
