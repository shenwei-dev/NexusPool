# ==================== 基础 ====================
-keepattributes Signature
-keepattributes Exceptions
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ==================== 项目数据模型（Gson 反射序列化） ====================
-keep class com.family.chat.dto.** { *; }
-keep class com.family.chat.vo.** { *; }
-keep class com.family.chat.common.ApiResponse { *; }
-keep class com.family.chat.model.** { *; }
-keep class com.family.chat.db.LocalMessage { *; }

# ==================== Retrofit / OkHttp ====================
# OkHttp/Okio 对 R8 友好, 仅保留 dontwarn, 允许裁剪与混淆以减小体积
-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement
-dontwarn javax.annotation.**
-dontwarn kotlin.Unit
-dontwarn retrofit2.KotlinExtensions
-dontwarn retrofit2.KotlinExtensions$*
-dontwarn okhttp3.**
-dontwarn okio.**

# ==================== Gson ====================
# Gson 本体允许裁剪, 被反射序列化的模型类已在上方 keep
-dontwarn com.google.gson.**

# ==================== Protobuf (lite 运行时) ====================
-keep class * extends com.google.protobuf.GeneratedMessageLite { *; }
-keep class com.family.chat.protobuf.** { *; }

# ==================== Glide ====================
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep class * extends com.bumptech.glide.module.AppGlideModule { <init>(...); }
-keep public enum com.bumptech.glide.load.ImageHeaderParser$** { **[] $VALUES; public *; }
-keep class com.bumptech.glide.load.data.ParcelFileDescriptorRewinder$InternalRewinder { *** rewind(); }

# ==================== Room ====================
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# ==================== 声网 Agora ====================
-keep class io.agora.** { *; }
-dontwarn io.agora.**

# ==================== uCrop ====================
-keep class com.yalantis.ucrop.** { *; }
-keep interface com.yalantis.ucrop.** { *; }

# ==================== PhotoView ====================
-keep class com.github.chrisbanes.photoview.** { *; }
