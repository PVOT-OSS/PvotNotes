<!--
SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
SPDX-License-Identifier: Apache-2.0
-->

# Privacy Policy — Reminders

_Effective 14 November 2026_

Reminders (package `org.prauga.reminders`) is a task and reminder app published by Saalim Quadri for
PVOT-OSS. This policy explains what the app does with your data.

**In short: the app doesn't collect, send, share or sell any personal data. It has no accounts, no
ads, no analytics and no tracking, and it has no permission to use the internet.**

## What the app keeps on your device

- **Your tasks**: each task's title, notes, due date and time, repeat rule, reminders, priority,
  subtasks and whether it's done, when it was created and last changed, and an internal identifier
  and revision number.
- **Reminder bookkeeping**: when the app last checked for reminders that came due, and the time of
  any reminder you snoozed, so it can ring again after a restart.

All of this stays in the app's private storage. Deleting a task removes it, with its subtasks, from
the app straight away. Everything is deleted when you clear the app's storage or uninstall it.

## Backups

If you turn on Android's device backup, Android may include your tasks in your device backup or in a
transfer to a new device (for example, Google's backup on most devices), under the terms of that
backup service. The app itself never sends them anywhere. You can turn backup off in your device's
settings.

## Sharing

When you share text from another app to Reminders, the app saves it as a new task on your device.
Nothing is shared unless you choose to share it.

## Notifications and alarms

Reminders shows a notification when a task's reminder comes due, with its title and actions to mark
it done or snooze it. Notifications are shown by Android on your device; nothing is sent elsewhere.

## The home screen shortcut

The app adds a "New reminder" shortcut to your launcher and tells Android when you use it, so the
launcher can rank its shortcuts. This stays on your device.

## Permissions

- `POST_NOTIFICATIONS`: to show reminders. Android asks you first; without it, reminders can't
  appear.
- `SCHEDULE_EXACT_ALARM`: to ring at the exact minute you set. You can allow or deny it in Android's
  settings; without it, reminders may arrive a few minutes late.
- `RECEIVE_BOOT_COMPLETED`: to set your reminders again after your phone restarts.
- `org.prauga.reminders.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION` is added by the AndroidX library to
  keep the app's internal broadcasts private; it gives the app no access to your data.

The app does not have the `INTERNET` permission.

## Children

The app is not directed at children and collects no data from anyone, children included.

## Changes

If the app starts handling data differently, this policy will be updated before that version is
released, with a new effective date at the top.

## Contact

Questions about this policy: danascape@gmail.com, or open an issue at
https://github.com/PVOT-OSS/Notes/issues.
