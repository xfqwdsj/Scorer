# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# Keep `Companion` object fields of serializable classes.
# This avoids serializer lookup through `getDeclaredClasses` as done for named companion objects.
-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1> {
    static <1>$Companion Companion;
}

# Keep `serializer()` on companion objects (both default and named) of serializable classes.
-if @kotlinx.serialization.Serializable class ** {
    static **$* *;
}
-keepclassmembers class <2>$<3> {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep `INSTANCE.serializer()` of serializable objects.
-if @kotlinx.serialization.Serializable class ** {
    public static ** INSTANCE;
}
-keepclassmembers class <1> {
    public static <1> INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
}

# @Serializable and @Polymorphic are used at runtime for polymorphic serialization.
-keepattributes RuntimeVisibleAnnotations,AnnotationDefault

# Serializer for classes with named companion objects are retrieved using `getDeclaredClasses`.
# If you have any, uncomment and replace classes with those containing named companion objects.
#-keepattributes InnerClasses # Needed for `getDeclaredClasses`.
#-if @kotlinx.serialization.Serializable class
#com.example.myapplication.HasNamedCompanion, # <-- List serializable classes with named companions.
#com.example.myapplication.HasNamedCompanion2
#{
#    static **$* *;
#}
#-keepnames class <1>$$serializer { # -keepnames suffices; class is kept when serializer() is kept.
#    static <1>$$serializer INSTANCE;
#}

## Netty
#-keepattributes Signature,InnerClasses
#-keepclasseswithmembers class io.netty.** {
#    *;
#}
#-keepnames class io.netty.** {
#    *;
#}

# Ktor
-keep class io.ktor.server.netty.EngineMain { *; }
-keep class io.ktor.server.config.HoconConfigLoader { *; }
-keep class kotlin.reflect.jvm.internal.** { *; }
-keep class kotlin.text.RegexOption { *; }

#-dontwarn io.netty.internal.tcnative.AsyncSSLPrivateKeyMethod
#-dontwarn io.netty.internal.tcnative.AsyncTask
#-dontwarn io.netty.internal.tcnative.Buffer
#-dontwarn io.netty.internal.tcnative.CertificateCallback
#-dontwarn io.netty.internal.tcnative.CertificateCompressionAlgo
#-dontwarn io.netty.internal.tcnative.CertificateVerifier
#-dontwarn io.netty.internal.tcnative.Library
#-dontwarn io.netty.internal.tcnative.SSL
#-dontwarn io.netty.internal.tcnative.SSLContext
#-dontwarn io.netty.internal.tcnative.SSLPrivateKeyMethod
#-dontwarn io.netty.internal.tcnative.SSLSessionCache
#-dontwarn io.netty.internal.tcnative.SessionTicketKey
#-dontwarn io.netty.internal.tcnative.SniHostNameMatcher
#-dontwarn java.lang.management.ManagementFactory
#-dontwarn java.lang.management.RuntimeMXBean
#-dontwarn org.apache.log4j.Level
#-dontwarn org.apache.log4j.Logger
#-dontwarn org.apache.log4j.Priority
#-dontwarn org.apache.logging.log4j.Level
#-dontwarn org.apache.logging.log4j.LogManager
#-dontwarn org.apache.logging.log4j.Logger
#-dontwarn org.apache.logging.log4j.message.MessageFactory
#-dontwarn org.apache.logging.log4j.spi.ExtendedLogger
#-dontwarn org.apache.logging.log4j.spi.ExtendedLoggerWrapper
#-dontwarn org.bouncycastle.jsse.BCSSLParameters
#-dontwarn org.bouncycastle.jsse.BCSSLSocket
#-dontwarn org.bouncycastle.jsse.provider.BouncyCastleJsseProvider
#-dontwarn org.conscrypt.BufferAllocator
#-dontwarn org.conscrypt.Conscrypt$Version
#-dontwarn org.conscrypt.Conscrypt
#-dontwarn org.conscrypt.ConscryptHostnameVerifier
#-dontwarn org.conscrypt.HandshakeListener
#-dontwarn org.eclipse.jetty.npn.NextProtoNego$ClientProvider
#-dontwarn org.eclipse.jetty.npn.NextProtoNego$Provider
#-dontwarn org.eclipse.jetty.npn.NextProtoNego$ServerProvider
#-dontwarn org.eclipse.jetty.npn.NextProtoNego
#-dontwarn org.jetbrains.annotations.Async$Execute
#-dontwarn org.jetbrains.annotations.Async$Schedule
#-dontwarn org.openjsse.javax.net.ssl.SSLParameters
#-dontwarn org.openjsse.javax.net.ssl.SSLSocket
#-dontwarn org.openjsse.net.ssl.OpenJSSE
#-dontwarn org.slf4j.impl.StaticLoggerBinder
#-dontwarn reactor.blockhound.integration.BlockHoundIntegration
-dontwarn io.netty.internal.tcnative.AsyncSSLPrivateKeyMethod
-dontwarn io.netty.internal.tcnative.AsyncTask
-dontwarn io.netty.internal.tcnative.Buffer
-dontwarn io.netty.internal.tcnative.CertificateCallback
-dontwarn io.netty.internal.tcnative.CertificateCompressionAlgo
-dontwarn io.netty.internal.tcnative.CertificateVerifier
-dontwarn io.netty.internal.tcnative.Library
-dontwarn io.netty.internal.tcnative.SSL
-dontwarn io.netty.internal.tcnative.SSLContext
-dontwarn io.netty.internal.tcnative.SSLPrivateKeyMethod
-dontwarn io.netty.internal.tcnative.SSLSessionCache
-dontwarn io.netty.internal.tcnative.SessionTicketKey
-dontwarn io.netty.internal.tcnative.SniHostNameMatcher
-dontwarn jakarta.servlet.ServletContainerInitializer
-dontwarn java.lang.Module
-dontwarn java.lang.management.ManagementFactory
-dontwarn java.lang.management.RuntimeMXBean
-dontwarn java.lang.module.ModuleDescriptor
-dontwarn javax.naming.Context
-dontwarn javax.naming.InitialContext
-dontwarn javax.naming.NamingException
-dontwarn org.apache.log4j.Level
-dontwarn org.apache.log4j.Logger
-dontwarn org.apache.log4j.Priority
-dontwarn org.apache.logging.log4j.Level
-dontwarn org.apache.logging.log4j.LogManager
-dontwarn org.apache.logging.log4j.Logger
-dontwarn org.apache.logging.log4j.message.MessageFactory
-dontwarn org.apache.logging.log4j.spi.ExtendedLogger
-dontwarn org.apache.logging.log4j.spi.ExtendedLoggerWrapper
-dontwarn org.bouncycastle.jsse.BCSSLParameters
-dontwarn org.bouncycastle.jsse.BCSSLSocket
-dontwarn org.bouncycastle.jsse.provider.BouncyCastleJsseProvider
-dontwarn org.codehaus.janino.ClassBodyEvaluator
-dontwarn org.conscrypt.BufferAllocator
-dontwarn org.conscrypt.Conscrypt$Version
-dontwarn org.conscrypt.Conscrypt
-dontwarn org.conscrypt.ConscryptHostnameVerifier
-dontwarn org.conscrypt.HandshakeListener
-dontwarn org.eclipse.jetty.npn.NextProtoNego$ClientProvider
-dontwarn org.eclipse.jetty.npn.NextProtoNego$Provider
-dontwarn org.eclipse.jetty.npn.NextProtoNego$ServerProvider
-dontwarn org.eclipse.jetty.npn.NextProtoNego
-dontwarn org.jetbrains.annotations.Async$Execute
-dontwarn org.jetbrains.annotations.Async$Schedule
-dontwarn org.openjsse.javax.net.ssl.SSLParameters
-dontwarn org.openjsse.javax.net.ssl.SSLSocket
-dontwarn org.openjsse.net.ssl.OpenJSSE
-dontwarn reactor.blockhound.integration.BlockHoundIntegration