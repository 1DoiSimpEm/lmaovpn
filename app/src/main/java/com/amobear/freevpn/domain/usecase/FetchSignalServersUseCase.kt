package com.amobear.freevpn.domain.usecase

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

class FetchSignalServersUseCase @Inject constructor(
    private val signalApiClient: SignalApiClient,
    private val signalRequestProvider: SignalRequestProvider
) {
    suspend operator fun invoke(): Result<SignalServerResponse> {
        val (headers, query) = signalRequestProvider.build()
        return signalApiClient.fetchServers(
            ispName = query.ispName,
            ispCountry = query.ispCountry,
            ispRegion = query.ispRegion,
            ispCity = query.ispCity,
            ispIp = query.ispIp,
            devImsi = query.devImsi,
            devLang = query.devLang,
            appPackage = headers.appPackage,
            appVerCode = headers.appVerCode,
            authId = headers.authId,
            authToken = headers.authToken,
            reqToken = headers.reqToken,
            userAgent = headers.userAgent
        )
    }
}

