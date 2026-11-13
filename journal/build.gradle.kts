// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

plugins {
    alias(libs.plugins.pvot.android.application)
    alias(libs.plugins.pvot.android.compose)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "org.prauga.journal"

    defaultConfig {
        applicationId = "org.prauga.journal"
        versionCode = rootProject.extra["journal.versionCode"] as Int
        versionName = rootProject.extra["journal.versionName"] as String
    }

    buildTypes {
        debug { applicationIdSuffix = ".dev" }
        release { optimization { enable = true } }
    }
}

dependencies {
    implementation(project(":core:data"))
    implementation(project(":core:ui"))
    implementation(libs.pvot.design.system)
    implementation(libs.pvot.core.ui)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
}
