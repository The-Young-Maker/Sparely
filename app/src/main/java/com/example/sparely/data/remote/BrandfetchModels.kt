package com.example.sparely.data.remote

import com.google.gson.annotations.SerializedName

data class BrandfetchBrand(
    @SerializedName("brandId") val brandId: String?,
    @SerializedName("name") val name: String?,
    @SerializedName("domain") val domain: String?,
    @SerializedName("icon") val iconUrl: String?,
    @SerializedName("claimed") val claimed: Boolean?
)