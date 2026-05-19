# Firebase Firestore POJOs
-keepclassmembers class com.pgmanager.app.data.model.** {
    *;
}

# MPAndroidChart
-keep class com.github.mikephil.charting.** { *; }
-dontwarn com.github.mikephil.charting.**

# Firebase
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**
