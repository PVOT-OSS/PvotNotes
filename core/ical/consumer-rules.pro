# SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
# SPDX-License-Identifier: Apache-2.0

# ical4j's optional zone rules provider uses a JDK class Android doesn't have.
-dontwarn java.time.zone.ZoneRulesProvider

# ical4j finds its factories through ServiceLoader and its time zone cache by name
# (ical4j.properties).
-keep class * implements net.fortuna.ical4j.model.PropertyFactory { <init>(); }
-keep class * implements net.fortuna.ical4j.model.ParameterFactory { <init>(); }
-keep class * implements net.fortuna.ical4j.model.ComponentFactory { <init>(); }
-keep class net.fortuna.ical4j.util.MapTimeZoneCache { <init>(); }
