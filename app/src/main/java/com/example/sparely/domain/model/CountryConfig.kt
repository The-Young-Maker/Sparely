package com.example.sparely.domain.model

/**
 * Country-specific financial configurations including tax rates,
 * recommended savings percentages, and cultural financial norms.
 */
data class CountryConfig(
    val countryCode: String,
    val countryName: String,
    val defaultCurrency: String,
    val defaultLocale: String,
    val languageCode: String,
    val languageName: String,
    val taxConfig: TaxConfiguration,
    val savingsNorms: SavingsNorms,
    val financialCulture: FinancialCulture
)

/**
 * Tax configuration for a country.
 */
data class TaxConfiguration(
    val incomeTaxRate: Double, // Average income tax rate
    val salesTaxRate: Double,  // VAT/Sales tax rate
    val socialSecurityRate: Double, // Social security/pension contributions
    val hasProgressiveTax: Boolean,
    val taxBrackets: List<TaxBracket> = emptyList()
)

data class TaxBracket(
    val minIncome: Double,
    val maxIncome: Double?,
    val rate: Double
)

/**
 * Cultural savings norms and recommendations for a country.
 */
data class SavingsNorms(
    val recommendedEmergencyMonths: Int, // Number of months of expenses
    val typicalSavingsRate: Double, // % of income people typically save
    val retirementSavingsRate: Double, // Recommended retirement savings %
    val typicalInvestmentAge: Int // Age people typically start investing
)

/**
 * Financial culture and norms.
 */
data class FinancialCulture(
    val cashPreference: CashPreference,
    val debtAttitude: DebtAttitude,
    val homeOwnershipRate: Double, // % of population that owns homes
    val typicalRetirementAge: Int
)

enum class CashPreference {
    MOSTLY_CASH,
    BALANCED,
    MOSTLY_DIGITAL
}

enum class DebtAttitude {
    DEBT_AVERSE,    // Avoid debt at all costs
    PRAGMATIC,      // Use debt strategically
    DEBT_COMFORTABLE // Comfortable with debt
}

/**
 * Pre-configured country profiles with realistic financial data.
 */
object CountryProfiles {
    
    val UNITED_STATES = CountryConfig(
        countryCode = "US",
        countryName = "United States",
        defaultCurrency = "USD",
        defaultLocale = "en_US",
        languageCode = "en",
        languageName = "English",
        taxConfig = TaxConfiguration(
            incomeTaxRate = 0.22,
            salesTaxRate = 0.07,
            socialSecurityRate = 0.0765,
            hasProgressiveTax = true,
            taxBrackets = listOf(
                TaxBracket(0.0, 11000.0, 0.10),
                TaxBracket(11000.0, 44725.0, 0.12),
                TaxBracket(44725.0, 95375.0, 0.22),
                TaxBracket(95375.0, null, 0.24)
            )
        ),
        savingsNorms = SavingsNorms(
            recommendedEmergencyMonths = 6,
            typicalSavingsRate = 0.08,
            retirementSavingsRate = 0.15,
            typicalInvestmentAge = 25
        ),
        financialCulture = FinancialCulture(
            cashPreference = CashPreference.MOSTLY_DIGITAL,
            debtAttitude = DebtAttitude.PRAGMATIC,
            homeOwnershipRate = 0.65,
            typicalRetirementAge = 67
        )
    )
    
    val UNITED_KINGDOM = CountryConfig(
        countryCode = "GB",
        countryName = "United Kingdom",
        defaultCurrency = "GBP",
        defaultLocale = "en_GB",
        languageCode = "en",
        languageName = "English (UK)",
        taxConfig = TaxConfiguration(
            incomeTaxRate = 0.20,
            salesTaxRate = 0.20,
            socialSecurityRate = 0.12,
            hasProgressiveTax = true
        ),
        savingsNorms = SavingsNorms(
            recommendedEmergencyMonths = 6,
            typicalSavingsRate = 0.05,
            retirementSavingsRate = 0.12,
            typicalInvestmentAge = 30
        ),
        financialCulture = FinancialCulture(
            cashPreference = CashPreference.MOSTLY_DIGITAL,
            debtAttitude = DebtAttitude.PRAGMATIC,
            homeOwnershipRate = 0.63,
            typicalRetirementAge = 66
        )
    )
    
    val CANADA = CountryConfig(
        countryCode = "CA",
        countryName = "Canada",
        defaultCurrency = "CAD",
        defaultLocale = "en_CA",
        languageCode = "en",
        languageName = "English (CA)",
        taxConfig = TaxConfiguration(
            incomeTaxRate = 0.25,
            salesTaxRate = 0.13,
            socialSecurityRate = 0.057,
            hasProgressiveTax = true
        ),
        savingsNorms = SavingsNorms(
            recommendedEmergencyMonths = 6,
            typicalSavingsRate = 0.10,
            retirementSavingsRate = 0.18,
            typicalInvestmentAge = 28
        ),
        financialCulture = FinancialCulture(
            cashPreference = CashPreference.MOSTLY_DIGITAL,
            debtAttitude = DebtAttitude.PRAGMATIC,
            homeOwnershipRate = 0.67,
            typicalRetirementAge = 65
        )
    )
    
    val FRANCE = CountryConfig(
        countryCode = "FR",
        countryName = "France",
        defaultCurrency = "EUR",
        defaultLocale = "fr_FR",
        languageCode = "fr",
        languageName = "Français",
        taxConfig = TaxConfiguration(
            incomeTaxRate = 0.30,
            salesTaxRate = 0.20,
            socialSecurityRate = 0.22,
            hasProgressiveTax = true
        ),
        savingsNorms = SavingsNorms(
            recommendedEmergencyMonths = 4,
            typicalSavingsRate = 0.15,
            retirementSavingsRate = 0.10,
            typicalInvestmentAge = 35
        ),
        financialCulture = FinancialCulture(
            cashPreference = CashPreference.BALANCED,
            debtAttitude = DebtAttitude.DEBT_AVERSE,
            homeOwnershipRate = 0.58,
            typicalRetirementAge = 62
        )
    )
    
    val GERMANY = CountryConfig(
        countryCode = "DE",
        countryName = "Germany",
        defaultCurrency = "EUR",
        defaultLocale = "de_DE",
        languageCode = "de",
        languageName = "Deutsch",
        taxConfig = TaxConfiguration(
            incomeTaxRate = 0.30,
            salesTaxRate = 0.19,
            socialSecurityRate = 0.20,
            hasProgressiveTax = true
        ),
        savingsNorms = SavingsNorms(
            recommendedEmergencyMonths = 5,
            typicalSavingsRate = 0.18,
            retirementSavingsRate = 0.12,
            typicalInvestmentAge = 32
        ),
        financialCulture = FinancialCulture(
            cashPreference = CashPreference.MOSTLY_CASH,
            debtAttitude = DebtAttitude.DEBT_AVERSE,
            homeOwnershipRate = 0.51,
            typicalRetirementAge = 67
        )
    )
    
    val SPAIN = CountryConfig(
        countryCode = "ES",
        countryName = "Spain",
        defaultCurrency = "EUR",
        defaultLocale = "es_ES",
        languageCode = "es",
        languageName = "Español",
        taxConfig = TaxConfiguration(
            incomeTaxRate = 0.24,
            salesTaxRate = 0.21,
            socialSecurityRate = 0.065,
            hasProgressiveTax = true
        ),
        savingsNorms = SavingsNorms(
            recommendedEmergencyMonths = 5,
            typicalSavingsRate = 0.06,
            retirementSavingsRate = 0.08,
            typicalInvestmentAge = 35
        ),
        financialCulture = FinancialCulture(
            cashPreference = CashPreference.BALANCED,
            debtAttitude = DebtAttitude.DEBT_AVERSE,
            homeOwnershipRate = 0.76,
            typicalRetirementAge = 67
        )
    )
    
    val JAPAN = CountryConfig(
        countryCode = "JP",
        countryName = "Japan",
        defaultCurrency = "JPY",
        defaultLocale = "ja_JP",
        languageCode = "ja",
        languageName = "日本語",
        taxConfig = TaxConfiguration(
            incomeTaxRate = 0.20,
            salesTaxRate = 0.10,
            socialSecurityRate = 0.15,
            hasProgressiveTax = true
        ),
        savingsNorms = SavingsNorms(
            recommendedEmergencyMonths = 8,
            typicalSavingsRate = 0.25,
            retirementSavingsRate = 0.12,
            typicalInvestmentAge = 30
        ),
        financialCulture = FinancialCulture(
            cashPreference = CashPreference.MOSTLY_CASH,
            debtAttitude = DebtAttitude.DEBT_AVERSE,
            homeOwnershipRate = 0.61,
            typicalRetirementAge = 65
        )
    )
    
    val AUSTRALIA = CountryConfig(
        countryCode = "AU",
        countryName = "Australia",
        defaultCurrency = "AUD",
        defaultLocale = "en_AU",
        languageCode = "en",
        languageName = "English (AU)",
        taxConfig = TaxConfiguration(
            incomeTaxRate = 0.25,
            salesTaxRate = 0.10,
            socialSecurityRate = 0.095,
            hasProgressiveTax = true
        ),
        savingsNorms = SavingsNorms(
            recommendedEmergencyMonths = 6,
            typicalSavingsRate = 0.09,
            retirementSavingsRate = 0.12,
            typicalInvestmentAge = 27
        ),
        financialCulture = FinancialCulture(
            cashPreference = CashPreference.MOSTLY_DIGITAL,
            debtAttitude = DebtAttitude.PRAGMATIC,
            homeOwnershipRate = 0.66,
            typicalRetirementAge = 67
        )
    )
    
    val INDIA = CountryConfig(
        countryCode = "IN",
        countryName = "India",
        defaultCurrency = "INR",
        defaultLocale = "en_IN",
        languageCode = "en",
        languageName = "English (IN)",
        taxConfig = TaxConfiguration(
            incomeTaxRate = 0.20,
            salesTaxRate = 0.18,
            socialSecurityRate = 0.12,
            hasProgressiveTax = true
        ),
        savingsNorms = SavingsNorms(
            recommendedEmergencyMonths = 8,
            typicalSavingsRate = 0.30,
            retirementSavingsRate = 0.15,
            typicalInvestmentAge = 25
        ),
        financialCulture = FinancialCulture(
            cashPreference = CashPreference.BALANCED,
            debtAttitude = DebtAttitude.DEBT_AVERSE,
            homeOwnershipRate = 0.87,
            typicalRetirementAge = 60
        )
    )
    
    val MEXICO = CountryConfig(
        countryCode = "MX",
        countryName = "Mexico",
        defaultCurrency = "MXN",
        defaultLocale = "es_MX",
        languageCode = "es",
        languageName = "Español (MX)",
        taxConfig = TaxConfiguration(
            incomeTaxRate = 0.30,
            salesTaxRate = 0.16,
            socialSecurityRate = 0.065,
            hasProgressiveTax = true
        ),
        savingsNorms = SavingsNorms(
            recommendedEmergencyMonths = 6,
            typicalSavingsRate = 0.12,
            retirementSavingsRate = 0.10,
            typicalInvestmentAge = 30
        ),
        financialCulture = FinancialCulture(
            cashPreference = CashPreference.MOSTLY_CASH,
            debtAttitude = DebtAttitude.PRAGMATIC,
            homeOwnershipRate = 0.62,
            typicalRetirementAge = 65
        )
    )
    
    val BRAZIL = CountryConfig(
        countryCode = "BR",
        countryName = "Brazil",
        defaultCurrency = "BRL",
        defaultLocale = "pt_BR",
        languageCode = "pt",
        languageName = "Português",
        taxConfig = TaxConfiguration(
            incomeTaxRate = 0.275,
            salesTaxRate = 0.17,
            socialSecurityRate = 0.11,
            hasProgressiveTax = true
        ),
        savingsNorms = SavingsNorms(
            recommendedEmergencyMonths = 6,
            typicalSavingsRate = 0.05,
            retirementSavingsRate = 0.08,
            typicalInvestmentAge = 28
        ),
        financialCulture = FinancialCulture(
            cashPreference = CashPreference.BALANCED,
            debtAttitude = DebtAttitude.PRAGMATIC,
            homeOwnershipRate = 0.75,
            typicalRetirementAge = 65
        )
    )
    
    /**
     * All available country profiles.
     */
    val ALL_COUNTRIES = listOf(
        UNITED_STATES,
        UNITED_KINGDOM,
        CANADA,
        FRANCE,
        GERMANY,
        SPAIN,
        JAPAN,
        AUSTRALIA,
        INDIA,
        MEXICO,
        BRAZIL
    )
    
    /**
     * Get country config by country code.
     */
    fun getByCode(code: String): CountryConfig? = 
        ALL_COUNTRIES.find { it.countryCode == code }
    
    /**
     * Get countries by language.
     */
    fun getByLanguage(languageCode: String): List<CountryConfig> = 
        ALL_COUNTRIES.filter { it.languageCode == languageCode }
}
