import java.util.Properties

val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}
fun localProp(key: String, default: String = ""): String =
    (localProps.getProperty(key) ?: default).replace("\\", "\\\\").replace("\"", "\\\"")

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}
android {
    namespace = "ai.orhan"
    compileSdk = libs.versions.compileSdk.get().toInt()
    defaultConfig {
        applicationId = "ai.orhan"
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        // CI passes -PbuildNumber=<github.run_number>; local builds default to 1.
        val buildNumber = (project.findProperty("buildNumber") as String?)?.toIntOrNull()
            ?: System.getenv("BUILD_NUMBER")?.toIntOrNull() ?: 1
        versionCode = buildNumber
        versionName = "0.1.$buildNumber"
        buildConfigField("String", "DUA_ENGINE_URL", "\"${localProp("DUA_ENGINE_URL", "https://dua-ai-engine-228305912525.us-central1.run.app")}\"")
        buildConfigField("String", "DUA_API_URL", "\"${localProp("DUA_API_URL", "https://dua-api-228305912525.us-central1.run.app")}\"")
        buildConfigField("String", "SUPABASE_URL", "\"${localProp("SUPABASE_URL")}\"")
        buildConfigField("String", "SUPABASE_ANON_KEY", "\"${localProp("SUPABASE_ANON_KEY")}\"")
    }
    buildTypes {
        release { isMinifyEnabled = false }
        // Dev-only tokens; never baked into release builds.
        debug {
            buildConfigField("String", "DUA_DEV_JWT", "\"${localProp("DUA_DEV_JWT")}\"")
            buildConfigField("String", "DUA_APP_TOKEN", "\"${localProp("DUA_APP_TOKEN")}\"")
        }
    }
    defaultConfig {
        // Release-safe defaults: empty strings so BuildConfig always compiles.
        buildConfigField("String", "DUA_DEV_JWT", "\"\"")
        buildConfigField("String", "DUA_APP_TOKEN", "\"\"")
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    // Ship the repo's license notices as offline assets (Settings → About → Open-source licences).
    sourceSets["main"].assets.srcDir(layout.buildDirectory.dir("generated/licenseAssets"))
}

// Single source of truth: THIRD-PARTY-NOTICES.md + licenses/*.txt at repo root are copied
// into generated assets at build time, so the in-app text can never drift from the repo.
val copyLicenseAssets by tasks.registering(Copy::class) {
    from(rootProject.file("THIRD-PARTY-NOTICES.md"))
    from(rootProject.file("licenses")) { into("licenses") }
    into(layout.buildDirectory.dir("generated/licenseAssets/oss"))
}
tasks.named("preBuild") { dependsOn(copyLicenseAssets) }
dependencies {
    implementation(platform(libs.compose.bom))
    implementation(libs.bundles.compose)
    implementation(libs.bundles.settings.feature)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    debugImplementation(libs.bundles.compose.debug)
    testImplementation(libs.junit)
    testImplementation("org.json:json:20240303") // real org.json for request-shape tests (android.jar stubs throw)
}
