package com.example.sparely.domain.model

import java.util.Currency
import java.util.Locale

/**
 * Regional and localization settings for the application.
 */
data class RegionalSettings(
    val countryCode: String = "US",
    val languageCode: String = "en",
    val currencyCode: String = "USD",
    val locale: String = "en_US",
    val dateFormat: DateFormatPreference = DateFormatPreference.MONTH_DAY_YEAR,
    val use24HourTime: Boolean = false,
    val numberFormat: NumberFormatPreference = NumberFormatPreference.STANDARD,
    val customIncomeTaxRate: Double? = null // Optional override for country's default tax rate
) {
    /**
     * Get the Currency instance for the configured currency code.
     */
    fun getCurrency(): Currency = try {
        Currency.getInstance(currencyCode)
    } catch (e: IllegalArgumentException) {
        Currency.getInstance("USD")
    }
    
    /**
     * Get the Locale instance for the configured locale.
     */
    fun getLocale(): Locale = try {
        val parts = locale.split("_")
        when (parts.size) {
            1 -> Locale(parts[0])
            2 -> Locale(parts[0], parts[1])
            else -> Locale.US
        }
    } catch (e: Exception) {
        Locale.US
    }
    
    /**
     * Get the currency symbol for display (e.g., "$", "€", "£").
     */
    fun getCurrencySymbol(): String = try {
        getCurrency().symbol
    } catch (e: Exception) {
        "$"
    }
    
    /**
     * Get the country configuration if available.
     */
    fun getCountryConfig(): CountryConfig? = CountryProfiles.getByCode(countryCode)
    
    /**
     * Get the effective income tax rate (custom override or country default).
     */
    fun getEffectiveIncomeTaxRate(): Double {
        return customIncomeTaxRate ?: getCountryConfig()?.taxConfig?.incomeTaxRate ?: 0.22
    }
    
    companion object {
        /**
         * Create regional settings from a country configuration.
         */
        fun fromCountryConfig(config: CountryConfig): RegionalSettings {
            return RegionalSettings(
                countryCode = config.countryCode,
                languageCode = config.languageCode,
                currencyCode = config.defaultCurrency,
                locale = config.defaultLocale,
                dateFormat = when (config.countryCode) {
                    "US" -> DateFormatPreference.MONTH_DAY_YEAR
                    "JP", "CN", "KR" -> DateFormatPreference.YEAR_MONTH_DAY
                    else -> DateFormatPreference.DAY_MONTH_YEAR
                },
                use24HourTime = config.countryCode != "US",
                numberFormat = when (config.defaultLocale) {
                    "de_DE", "es_ES", "fr_FR" -> NumberFormatPreference.EUROPEAN
                    "en_IN" -> NumberFormatPreference.INDIAN
                    else -> NumberFormatPreference.STANDARD
                }
            )
        }
    }
}

/**
 * Date format preferences.
 */
enum class DateFormatPreference(val pattern: String, val displayName: String) {
    MONTH_DAY_YEAR("MM/dd/yyyy", "MM/DD/YYYY"),
    DAY_MONTH_YEAR("dd/MM/yyyy", "DD/MM/YYYY"),
    YEAR_MONTH_DAY("yyyy-MM-dd", "YYYY-MM-DD"),
    MONTH_DAY_YEAR_LONG("MMMM d, yyyy", "Month Day, Year");
    
    fun format(date: java.time.LocalDate): String {
        return date.format(java.time.format.DateTimeFormatter.ofPattern(pattern))
    }
}

/**
 * Number format preferences for displaying amounts.
 */
enum class NumberFormatPreference(val displayName: String) {
    STANDARD("1,234.56"),
    EUROPEAN("1.234,56"),
    INDIAN("1,23,456.78"),
    SPACES("1 234.56")
}

/**
 * Common currency presets for quick selection.
 */
object CurrencyPresets {
    data class CurrencyInfo(
        val code: String,
        val symbol: String,
        val name: String,
        val locale: String
    )
    
    val common = listOf(
        CurrencyInfo("USD", "$", "US Dollar", "en_US"),
        CurrencyInfo("EUR", "€", "Euro", "en_EU"),
        CurrencyInfo("GBP", "£", "British Pound", "en_GB"),
        CurrencyInfo("JPY", "¥", "Japanese Yen", "ja_JP"),
        CurrencyInfo("CAD", "$", "Canadian Dollar", "en_CA"),
        CurrencyInfo("AUD", "$", "Australian Dollar", "en_AU"),
        CurrencyInfo("CHF", "Fr", "Swiss Franc", "de_CH"),
        CurrencyInfo("CNY", "¥", "Chinese Yuan", "zh_CN"),
        CurrencyInfo("INR", "₹", "Indian Rupee", "en_IN"),
        CurrencyInfo("MXN", "$", "Mexican Peso", "es_MX"),
        CurrencyInfo("BRL", "R$", "Brazilian Real", "pt_BR"),
        CurrencyInfo("ZAR", "R", "South African Rand", "en_ZA"),
        CurrencyInfo("KRW", "₩", "South Korean Won", "ko_KR"),
        CurrencyInfo("SGD", "$", "Singapore Dollar", "en_SG"),
        CurrencyInfo("NZD", "$", "New Zealand Dollar", "en_NZ")
    )
    
    fun getByCode(code: String): CurrencyInfo? = common.find { it.code == code }
}
