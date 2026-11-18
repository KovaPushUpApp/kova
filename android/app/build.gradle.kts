import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

// La clé de signature de la version publiée. Le fichier keystore.properties et la
// clé elle-même ne sont jamais enregistrés dans Git : sans eux, la version de
// publication se construit quand même, mais non signée.
val signature = Properties().apply {
    val fichier = rootProject.file("keystore.properties")
    if (fichier.exists()) fichier.inputStream().use { load(it) }
}

// La clé du projet PostHog (statistiques d'usage), dans mesures.properties : cle=phc_...
// Sans ce fichier, l'app se construit quand même et n'envoie rien.
val mesures = Properties().apply {
    val fichier = rootProject.file("mesures.properties")
    if (fichier.exists()) fichier.inputStream().use { load(it) }
}

android {
    namespace = "com.getkova.app"
    compileSdk = 36

    signingConfigs {
        if (signature.containsKey("fichier")) {
            create("publication") {
                storeFile = rootProject.file(signature.getProperty("fichier"))
                storePassword = signature.getProperty("motDePasse")
                keyAlias = signature.getProperty("alias")
                keyPassword = signature.getProperty("motDePasse")
            }
        }
    }

    defaultConfig {
        applicationId = "com.getkova.app"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"
        buildConfigField("String", "CLE_MESURES", "\"" + mesures.getProperty("cle", "").trim() + "\"")

        // Pour un APK plus léger destiné à un vrai téléphone : ./gradlew assembleDebug -Pabi=arm64-v8a
        (project.findProperty("abi") as String?)?.let { abi -> ndk { abiFilters += abi } }
    }

    buildTypes {
        release {
            // Retire le code et les ressources inutilisés : l'app est plus légère.
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.findByName("publication")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    packaging {
        // Bibliothèques natives compressées dans l'APK : il est nettement plus léger à transmettre.
        jniLibs { useLegacyPackaging = true }
    }

    lint {
        // Les textes sont partagés avec iOS : ceux que seule l'app iOS utilise restent
        // dans le fichier, et la réduction des ressources les ignore.
        disable += "UnusedResources"
        // Chemin du SDK écrit par Android Studio sur ce poste, hors du dépôt.
        disable += "PropertyEscape"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

kotlin {
    jvmToolchain(21)
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    implementation(libs.mlkit.pose)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.billing)

    testImplementation(libs.junit)
}
