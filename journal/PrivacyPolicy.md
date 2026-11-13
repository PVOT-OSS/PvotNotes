<!--
SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
SPDX-License-Identifier: Apache-2.0
-->

# Privacy Policy — Journal

_Effective 14 November 2026_

Journal (package `org.prauga.journal`) is a diary app published by Saalim Quadri for PVOT-OSS. This
policy explains what the app does with your data.

**In short: the app doesn't collect, send, share or sell any personal data. It has no accounts, no
ads, no analytics and no tracking, and it has no permission to use the internet.**

## What the app keeps on your device

- **Your entries**: each entry's date and time, title, text and tags, when it was created and last
  changed, and an internal identifier and revision number.
- **Your photos**: copies of the photos you add to an entry.
- **A setting**: whether the timeline is grouped by day, week or month.

All of this stays in the app's private storage. Deleting an entry removes it and its photo copies
from the app straight away; the originals in your gallery are not touched. Everything is deleted
when you clear the app's storage or uninstall it.

## Photos

When you add photos, you pick them in Android's photo picker, and the app gets access to only the
photos you pick. It copies them into its private storage, so the entry keeps them even if you delete
the original. The app can't see the rest of your gallery and needs no storage permission.

## Backups

If you turn on Android's device backup, Android may include your entries, their photos and the
app's setting in your device backup or in a transfer to a new device (for example, Google's backup
on most devices), under the terms of that backup service. The app itself never sends them anywhere.
You can turn backup off in your device's settings.

## Sharing

- When you share text or a photo from another app to Journal, the app saves it as a new entry on
  your device.
- When you share an entry, the app passes its title and text to the app you pick in Android's share
  sheet. What happens to it there is up to that app.

Nothing is shared unless you choose to share it.

## Links

When you tap a link in an entry, Android opens it in another app, such as your browser. Journal
itself doesn't load anything from the internet.

## The home screen shortcut

The app adds a "New entry" shortcut to your launcher and tells Android when you use it, so the
launcher can rank its shortcuts. This stays on your device.

## Permissions

The app requests no permissions. The only one listed in the app,
`org.prauga.journal.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`, is added by the AndroidX library to keep
the app's internal broadcasts private; it gives the app no access to your data.

The app does not have the `INTERNET` permission.

## Children

The app is not directed at children and collects no data from anyone, children included.

## Changes

If the app starts handling data differently, this policy will be updated before that version is
released, with a new effective date at the top.

## Contact

Questions about this policy: danascape@gmail.com, or open an issue at
https://github.com/PVOT-OSS/Notes/issues.
