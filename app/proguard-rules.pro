# R8 rules for the release build.
#
# Most of what Flow uses already ships its own consumer rules, verified in the
# resolved artifacts rather than assumed:
#   - room-runtime keeps `* extends androidx.room.RoomDatabase { void <init>(); }`,
#     which covers the generated FlowDatabase_Impl that Room loads by name. Entities,
#     DAOs and DatabaseConverters are only ever touched by generated code that
#     references them directly, so they need nothing extra.
#   - kotlinx-serialization-core ships both its common rules and an R8 full-mode
#     file that preserves `INSTANCE` and `serializer()` on serializable objects.
#   - navigation-compose and the Compose libraries declare themselves shrink-safe.
#   - Manifest-declared components (FlowApplication, MainActivity, AlarmReceiver,
#     BootReceiver, TimezoneChangedReceiver) are kept automatically by the rules AGP
#     generates from AndroidManifest.xml, so they need no rules here.
#
# What follows covers the name-sensitive parts that are specific to this project.

# Category is persisted as its enum constant name: DatabaseConverters.fromCategory
# writes `category.name` into the reminders table and toCategory reads it back with
# `Category.valueOf`. Enum.valueOf resolves against the actual field names, so if R8
# renames the constants every previously saved row fails to load.
-keepclassmembers enum com.deepak.flow.core.model.Category {
    <fields>;
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Schedules, reminder times and active hours are stored as JSON columns and decoded
# through kotlinx.serialization in ReminderRepositoryImpl. The
# sealed Schedule hierarchy is the highest-risk thing in the app under R8, because a
# missing serializer or a stripped `data object` subclass fails at decode time rather
# than at build time, and a reminder would silently lose its repeat rule. This package
# is a handful of small data classes, so keeping it whole costs a negligible amount of
# size in exchange for removing that failure mode.
-keep class com.deepak.flow.core.model.** { *; }

# The generated serializers and companions backing the classes above. Redundant with
# the kotlinx-serialization consumer rules, kept explicitly because their failure mode
# is silent.
-keep,includedescriptorclasses class com.deepak.flow.core.model.**$$serializer { *; }
-keepclassmembers class com.deepak.flow.core.model.** {
    *** Companion;
}

# Type-safe navigation routes. Route patterns come from the serializer descriptor's
# serialName, which the compiler emits as a string constant, so obfuscation does not
# actually shift them; these are kept only so that stack traces from navigation
# failures stay readable.
-keepnames class com.deepak.flow.app.navigation.FlowRoute
-keepnames class com.deepak.flow.app.navigation.FlowRoute$*
