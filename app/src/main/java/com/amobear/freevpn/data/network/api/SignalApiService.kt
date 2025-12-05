package com.amobear.freevpn.data.network.api

import com.amobear.freevpn.data.network.api.dto.SignalRegisterRequest
import com.amobear.freevpn.data.network.api.dto.SignalRegisterResponse
import com.amobear.freevpn.data.network.api.dto.SignalResponseDto
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Header
import retrofit2.http.Query

interface SignalApiService {

    @POST("v2/device/")
    suspend fun registerDevice(
        @Body body: SignalRegisterRequest,
        @Header("s-app-package") appPackage: String,
        @Header("s-app-ver-code") appVerCode: String,
        @Header("s-auth-id") authId: String = "0",
        @Header("s-auth-token") authToken: String = "0",
        @Header("s-req-token") reqToken: String,
        @Header("User-Agent") userAgent: String,
        @Header("Accept-Encoding") acceptEncoding: String = "gzip",
        @Header("Connection") connection: String = "Keep-Alive",
        @Header("http.keepAlive") keepAlive: String = "false",
    ): Response<SignalRegisterResponse>

    @GET("v2/server/")
    suspend fun getServers(
        @Query("isp_name") ispName: String?,
        @Query("isp_country") ispCountry: String?,
        @Query("isp_region") ispRegion: String?,
        @Query("isp_city") ispCity: String?,
        @Query("isp_ip") ispIp: String?,
        @Query("dev_imsi") devImsi: String?,
        @Query("dev_lang") devLang: String?,

        // Dynamic headers (all supplied by caller)
        @Header("s-app-package") appPackage: String,
        @Header("s-app-ver-code") appVerCode: String,
        @Header("s-auth-id") authId: String,
        @Header("s-auth-token") authToken: String,
        @Header("s-req-token") reqToken: String,
        @Header("User-Agent") userAgent: String,
        @Header("Accept-Encoding") acceptEncoding: String = "gzip",
        @Header("Connection") connection: String = "Keep-Alive",
        @Header("http.keepAlive") keepAlive: String = "false",
    ): Response<SignalResponseDto>
}

