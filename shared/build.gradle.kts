import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
}

// Client configuration only. Never put a service-role/secret key in an app build.
val localProperties = Properties().apply {
    rootProject.file("local.properties").takeIf { it.exists() }?.inputStream()?.use { load(it) }
}
val supabaseUrl = providers.environmentVariable("SUPABASE_URL")
    .map { it.trim() }.filter { it.isNotEmpty() }
    .orElse(localProperties.getProperty("SUPABASE_URL", "").trim())
val supabaseKey = providers.environmentVariable("SUPABASE_PUBLISHABLE_KEY")
    .map { it.trim() }.filter { it.isNotEmpty() }
    .orElse(localProperties.getProperty("SUPABASE_PUBLISHABLE_KEY", "").trim())
abstract class CheckSupabaseConfig : DefaultTask() {
    @get:Input abstract val projectUrl: Property<String>
    @get:Input abstract val publishableKey: Property<String>

    @TaskAction
    fun verify() {
        val missing = buildList {
            if (projectUrl.get().isBlank()) add("SUPABASE_URL")
            if (publishableKey.get().isBlank()) add("SUPABASE_PUBLISHABLE_KEY")
        }
        check(missing.isEmpty()) {
            "Missing ${missing.joinToString()} in the saved root local.properties file. Save it, then rebuild."
        }
        check(projectUrl.get().startsWith("https://") && !projectUrl.get().contains("YOUR_PROJECT")) {
            "SUPABASE_URL must be your real https:// project URL in local.properties."
        }
        check(!publishableKey.get().startsWith("sb_secret_") &&
            !publishableKey.get().contains("YOUR_PUBLISHABLE") &&
            !publishableKey.get().contains("your_publishable")) {
            "Use your real Supabase publishable key (or legacy anon key)."
        }
        logger.lifecycle("Supabase URL and client key are present. Rebuild and relaunch the app to use them.")
    }
}
tasks.register<CheckSupabaseConfig>("checkSupabaseConfig") {
    group = "verification"
    description = "Checks saved Supabase client configuration without printing its values."
    projectUrl.set(supabaseUrl)
    publishableKey.set(supabaseKey)
}
abstract class GenerateSupabaseConfig : DefaultTask() {
    @get:Input abstract val projectUrl: Property<String>
    @get:Input abstract val publishableKey: Property<String>
    @get:OutputDirectory abstract val outputDirectory: DirectoryProperty

    @TaskAction
    fun generate() {
        fun literal(value: String) = "\"" + value.replace("\\", "\\\\")
            .replace("\"", "\\\"").replace("$", "\${'$'}{'$'}")
            .replace("\r", "\\r").replace("\n", "\\n") + "\""
        val target = outputDirectory.get().file("com/weissev/stagecall/data/SupabaseConfig.kt").asFile
        target.parentFile.mkdirs()
        target.writeText("""
            package com.weissev.stagecall.data
            internal object SupabaseConfig {
                const val url = ${literal(projectUrl.get())}
                const val publishableKey = ${literal(publishableKey.get())}
                val isConfigured get() = url.isNotBlank() && publishableKey.isNotBlank()
            }
        """.trimIndent())
    }
}
val generateSupabaseConfig by tasks.registering(GenerateSupabaseConfig::class) {
    projectUrl.set(supabaseUrl)
    publishableKey.set(supabaseKey)
    outputDirectory.set(layout.buildDirectory.dir("generated/supabase/commonMain"))
}

kotlin {
    listOf(iosArm64(), iosSimulatorArm64()).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "Shared"
            isStatic = true
        }
    }
    jvm()
    js {
        browser()
        binaries.executable()
    }
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
        binaries.executable()
    }
    android {
        namespace = "com.weissev.stagecall.shared"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()
        compilerOptions { jvmTarget = JvmTarget.JVM_11 }
        androidResources { enable = true }
        withHostTest { isIncludeAndroidResources = true }
        withDeviceTestBuilder { sourceSetTreeName = "test" }
            .configure { instrumentationRunner = "androidx.test.runner.AndroidJUnitRunner" }
    }
    sourceSets {
        androidMain.dependencies {
            implementation(libs.ktor.client.okhttp)
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.compose.uiTooling)
        }
        commonMain.dependencies {
            implementation(libs.supabase.postgrest)
            implementation(libs.supabase.auth)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)

            implementation(compose.materialIconsExtended)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
        }
        commonMain { kotlin.srcDir(generateSupabaseConfig.flatMap { it.outputDirectory }) }
        jvmMain.dependencies { implementation(libs.ktor.client.cio) }
        iosMain.dependencies { implementation(libs.ktor.client.darwin) }
        jsMain.dependencies { implementation(libs.ktor.client.js) }
        wasmJsMain.dependencies { implementation(libs.ktor.client.js) }
        jsMain.dependencies { implementation(libs.wrappers.browser) }
        getByName("androidDeviceTest").dependencies { implementation(libs.androidx.testRunner) }
    }
}

dependencies { androidRuntimeClasspath(libs.compose.uiTooling) }
