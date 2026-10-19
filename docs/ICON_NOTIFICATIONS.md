# App icon, notification icon and dynamic-island behaviour

## Single source of truth
The launcher icon the user picks is stored as which `activity-alias` is enabled (component state survives
restarts, reboots and app updates). `NotificationIcon.current(context)` maps that choice to a monochrome
status-bar glyph (`ic_stat_*`). A photo chosen from the gallery uses the official glyph (`ic_stat_official`).
A fresh install enables `MainActivityAlias` (official icon) only. `IconRepairReceiver` re-enables the default
after an update if no alias is left on.

## Root causes found
- Several notifications (downloads, song recognition, file downloads) used system or upstream glyphs instead of `NotificationIcon.current`.
- Media3 falls back to its own default small icon unless `media3_notification_small_icon` is supplied; we override that resource and also set the icon on every notification build.
- Small icons must be white-on-transparent masks; coloured artwork is rendered as a solid blob by Android.
- Album art stays the large icon / media artwork; it is never replaced by an app icon.

## What cannot be changed
`android:icon` on `<application>` is fixed at install time. Some OEM surfaces (Samsung, Xiaomi, OnePlus
"dynamic island"/live-activity chips, always-on display) read that application icon or the launcher's cached
icon, so they can lag behind the alias change until the launcher refreshes. We do not use hidden APIs or
hacks to override system UI. If an OEM shows a stale icon, clearing the launcher cache or a reboot refreshes it.
