// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

plugins { alias(libs.plugins.pvot.android.library) }

android {
    namespace = "org.prauga.notes.core.ical"
    defaultConfig { consumerProguardFiles("consumer-rules.pro") }
}

dependencies {
    api(project(":core:model"))
    implementation(libs.ical4j)
}
