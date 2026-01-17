package com.example.sparely.data.remote

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface BrandfetchApi {
    @GET("v2/search/{name}")
    suspend fun searchBrands(
        @Path("name") name: String,
        @Query("c") clientId: String
    ): retrofit2.Response<List<BrandfetchBrand>>
}
