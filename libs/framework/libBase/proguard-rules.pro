###-----------基本指令--------------------
###-----------指定代码的压缩级别------------
-optimizationpasses 9
###-----------是否使用大小写混合------------
#-dontusemixedcaseclassnames
###-----------混淆时是否做预校验------------
-dontpreverify
###-----------混淆时是否记录日志------------
-verbose
###-----------忽略警告------------
-ignorewarnings
#-keepattributes EnclosingMethod
-keep class com.hive.nativec.**{*;}

-keepclasseswithmembernames class * {
    native <methods>;
}

-keep @com.hive.annotation.NotProguard class * {*;}
-keep class * {
    @com.hive.annotation.NotProguard <fields>;
}
-keepclassmembers class * {
    @com.hive.annotation.NotProguard <methods>;
}
##js
-keepattributes *Annotation*
-keepattributes *JavascriptInterface*

-keep class com.tencent.mm.opensdk.** {
    *;
}

-keep class com.tencent.wxop.** {
    *;
}

-keep class com.tencent.mm.sdk.** {
    *;
}

#dbflow
-keep class com.raizlabs.android.**{*;}

-keepnames class * extends com.hive.views.fragment.PagerTitleView

-keepclassmembers class * extends com.hive.views.fragment.PagerTitleView {*;}
