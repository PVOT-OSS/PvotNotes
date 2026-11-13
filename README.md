<!--
SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
SPDX-License-Identifier: Apache-2.0
-->

# Notes, Reminders and Journal

Three private, offline Android apps on one shared core, built with Kotlin, Jetpack Compose and
[PvotLib](https://github.com/PVOT-OSS/PvotLib). Entries are stored in an iCalendar-shaped model
(RFC 5545), so they can be exported to and imported from `.ics` files.

| app | applicationId | what |
|---|---|---|
| `notes/` | `org.prauga.notes` | Notes with Markdown, checklists, tags, colours and collections |
| `reminders/` | `org.prauga.reminders` | Tasks with due dates, repeats, reminders, priority and subtasks |
| `journal/` | `org.prauga.journal` | Dated entries on a timeline, with photos and tags |

## Modules

| path | what |
|---|---|
| `core/model/` | `:core:model` — the entry model, pure Kotlin |
| `core/ical/` | `:core:ical` — iCalendar mapping and recurrence (ical4j) |
| `core/data/` | `:core:data` — Room database and repository |
| `core/ui/` | `:core:ui` — shared Compose components and the Markdown renderer |
| `build-logic/convention/` | `pvot.android.application`, `pvot.android.library`, `pvot.android.compose`, `pvot.hilt`, `pvot.jvm.library` |
| `PvotLib/` | the PvotLib design system, as a git submodule |

## Build

```sh
git submodule update --init
./gradlew assembleDebug assembleRelease testDebugUnitTest lintDebug
```

JDK 21 and the Android SDK are required. Debug builds install as `<applicationId>.dev`.

## Release

Each app keeps its store listing (fastlane layout, read by Play and F-Droid) in
`<app>/fastlane/metadata/android/` and its privacy policy in `<app>/PrivacyPolicy.md`. F-Droid build
recipes are in `fdroid/metadata/`. Releases are tagged per app: `notes-vX.Y.Z`, `reminders-vX.Y.Z`,
`journal-vX.Y.Z`; versions are set in the root `build.gradle.kts`.

## Licence

Apache-2.0, see `LICENSES/`. The project follows [REUSE](https://reuse.software/).
