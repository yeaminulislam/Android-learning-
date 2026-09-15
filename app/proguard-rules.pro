# JS bridge methods must keep their names for addJavascriptInterface
-keepclassmembers class com.shieldbrowser.app.browser.MediaBridge {
    public *;
}
-keepclassmembers class com.shieldbrowser.app.ext.ExtBridge {
    public *;
}
-keepattributes JavascriptInterface
-dontwarn org.conscrypt.**
