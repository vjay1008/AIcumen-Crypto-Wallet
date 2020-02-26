package com.vijay.blockchain.model

import java.io.Serializable

data class LinkedinToken(
    val accessToken: String,
    val expiredTime: Long
) : Serializable