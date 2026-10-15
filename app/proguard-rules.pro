# ProGuard rules for Smart Shopping List Android 2026

# Room Database
-keepclassmembers class * extends androidx.room.RoomDatabase {
    public void clearAllTables();
    <methods>;
}
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# Keep entities and models
-keep class ro.smartshopping.list.data.local.entity.** { *; }
-keep class ro.smartshopping.list.domain.model.** { *; }

# Google Credential Manager
-keep class androidx.credentials.** { *; }
-keep class com.google.android.libraries.identity.googleid.** { *; }
