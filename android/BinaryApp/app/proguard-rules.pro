# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# Keep Room entity classes
-keep class com.binaryapp.data.local.entities.** { *; }

# Keep DAO interfaces
-keep interface com.binaryapp.data.local.dao.** { *; }

# Keep ViewModels
-keep class com.binaryapp.viewmodel.** { *; }
