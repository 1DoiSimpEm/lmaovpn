package com.amobear.freevpn.data.network.api.dto

import com.google.gson.annotations.SerializedName

data class SignalRegisterRequest(
    @SerializedName("dev_id") val devId: String,
    @SerializedName("dev_model") val devModel: String,
    @SerializedName("dev_manufacturer") val devManufacturer: String,
    @SerializedName("dev_lang") val devLang: String,
    @SerializedName("dev_os") val devOs: String,
    @SerializedName("dev_country") val devCountry: String,
    @SerializedName("app_package") val appPackage: String,
    @SerializedName("app_ver_name") val appVerName: String,
    @SerializedName("app_ver_code") val appVerCode: Int
)

data class SignalRegisterResponse(
    @SerializedName("auth_id") val authId: Long = 0,
    @SerializedName("auth_token") val authToken: Long = 0
)

