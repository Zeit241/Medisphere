import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.navigation.safeargs)
    alias(libs.plugins.kotlin.parcelize)
    id("org.jlleitschuh.gradle.ktlint")
    id("org.jetbrains.kotlinx.kover")
}

configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
    filter {
        exclude("**/androidTest/**")
    }
}

val localProperties =
    Properties().apply {
        val f = rootProject.file("local.properties")
        if (f.exists()) {
            FileInputStream(f).use { load(it) }
        }
    }

/** Static token Directus (те же права, что у админки на файлы) — для Glide GET /assets без 403 */
val directusStaticToken: String = localProperties.getProperty("directus.static.token", "")

fun escapeForBuildConfigField(value: String): String = value.replace("\\", "\\\\").replace("\"", "\\\"")

android {
    namespace = "com.example.kursovaya"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.kursovaya"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        // Хост ПК с эмулятора; совпадает с Retrofit BASE_URL. Для физ. устройства — IP машины в LAN.
        buildConfigField("String", "LOOPBACK_REPLACEMENT_HOST", "\"10.0.2.2\"")
        buildConfigField(
            "String",
            "DIRECTUS_STATIC_TOKEN",
            "\"${escapeForBuildConfigField(directusStaticToken)}\"",
        )
    }
    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
    sourceSets {
        getByName("test") {
            assets.srcDirs("src/test/assets")
        }
    }
    buildTypes {
        debug {
            // URL картинок с бэка часто приходят как http://localhost:8055/... — на эмуляторе туда ведёт 10.0.2.2
            buildConfigField("boolean", "REWRITE_LOOPBACK_IN_IMAGE_URLS", "true")
        }
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            buildConfigField("boolean", "REWRITE_LOOPBACK_IN_IMAGE_URLS", "false")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    testOptions {
        unitTests {
            isReturnDefaultValues = true
            isIncludeAndroidResources = true
        }
    }
}

kover {
    reports {
        filters {
            includes {
                classes(
                    "com.example.kursovaya.util.ServicePriceFormatter",
                    "com.example.kursovaya.repository.DoctorsListCache",
                    "com.example.kursovaya.model.api.AppointmentApiExtensionsKt",
                    "com.example.kursovaya.model.api.DoctorApiExtensionsKt",
                )
            }
        }
        verify {
            rule {
                minBound(70)
            }
        }
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.maplibre.sdk)
    implementation(libs.jts.core)
    implementation(libs.places)
    testImplementation(libs.junit)
    testImplementation("org.mockito:mockito-core:5.1.1")
    testImplementation("org.mockito:mockito-inline:5.2.0")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.1.0")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
    testImplementation("com.squareup.retrofit2:retrofit:2.9.0")
    testImplementation("com.squareup.retrofit2:converter-gson:2.9.0")
    testImplementation("org.robolectric:robolectric:4.14.1")
    testImplementation("androidx.test:core:1.6.1")
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    implementation("androidx.security:security-crypto:1.1.0-alpha06")
//    implementation("com.google.android.material:material:1.11.0")
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)

    // Retrofit
    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
    implementation(libs.gson)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)

    // Lifecycle
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)

    // Glide
    implementation(libs.glide)

    // STOMP WebSocket client
    implementation("com.github.NaikSoftware:StompProtocolAndroid:1.6.6")
    implementation("io.reactivex.rxjava2:rxjava:2.2.21")
    implementation("io.reactivex.rxjava2:rxandroid:2.1.1")

    // Input mask
    implementation("com.redmadrobot:input-mask-android:7.2.4")
}
