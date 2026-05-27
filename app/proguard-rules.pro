# Proguard rules for Happ VPN.
# Keep native fields and Room models from obfuscation if required.
-keepclassmembers class * extends androidx.room.RoomDatabase {
    <init>(...);
}
