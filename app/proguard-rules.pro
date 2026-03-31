-keep class com.school.manager.** { *; }
-keepclassmembers class * extends androidx.lifecycle.ViewModel { *; }

# Fix for Google Tink / EncryptedSharedPreferences missing annotations
-dontwarn com.google.errorprone.annotations.CanIgnoreReturnValue
-dontwarn com.google.errorprone.annotations.CheckReturnValue
-dontwarn com.google.errorprone.annotations.Immutable
-dontwarn com.google.errorprone.annotations.RestrictedApi

# Optional: also suppress related ones if they appear later
-dontwarn javax.annotation.**