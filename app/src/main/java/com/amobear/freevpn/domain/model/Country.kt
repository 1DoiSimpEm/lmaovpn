package com.amobear.freevpn.domain.model

data class Country(
    val code: String,
    val name: String,
    val flagEmoji: String,
    val serverCount: Int = 0
)

