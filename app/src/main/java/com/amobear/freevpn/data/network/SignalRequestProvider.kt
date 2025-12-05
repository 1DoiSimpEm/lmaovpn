package com.amobear.freevpn.data.network

import android.content.Context
import android.os.Build
import android.provider.Settings
import android.telephony.TelephonyManager
import android.text.TextUtils
import com.amobear.freevpn.BuildConfig
import com.amobear.freevpn.domain.usecase.SignalHeaderParams
import com.amobear.freevpn.domain.usecase.SignalQueryParams
import dagger.hilt.android.qualifiers.ApplicationContext
import java.math.BigInteger
import java.nio.ByteBuffer
import java.security.MessageDigest
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SignalRequestProvider @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val prefs by lazy {
        context.applicationContext.getSharedPreferences("signal_request_prefs", Context.MODE_PRIVATE)
    }

    fun build(): Pair<SignalHeaderParams, SignalQueryParams> {
        val authId = getOrCreatePositiveLong("auth_id").toString()
        val authToken = getOrCreatePositiveLong("auth_token").toString()

        val appPackage = context.packageName
        val appVerCode = BuildConfig.VERSION_CODE.toString()
        val reqToken = md5("auth_id=$authId&auth_token=$authToken&app_package=$appPackage&app_ver_code=$appVerCode&app_signature=2d19513c8872e4625f1353167a0cde7c")
        val userAgent = buildUserAgent()

        val telephony = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
        val simOperator = telephony?.simOperator

        val locale = Locale.getDefault()
        val devLang = "${locale.language}_${locale.country}"

        val headers = SignalHeaderParams(
            appPackage = appPackage,
            appVerCode = appVerCode,
            authId = authId,
            authToken = authToken,
            reqToken = reqToken,
            userAgent = userAgent
        )

        val query = SignalQueryParams(
            ispName = null, // not available locally without remote IP lookup
            ispCountry = locale.country.ifBlank { null },
            ispRegion = null,
            ispCity = null,
            ispIp = null,
            devImsi = simOperator,
            devLang = devLang
        )

        return headers to query
    }

    private fun getOrCreatePositiveLong(key: String): Long {
        val existing = prefs.getLong(key, -1L)
        if (existing > 0) return existing

        var value = System.currentTimeMillis()
        do {
            try {
                val uuid = UUID.randomUUID()
                val buffer = ByteBuffer.wrap(ByteArray(16))
                buffer.putLong(uuid.leastSignificantBits)
                buffer.putLong(uuid.mostSignificantBits)
                value = BigInteger(buffer.array()).toLong()
            } catch (_: Exception) {
            }
        } while (value <= 0)

        prefs.edit().putLong(key, value).apply()
        return value
    }

    private fun md5(input: String): String {
        return try {
            val md = MessageDigest.getInstance("MD5")
            val digest = md.digest(input.toByteArray())
            val bigInt = BigInteger(1, digest)
            var hash = bigInt.toString(16)
            while (hash.length < 32) {
                hash = "0$hash"
            }
            hash
        } catch (_: Exception) {
            ""
        }
    }

    private fun buildUserAgent(): String {
        val systemUA = System.getProperty("http.agent")
        if (!systemUA.isNullOrBlank()) return systemUA
        return "Dalvik/2.1.0 (Linux; U; Android ${Build.VERSION.RELEASE}; ${Build.MODEL})"
    }
}

