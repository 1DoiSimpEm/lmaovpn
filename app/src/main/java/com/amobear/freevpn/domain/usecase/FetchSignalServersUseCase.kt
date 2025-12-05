package com.amobear.freevpn.domain.usecase

import android.util.Log
import com.amobear.freevpn.data.network.SignalApiClient
import com.amobear.freevpn.data.network.SignalRequestProvider
import com.amobear.freevpn.domain.model.SignalServerResponse
import javax.inject.Inject

data class SignalHeaderParams(
    val appPackage: String,
    val appVerCode: String,
    val authId: String,
    val authToken: String,
    val reqToken: String,
    val userAgent: String
)

data class SignalQueryParams(
    val ispName: String? = null,
    val ispCountry: String? = null,
    val ispRegion: String? = null,
    val ispCity: String? = null,
    val ispIp: String? = null,
    val devImsi: String? = null,
    val devLang: String? = null
)

/**
 * Result of fetching Signal servers, includes auth credentials for VPN connection
 */
data class SignalFetchResult(
    val serverResponse: SignalServerResponse,
    val authId: Long,
    val authToken: Long
)

class FetchSignalServersUseCase @Inject constructor(
    private val signalApiClient: SignalApiClient,
    private val signalRequestProvider: SignalRequestProvider
) {
    companion object {
        private const val TAG = "FetchSignalServersUseCase"
    }

    /**
     * Fetch servers and return with auth credentials
     */
    suspend fun fetchWithCredentials(): Result<SignalFetchResult> {
        return try {
            // Step 1: Build register request
            val registerRequest = signalRequestProvider.buildRegisterRequest()
            
            // Step 2: Get common parameters
            val appPackage = signalRequestProvider.getAppPackage()
            val appVerCode = signalRequestProvider.getAppVerCode()
            val initialReqToken = signalRequestProvider.generateInitialReqToken()
            val userAgent = signalRequestProvider.getUserAgent()
            val devLang = signalRequestProvider.getDevLang()
            val ispCountry = signalRequestProvider.getDevCountry()
            
            // Step 3: Register device first
            val registerResult = signalApiClient.registerDevice(
                body = registerRequest,
                appPackage = appPackage,
                appVerCode = appVerCode,
                reqToken = initialReqToken,
                userAgent = userAgent
            )

            val (authId, authToken) = registerResult.getOrElse {
                Log.e(TAG, "Failed to register device", it)
                return Result.failure(it)
            }

            // Step 4: Generate reqToken using credentials from register response
            val reqToken = signalRequestProvider.generateReqToken(authId, authToken)

            // Step 5: Fetch servers using credentials from register response
            val serverResult = signalApiClient.fetchServers(
                ispName = null,
                ispCountry = ispCountry,
                ispRegion = null,
                ispCity = null,
                ispIp = null,
                devImsi = null,
                devLang = devLang,
                appPackage = appPackage,
                appVerCode = appVerCode,
                authId = authId.toString(),
                authToken = authToken.toString(),
                reqToken = reqToken,
                userAgent = userAgent
            )
            
            serverResult.map { response ->
                SignalFetchResult(
                    serverResponse = response,
                    authId = authId,
                    authToken = authToken
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in FetchSignalServersUseCase", e)
            Result.failure(e)
        }
    }

    suspend operator fun invoke(): Result<SignalServerResponse> {
        return try {
            // Step 1: Build register request
            val registerRequest = signalRequestProvider.buildRegisterRequest()
            
            // Step 2: Get common parameters
            val appPackage = signalRequestProvider.getAppPackage()
            val appVerCode = signalRequestProvider.getAppVerCode()
            val initialReqToken = signalRequestProvider.generateInitialReqToken()
            val userAgent = signalRequestProvider.getUserAgent()
            val devLang = signalRequestProvider.getDevLang()
            val ispCountry = signalRequestProvider.getDevCountry()
            
            // Step 3: Register device first
            val registerResult = signalApiClient.registerDevice(
                body = registerRequest,
                appPackage = appPackage,
                appVerCode = appVerCode,
                reqToken = initialReqToken,
                userAgent = userAgent
            )

            val (authId, authToken) = registerResult.getOrElse {
                Log.e(TAG, "Failed to register device", it)
                return Result.failure(it)
            }

            // Step 4: Generate reqToken using credentials from register response
            val reqToken = signalRequestProvider.generateReqToken(authId, authToken)

            // Step 5: Fetch servers using credentials from register response
            signalApiClient.fetchServers(
                ispName = null,
                ispCountry = ispCountry,
                ispRegion = null,
                ispCity = null,
                ispIp = null,
                devImsi = null, // Can be added if needed
                devLang = devLang,
                appPackage = appPackage,
                appVerCode = appVerCode,
                authId = authId.toString(),
                authToken = authToken.toString(),
                reqToken = reqToken,
                userAgent = userAgent
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error in FetchSignalServersUseCase", e)
            Result.failure(e)
        }
    }
}

