package com.amobear.freevpn.data.network

import android.util.Log
import com.amobear.freevpn.data.network.api.SignalApiService
import com.amobear.freevpn.data.network.api.dto.SignalRegisterRequest
import com.amobear.freevpn.data.network.api.dto.SignalResponseDto
import com.amobear.freevpn.domain.model.SignalServer
import com.amobear.freevpn.domain.model.SignalServerConfig
import com.amobear.freevpn.domain.model.SignalServerResponse
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SignalApiClient @Inject constructor(
    private val signalApiService: SignalApiService
) {

    companion object {
        private const val TAG = "SignalApiClient"
        const val BASE_URL = "https://s1.free-signal.com/"
    }

    suspend fun fetchServers(
        ispName: String? = null,
        ispCountry: String? = null,
        ispRegion: String? = null,
        ispCity: String? = null,
        ispIp: String? = null,
        devImsi: String? = null,
        devLang: String? = null,
        appPackage: String,
        appVerCode: String,
        authId: String,
        authToken: String,
        reqToken: String,
        userAgent: String
    ): Result<SignalServerResponse> {
        return try {
            val response = signalApiService.getServers(
                ispName = ispName,
                ispCountry = ispCountry,
                ispRegion = ispRegion,
                ispCity = ispCity,
                ispIp = ispIp,
                devImsi = devImsi,
                devLang = devLang,
                appPackage = appPackage,
                appVerCode = appVerCode,
                authId = authId,
                authToken = authToken,
                reqToken = reqToken,
                userAgent = userAgent
            )

            if (response.isSuccessful) {
                val body = response.body()
                val mapped = mapToDomain(body)
                Result.success(mapped)
            } else {
                Log.w(TAG, "Request failed: ${response.code()} - ${response.message()}")
                Result.failure(Exception("Signal API error ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching Signal servers", e)
            Result.failure(e)
        }
    }

    suspend fun registerDevice(
        body: SignalRegisterRequest,
        appPackage: String,
        appVerCode: String,
        reqToken: String,
        userAgent: String
    ): Result<Pair<Long, Long>> {
        return try {
            val response = signalApiService.registerDevice(
                body = body,
                appPackage = appPackage,
                appVerCode = appVerCode,
                reqToken = reqToken,
                userAgent = userAgent
            )
            if (response.isSuccessful) {
                val reg = response.body()
                if (reg != null && reg.authId > 0 && reg.authToken > 0) {
                    Result.success(reg.authId to reg.authToken)
                } else {
                    Result.failure(IllegalStateException("Invalid register response"))
                }
            } else {
                Result.failure(IllegalStateException("Register failed ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun mapToDomain(body: SignalResponseDto?): SignalServerResponse {
        if (body == null) return SignalServerResponse()

        val config = SignalServerConfig(
            dnsServers = body.config?.dnsServer.orEmpty(),
            udpPorts = body.config?.udp.orEmpty(),
            tcpPorts = body.config?.tcp.orEmpty(),
            tunMtu = body.config?.tunMtu
        )

        val servers = body.servers.orEmpty().mapNotNull { item ->
            val ip = item.ip ?: return@mapNotNull null
            SignalServer(
                load = item.load ?: 0,
                isBt = item.isBt ?: false,
                obsKey = item.obsKey.orEmpty(),
                isVip = item.isVip ?: false,
                country = item.country.orEmpty(),
                obsAlgo = item.obsAlgo ?: 0,
                ip = ip,
                area = item.area.orEmpty(),
                isRunning = item.isRunning ?: false
            )
        }

        return SignalServerResponse(
            config = config,
            list = body.list,
            servers = servers
        )
    }
}
