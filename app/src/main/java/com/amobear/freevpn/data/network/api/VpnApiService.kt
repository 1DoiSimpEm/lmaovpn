package com.amobear.freevpn.data.network.api

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET

/**
 * VPN API Service interface for VPN Gate API
 * VPN Gate provides free VPN servers operated by volunteers
 * API endpoint: https://www.vpngate.net/api/iphone/
 * Returns CSV format, not JSON
 * No authentication required
 */
interface VpnApiService {
    
    /**
     * Get list of VPN servers from VPN Gate
     * Returns CSV format with server information
     * @return Response containing CSV data
     */
    @GET("api/iphone/")
    suspend fun getServers(): Response<ResponseBody>
}

