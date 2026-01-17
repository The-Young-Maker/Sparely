package com.example.sparely.domain.model

import java.time.LocalDate

/**
 * Domain representation of a store or website where expenses are made.
 */
data class Store(
    val id: Long = 0,
    val name: String,
    val websiteUrl: String? = null,
    val iconName: String? = null,
    val createdAt: LocalDate = LocalDate.now()
) {
    /**
     * Returns the Brandfetch logo URL for this store if a website URL is set.
     * @param clientId The Brandfetch client ID from settings
     */
    fun getBrandfetchLogoUrl(clientId: String?): String? {
        if (websiteUrl.isNullOrBlank() || clientId.isNullOrBlank()) return null
        val domain = websiteUrl
            .removePrefix("https://")
            .removePrefix("http://")
            .removePrefix("www.")
            .split("/")
            .firstOrNull()
            ?.trim()
        return if (domain.isNullOrBlank()) null else "https://cdn.brandfetch.io/$domain?c=$clientId"
    }
}

/**
 * User input for creating a new store.
 */
data class StoreInput(
    val name: String,
    val websiteUrl: String? = null,
    val iconName: String? = null
)
