// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.room) apply false
}

extra.apply {
    fun version(app: String, major: Int, minor: Int, patch: Int) {
        set("$app.versionCode", major * 10000 + minor * 100 + patch)
        set("$app.versionName", "$major.$minor.$patch")
    }
    version("notes", 1, 0, 0)
    version("reminders", 1, 0, 0)
    version("journal", 1, 0, 0)
}
