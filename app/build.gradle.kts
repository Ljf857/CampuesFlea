plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.example.campusflea"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.campusflea"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // APM 性能监控：开关与上报地址由 gradle.properties 注入，
        // 改采集策略只需改配置，不用动代码（见 gradle.properties 中的 apm.* 字段）
        buildConfigField("boolean", "APM_ENABLED", (project.findProperty("apm.enabled") as String? ?: "true"))
        buildConfigField("String", "APM_APP_ID", "\"${project.findProperty("apm.appId") as String? ?: "campus-flea"}\"")
        buildConfigField("String", "APM_UPLOAD_URL", "\"${project.findProperty("apm.uploadUrl") as String? ?: ""}\"")

        // 服务端地址：由 gradle.properties 的 api.* 注入，切换环境只改配置、不动代码。
        // api.wsUrl 留空时按 api.baseUrl 自动推导（http->ws / https->wss，末尾补 /ws）。
        val apiBaseUrl = (project.findProperty("api.baseUrl") as String?) ?: "http://47.120.62.89"
        val apiWsUrl = (project.findProperty("api.wsUrl") as String?)?.takeIf { it.isNotBlank() }
            ?: apiBaseUrl.replaceFirst("https://", "wss://")
                .replaceFirst("http://", "ws://")
                .trimEnd('/') + "/ws"
        buildConfigField("String", "API_BASE_URL", "\"$apiBaseUrl\"")
        buildConfigField("String", "WS_BASE_URL", "\"$apiWsUrl\"")
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
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.coil.compose)
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.gson)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
