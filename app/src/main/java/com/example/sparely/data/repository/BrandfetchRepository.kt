package com.example.sparely.data.repository

import com.example.sparely.data.remote.BrandfetchApi
import com.example.sparely.data.remote.BrandfetchBrand
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

interface BrandfetchRepository {
    suspend fun searchBrands(query: String, clientId: String): List<BrandfetchBrand>
}

class BrandfetchRepositoryImpl(
    private val api: BrandfetchApi
) : BrandfetchRepository {
    override suspend fun searchBrands(query: String, clientId: String): List<BrandfetchBrand> {
        return withContext(Dispatchers.IO) {
            try {
                val response = api.searchBrands(query, clientId)
                if (response.isSuccessful) {
                    response.body() ?: emptyList()
                } else {
                    emptyList()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                emptyList()
            }
        }
    }
}
