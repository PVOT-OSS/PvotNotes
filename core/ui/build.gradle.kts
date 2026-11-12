// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

plugins {
    alias(libs.plugins.pvot.android.library)
    alias(libs.plugins.pvot.android.compose)
}

android {
    namespace = "org.prauga.notes.core.ui"
    resourcePrefix = "ui_"
}

dependencies { api(libs.pvot.design.system) }
