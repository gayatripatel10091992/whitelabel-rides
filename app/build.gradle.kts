import com.android.build.api.variant.ResValue

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

/** Launcher names per brand flavor. Everything else about a brand lives in `core/Brands.kt`. */
val brandNames = mapOf("swiftride" to "SwiftRide", "cityhop" to "CityHop", "rihla" to "Rihla")

android {
    namespace = "com.gayatripatel.rides"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.gayatripatel.rides"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
    }

    // One codebase, 3 brands x 3 apps = 9 Play Store listings.
    flavorDimensions += listOf("brand", "role")
    productFlavors {
        brandNames.keys.forEach { brand ->
            create(brand) {
                dimension = "brand"
                applicationId = "com.gayatripatel.rides.$brand"
                buildConfigField("String", "BRAND_ID", "\"$brand\"")
            }
        }
        create("rider") {
            dimension = "role"
            buildConfigField("String", "ROLE", "\"rider\"")
        }
        create("driver") {
            dimension = "role"
            applicationIdSuffix = ".driver"
            buildConfigField("String", "ROLE", "\"driver\"")
        }
        create("kiosk") {
            dimension = "role"
            applicationIdSuffix = ".kiosk"
            buildConfigField("String", "ROLE", "\"kiosk\"")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            // Demo only: signed with the debug key so CI can produce installable release APKs.
            signingConfig = signingConfigs.getByName("debug")
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
}

// "SwiftRide", "SwiftRide Driver", "SwiftRide Kiosk"…
androidComponents {
    onVariants { variant ->
        val flavors = variant.productFlavors.toMap()
        val brand = brandNames.getValue(flavors.getValue("brand"))
        val role = flavors.getValue("role")
        val label = if (role == "rider") brand else "$brand ${role.replaceFirstChar { it.uppercase() }}"
        variant.resValues.put(variant.makeResValueKey("string", "app_name"), ResValue(label, null))
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(project(":ui"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.kotlinx.coroutines.core)
}
