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
####-----------保证异常时显示行号------------
#-renamesourcefileattribute SourceFile
#-keepattributes SourceFile,LineNumberTable
#
# #dump文件列出apk包内所有class的内部结构
#-dump class_files.txt
#
##seeds.txt文件列出未混淆的类和成员
#-printseeds seeds.txt
#
##usage.txt文件列出从apk中删除的代码
#-printusage unused.txt
#
##mapping文件列出混淆前后的映射
#-printmapping mapping.txt
#
####-----------注解------------
#-keepattributes *Annotation*
#
####-----------泛型------------
#-keepattributes Signature
#
####-----------异常------------
#-keepattributes Exceptions
#
####-----------混淆时所采用的算法------------
#-optimizations !code/simplification/arithmetic,!field/*,!class/merging/*
####-----------避免混淆Android基本组件的配置--------------------
####-----------保持Activity类不被混淆------------
##-keep public class * extends android.app.Activity
####-----------保持AppCompatActivity类不被混淆------------
##-keep public class * extends androidx.appcompat.app.AppCompatActivity
####-----------保持DialogFragment类不被混淆------------
##-keep public class * extends android.app.DialogFragment
####-----------保持Application类不被混淆------------
#-keep public class * extends android.app.Application
####-----------保持Service类不被混淆------------
#-keep public class * extends android.app.Service
####-----------保持BroadcastReceiver类不被混淆------------
#-keep public class * extends android.content.BroadcastReceiver
####-----------保持ContentProvider类不被混淆------------
#-keep public class * extends android.content.ContentProvider
####-----------保持BackupAgentHelper类不被混淆------------
#-keep public class * extends android.app.backup.BackupAgentHelper
####-----------保持Preference类不被混淆------------
#-keep public class * extends android.preference.Preference
####-----------保持ILicensingService类不被混淆------------
#-keep public class com.android.vending.licensing.ILicensingService
#
###-----------保持 native 方法不被混淆------------
-keepclasseswithmembernames class * {
    native <methods>;
}
#
#####-----------保持自定义控件类不被混淆------------
##-keepclasseswithmembers class * {
##    public <init>(android.content.Context, android.util.AttributeSet);
##}
##
#####-----------保持自定义控件类不被混淆------------
##-keepclasseswithmembers class * {
##    public <init>(android.content.Context, android.util.AttributeSet, int);
##}
#
####-----------保持自定义控件类不被混淆------------
##-keepclassmembers class * extends android.app.Activity {
##    public void *(android.view.View);
##}
#
####-----------保持枚举 enum 类不被混淆------------
#-keepclassmembers enum * {
#    public static **[] values();
#    public static ** valueOf(java.lang.String);
#}
#
####-----------# 保持 Parcelable 不被混淆------------
#-keep class * implements android.os.Parcelable {
#    public static final android.os.Parcelable$Creator *;
#}
#
###-----------# 保持注解NotProguard 不被混淆------------
-keep @com.hive.annotation.NotProguard class * {*;}
-keep class * {
    @com.hive.annotation.NotProguard <fields>;
}
-keepclassmembers class * {
    @com.hive.annotation.NotProguard <methods>;
}
#
####-----------# 保持 Serializable 不被混淆------------
#-keepnames class * implements java.io.Serializable
#-keepclassmembers class * implements java.io.Serializable {*;}
#
####----------混淆第三方库-----------------
#
####-----------保持 retrofit client 不被混淆------------
#-keep class com.excellence.retrofit.RetrofitHttpService { *; }
#
####-----------保持 retrofit 不被混淆------------
#-dontwarn retrofit2.**
#-keep class retrofit2.** { *; }
#-dontwarn javax.annotation.**
#
####-----------保持 okhttp 不被混淆------------
#-dontwarn com.squareup.okhttp3.**
#-keep class com.squareup.okhttp3.** { *;}
#-dontwarn okio.**
#
####-----------保持 GreenDao 不被混淆------------
#-keepclassmembers class * extends org.greenrobot.greendao.AbstractDao {
#public static java.lang.String TABLENAME;
#}
#-keep class **$Properties
#
####-----------保持 eventbus 不被混淆------------
#-keepattributes *Annotation*
#-keepclassmembers class ** {
#    @org.greenrobot.eventbus.Subscribe <methods>;
#}
#-keep enum org.greenrobot.eventbus.ThreadMode { *; }
## Only required if you use AsyncExecutor
#-keepclassmembers class * extends org.greenrobot.eventbus.util.ThrowableFailureEvent {
#    <init>(java.lang.Throwable);
#}
#
####-----------保持 gson 不被混淆------------
#-keep class sun.misc.Unsafe { *; }
#-keep class com.google.gson.stream.** { *; }
#
####-----------保持 Rxjava RxAndroid 不被混淆------------
#-dontwarn sun.misc.**
#-keepclassmembers class rx.internal.util.unsafe.*ArrayQueue*Field* {
#   long producerIndex;
#   long consumerIndex;
#}
#-keepclassmembers class rx.internal.util.unsafe.BaseLinkedQueueProducerNodeRef {
#    rx.internal.util.atomic.LinkedQueueNode producerNode;
#}
#-keepclassmembers class rx.internal.util.unsafe.BaseLinkedQueueConsumerNodeRef {
#    rx.internal.util.atomic.LinkedQueueNode consumerNode;
#}
#
####-----------保持 volley 不被混淆------------
#-keep class com.android.volley.** { *; }
#-keep class com.android.volley.toolbox.** { *; }
#
##webview的js接口
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}
#
##baserecycleviewadapterhelper
#-keep class com.chad.library.adapter.** {
#   *;
#}
#
##oss 阿里云存储
#-keep class com.alibaba.sdk.android.oss.** { *; }
#-dontwarn okio.**
#-dontwarn org.apache.commons.codec.binary.**
#
#-keep public class * implements com.bumptech.glide.module.GlideModule
#
##高德地图
#-keep class com.amap.api.location.**{*;}
#-keep class com.amap.api.fence.**{*;}
#-keep class com.autonavi.aps.amapapi.model.**{*;}
#
##retrofit2
#-dontwarn retrofit2.**
#-keep class retrofit2.** { *; }
#-keepattributes Signature
#-keepattributes Exceptions
#
##okhttputils
#-dontwarn com.zhy.http.**
#-keep class com.zhy.http.**{*;}
##okhttp
#-dontwarn okhttp3.**
#-keep class okhttp3.**{*;}
##okio
#-dontwarn okio.**
#-keep class okio.**{*;}
#
##eventbus3
#-keepattributes *Annotation*
#-keepclassmembers class ** {
#    @org.greenrobot.eventbus.Subscribe <methods>;
#}
#-keep enum org.greenrobot.eventbus.ThreadMode { *; }
#
##gilde
#-keep public class * implements com.bumptech.glide.module.GlideModule
#-keep class com.bumptech.glide.integration.okhttp3.OkHttpGlideModule
#-keep public enum com.bumptech.glide.load.resource.bitmap.ImageHeaderParser$** {
#  **[] $VALUES;
#  public *;
#}
##-keepresourcexmlelements manifest/application/meta-data@value=GlideModule
#
##stetho
#-keep class com.facebook.stetho.** { *; }
#-dontwarn org.mozilla.javascript.**
#-dontwarn org.mozilla.classfile.**
#-keep class org.mozilla.javascript.** { *; }
#
##gson
#-keep public class com.google.gson.**
#-keep public class com.google.gson.** {public private protected *;}
#-keep class sun.misc.Unsafe { *; }
#-keepattributes Signature
#-keepattributes *Annotation*
#
##ormlite
#-keepattributes *DatabaseField*
#-keepattributes *DatabaseTable*
#-keepattributes *SerializedName*
#-keep class com.j256.**
#-keepclassmembers class com.j256.** { *; }
#-keep enum com.j256.**
#-keepclassmembers enum com.j256.** { *; }
#-keep interface com.j256.**
#-keepclassmembers interface com.j256.** { *; }
#
#
##galleryfinal
#-keep class cn.finalteam.galleryfinal.widget.*{*;}
#-keep class cn.finalteam.galleryfinal.widget.crop.*{*;}
#-keep class cn.finalteam.galleryfinal.widget.zoonview.*{*;}
#
##==================protobuf======================
#-dontwarn com.google.**
#-keep class com.google.protobuf.** {*;}
#
#-keepclassmembers class * {
#   public <init> (org.json.JSONObject);
#}
#
#-keepclassmembers enum * {
#    public static **[] values();
#    public static ** valueOf(java.lang.String);
#}
#
#-dontwarn rx.internal.util.unsafe.*
#
#-dontwarn com.squareup.okhttp.**
#
#-dontwarn com.squareup.leakcanary.**
#
#-dontwarn com.yalantis.ucrop**
#-keep class com.yalantis.ucrop** { *; }
#-keep interface com.yalantis.ucrop** { *; }
#
##rx
#-dontwarn rx.**
#-keepclassmembers class rx.** { *; }
## retrolambda
#-dontwarn java.lang.invoke.*
#
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
#
##qskin
#
##ijk
#-keep class tv.danmaku.ijk.**{*;}
#
##点量
#-keep class cn.dolit.**{*;}
#
##迅雷
#-keep class com.xunlei.**{*;}
#
##x5内核
#-keep class com.tencent.**{*;}
#
##加载图
#-keep class com.wang.avi.**{*;}
#
#-keep class com.hive.nativec.**{*;}
#
-keepnames class * extends com.hive.views.fragment.PagerTitleView

-keepclassmembers class * extends com.hive.views.fragment.PagerTitleView {*;}
#
#-keep class jackmego.com.jieba_android.RequestCallback { *; }
#-keep class jackmego.com.jieba_android.JiebaSegmenter { *; }
#-keep class jackmego.com.jieba_android.JiebaSegmenter$** {   # keep enum
#    **[] $VALUES;
#    public *;
#}
#-keep class jackmego.com.jieba_android.SegToken { *; }
#-keep class com.microsoft.** { *; }
#-keep class com.iflytek.** { *; }
#
