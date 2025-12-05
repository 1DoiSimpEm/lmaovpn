package com.amobear.freevpn.di

import android.os.Build
import com.amobear.freevpn.BuildConfig
import com.amobear.freevpn.data.network.SignalApiClient
import com.amobear.freevpn.data.network.VpnApiClient
import com.amobear.freevpn.data.network.api.SignalApiService
import com.amobear.freevpn.data.network.api.VpnApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.Locale
import java.util.concurrent.TimeUnit
import javax.inject.Singleton
import javax.inject.Named

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    // Helper function to get version name (matching Android code exactly)
    // Note: For ProtonVPN compatibility, we use BuildConfig.VERSION_NAME directly
    // which is "5.14.76.0" from build.gradle.kts
    private fun versionName(): String {
        val devSuffix = "-dev"
        // If DEBUG and version doesn't end with -dev, add it
        return if (!BuildConfig.VERSION_NAME.endsWith(devSuffix) && BuildConfig.DEBUG) {
            BuildConfig.VERSION_NAME + devSuffix
        } else {
            BuildConfig.VERSION_NAME
        }
    }
    
    // Helper function to replace non-ASCII characters (matching Android code exactly)
    private fun String.replaceNonAscii(): String {
        return if (all { it.code < 128 }) {
            this
        } else {
            buildString {
                for (c in this@replaceNonAscii) {
                    append(if (c.code < 128) c else '?')
                }
            }
        }
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC // Changed to BASIC to reduce log noise
        }

        // Add required headers interceptor (x-pm-appversion, User-Agent)
        // Matching ProtonVPN Android app implementation exactly
        val headersInterceptor = okhttp3.Interceptor { chain ->
            val originalRequest = chain.request()
            
            // Build app version header (format: clientId@versionName[STORE_SUFFIX])
            // Matching ProtonVPN format exactly: "android-vpn@5.14.76.0+play"
            val clientId = "android-vpn" // Constants.MOBILE_CLIENT_ID
            val versionNameValue = BuildConfig.VERSION_NAME // "5.14.76.0" from build.gradle.kts
            val storeSuffix = BuildConfig.STORE_SUFFIX // "+play" from build.gradle.kts
            val appVersionHeader = "$clientId@$versionNameValue$storeSuffix"
            
            // Build User-Agent header (format: ProtonVPN/version (Android version; brand model))
            // Must replace non-ASCII characters like Android code does
            val userAgent = String.format(
                Locale.US,
                "ProtonVPN/%s (Android %s; %s %s)",
                BuildConfig.VERSION_NAME, // Use BuildConfig.VERSION_NAME
                Build.VERSION.RELEASE,
                Build.BRAND,
                Build.MODEL
            ).replaceNonAscii()
            
            val requestWithHeaders = originalRequest.newBuilder()
                .header("x-pm-appversion", appVersionHeader) // Required by API
                .header("User-Agent", userAgent)
                .build()
            
            chain.proceed(requestWithHeaders)
        }

        return OkHttpClient.Builder()
            .addInterceptor(headersInterceptor)
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    @Named("vpnApi")
    fun provideVpnApiRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(VpnApiClient.VPN_GATE_API_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideVpnApiService(@Named("vpnApi") retrofit: Retrofit): VpnApiService {
        return retrofit.create(VpnApiService::class.java)
    }

    @Named("signalApi")
    @Provides
    @Singleton
    fun provideSignalApiRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(SignalApiClient.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideSignalApiService(@Named("signalApi") retrofit: Retrofit): SignalApiService =
        retrofit.create(SignalApiService::class.java)
}

