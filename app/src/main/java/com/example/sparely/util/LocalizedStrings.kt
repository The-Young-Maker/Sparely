package com.example.sparely.util

/**
 * Localization strings for the app.
 * This provides translation support for multiple languages.
 */
object LocalizedStrings {
    
    /**
     * Get localized string by key and language code.
     */
    fun get(key: String, languageCode: String = "en"): String {
        val strings = when (languageCode) {
            "es" -> spanishStrings
            "fr" -> frenchStrings
            "de" -> germanStrings
            "pt" -> portugueseStrings
            "ja" -> japaneseStrings
            else -> englishStrings
        }
        return strings[key] ?: englishStrings[key] ?: key
    }
    
    // English strings (default)
    private val englishStrings = mapOf(
        // Onboarding
        "onboarding_welcome" to "Welcome to Sparely",
        "onboarding_subtitle" to "Smart savings made simple",
        "onboarding_country_title" to "Choose your country",
        "onboarding_country_subtitle" to "We'll customize the app for your region",
        "onboarding_name_title" to "What's your name?",
        "onboarding_name_hint" to "Enter your name",
        "onboarding_income_title" to "Monthly income",
        "onboarding_income_hint" to "Enter your monthly income",
        "onboarding_age_title" to "Your age",
        "onboarding_living_title" to "Living situation",
        "onboarding_living_with_parents" to "With family",
        "onboarding_living_renting" to "Renting",
        "onboarding_living_homeowner" to "Homeowner",
        "onboarding_living_other" to "Other",
        "onboarding_accounts_title" to "Current accounts",
        "onboarding_main_account" to "Main account balance",
        "onboarding_savings_account" to "Savings account balance",
        "onboarding_risk_title" to "Risk profile",
        "onboarding_risk_conservative" to "Conservative",
        "onboarding_risk_balanced" to "Balanced",
        "onboarding_risk_aggressive" to "Aggressive",
        "onboarding_debts_title" to "Do you have debts?",
        "onboarding_emergency_title" to "Emergency fund",
        "onboarding_goal_title" to "Primary financial goal",
        
        // Common
        "continue" to "Continue",
        "skip" to "Skip",
        "back" to "Back",
        "save" to "Save",
        "cancel" to "Cancel",
        "delete" to "Delete",
        "edit" to "Edit",
        "yes" to "Yes",
        "no" to "No",
        
        // Currency
        "currency_symbol" to "$",
        
        // Dashboard
        "dashboard_title" to "Dashboard",
        "dashboard_total_saved" to "Total Saved",
        "dashboard_this_month" to "This Month",
    "history_title" to "History",
    "vaults_title" to "Vaults",
    "budgets_title" to "Budgets",
    "challenges_title" to "Challenges",
    "recurring_title" to "Recurring",
    "health_title" to "Health",
    "expense_entry_title" to "Log purchase",
        
        // Settings
        "settings_title" to "Settings",
        "settings_profile" to "Profile",
        "settings_regional" to "Regional Settings",
        "settings_language" to "Language",
        "settings_currency" to "Currency",
        "settings_notifications" to "Notifications"
    )
    
    // Spanish strings
    private val spanishStrings = mapOf(
        "onboarding_welcome" to "Bienvenido a Sparely",
        "onboarding_subtitle" to "Ahorro inteligente simplificado",
        "onboarding_country_title" to "Elige tu país",
        "onboarding_country_subtitle" to "Personalizaremos la aplicación para tu región",
        "onboarding_name_title" to "¿Cómo te llamas?",
        "onboarding_name_hint" to "Ingresa tu nombre",
        "onboarding_income_title" to "Ingresos mensuales",
        "onboarding_income_hint" to "Ingresa tus ingresos mensuales",
        "onboarding_age_title" to "Tu edad",
        "onboarding_living_title" to "Situación de vivienda",
        "onboarding_living_with_parents" to "Con familia",
        "onboarding_living_renting" to "Alquilando",
        "onboarding_living_homeowner" to "Propietario",
        "onboarding_living_other" to "Otro",
        "onboarding_accounts_title" to "Cuentas actuales",
        "onboarding_main_account" to "Saldo cuenta principal",
        "onboarding_savings_account" to "Saldo cuenta de ahorros",
        "onboarding_risk_title" to "Perfil de riesgo",
        "onboarding_risk_conservative" to "Conservador",
        "onboarding_risk_balanced" to "Equilibrado",
        "onboarding_risk_aggressive" to "Agresivo",
        "onboarding_debts_title" to "¿Tienes deudas?",
        "onboarding_emergency_title" to "Fondo de emergencia",
        "onboarding_goal_title" to "Objetivo financiero principal",
        "continue" to "Continuar",
        "skip" to "Saltar",
        "back" to "Atrás",
        "save" to "Guardar",
        "cancel" to "Cancelar",
        "delete" to "Eliminar",
        "edit" to "Editar",
        "yes" to "Sí",
        "no" to "No"
    )
    
    // French strings
    private val frenchStrings = mapOf(
        "onboarding_welcome" to "Bienvenue sur Sparely",
        "onboarding_subtitle" to "Épargne intelligente simplifiée",
        "onboarding_country_title" to "Choisissez votre pays",
        "onboarding_country_subtitle" to "Nous personnaliserons l'application pour votre région",
        "onboarding_name_title" to "Comment vous appelez-vous?",
        "onboarding_name_hint" to "Entrez votre nom",
        "onboarding_income_title" to "Revenu mensuel",
        "onboarding_income_hint" to "Entrez votre revenu mensuel",
        "onboarding_age_title" to "Votre âge",
        "onboarding_living_title" to "Situation de logement",
        "onboarding_living_with_parents" to "Avec la famille",
        "onboarding_living_renting" to "Locataire",
        "onboarding_living_homeowner" to "Propriétaire",
        "onboarding_living_other" to "Autre",
        "onboarding_accounts_title" to "Comptes actuels",
        "onboarding_main_account" to "Solde compte principal",
        "onboarding_savings_account" to "Solde compte épargne",
        "onboarding_risk_title" to "Profil de risque",
        "onboarding_risk_conservative" to "Conservateur",
        "onboarding_risk_balanced" to "Équilibré",
        "onboarding_risk_aggressive" to "Agressif",
        "onboarding_debts_title" to "Avez-vous des dettes?",
        "onboarding_emergency_title" to "Fonds d'urgence",
        "onboarding_goal_title" to "Objectif financier principal",
        "continue" to "Continuer",
        "skip" to "Passer",
        "back" to "Retour",
        "save" to "Enregistrer",
        "cancel" to "Annuler",
        "delete" to "Supprimer",
        "edit" to "Modifier",
        "yes" to "Oui",
        "no" to "Non"
    )
    
    // German strings
    private val germanStrings = mapOf(
        "onboarding_welcome" to "Willkommen bei Sparely",
        "onboarding_subtitle" to "Intelligentes Sparen leicht gemacht",
        "onboarding_country_title" to "Wählen Sie Ihr Land",
        "onboarding_country_subtitle" to "Wir passen die App für Ihre Region an",
        "onboarding_name_title" to "Wie heißen Sie?",
        "onboarding_name_hint" to "Geben Sie Ihren Namen ein",
        "onboarding_income_title" to "Monatliches Einkommen",
        "onboarding_income_hint" to "Geben Sie Ihr monatliches Einkommen ein",
        "onboarding_age_title" to "Ihr Alter",
        "onboarding_living_title" to "Wohnsituation",
        "onboarding_living_with_parents" to "Bei Familie",
        "onboarding_living_renting" to "Mieter",
        "onboarding_living_homeowner" to "Eigentümer",
        "onboarding_living_other" to "Andere",
        "onboarding_accounts_title" to "Aktuelle Konten",
        "onboarding_main_account" to "Hauptkonto Saldo",
        "onboarding_savings_account" to "Sparkonto Saldo",
        "onboarding_risk_title" to "Risikoprofil",
        "onboarding_risk_conservative" to "Konservativ",
        "onboarding_risk_balanced" to "Ausgewogen",
        "onboarding_risk_aggressive" to "Aggressiv",
        "onboarding_debts_title" to "Haben Sie Schulden?",
        "onboarding_emergency_title" to "Notfallfonds",
        "onboarding_goal_title" to "Hauptfinanzziel",
        "continue" to "Weiter",
        "skip" to "Überspringen",
        "back" to "Zurück",
        "save" to "Speichern",
        "cancel" to "Abbrechen",
        "delete" to "Löschen",
        "edit" to "Bearbeiten",
        "yes" to "Ja",
        "no" to "Nein"
    )
    
    // Portuguese strings
    private val portugueseStrings = mapOf(
        "onboarding_welcome" to "Bem-vindo ao Sparely",
        "onboarding_subtitle" to "Economia inteligente simplificada",
        "onboarding_country_title" to "Escolha seu país",
        "onboarding_country_subtitle" to "Personalizaremos o aplicativo para sua região",
        "onboarding_name_title" to "Qual é o seu nome?",
        "onboarding_name_hint" to "Digite seu nome",
        "onboarding_income_title" to "Renda mensal",
        "onboarding_income_hint" to "Digite sua renda mensal",
        "onboarding_age_title" to "Sua idade",
        "onboarding_living_title" to "Situação de moradia",
        "onboarding_living_with_parents" to "Com família",
        "onboarding_living_renting" to "Alugando",
        "onboarding_living_homeowner" to "Proprietário",
        "onboarding_living_other" to "Outro",
        "onboarding_accounts_title" to "Contas atuais",
        "onboarding_main_account" to "Saldo conta principal",
        "onboarding_savings_account" to "Saldo conta poupança",
        "onboarding_risk_title" to "Perfil de risco",
        "onboarding_risk_conservative" to "Conservador",
        "onboarding_risk_balanced" to "Equilibrado",
        "onboarding_risk_aggressive" to "Agressivo",
        "onboarding_debts_title" to "Você tem dívidas?",
        "onboarding_emergency_title" to "Fundo de emergência",
        "onboarding_goal_title" to "Objetivo financeiro principal",
        "continue" to "Continuar",
        "skip" to "Pular",
        "back" to "Voltar",
        "save" to "Salvar",
        "cancel" to "Cancelar",
        "delete" to "Excluir",
        "edit" to "Editar",
        "yes" to "Sim",
        "no" to "Não"
    )
    
    // Japanese strings
    private val japaneseStrings = mapOf(
        "onboarding_welcome" to "Sparelyへようこそ",
        "onboarding_subtitle" to "スマートな貯蓄を簡単に",
        "onboarding_country_title" to "国を選択してください",
        "onboarding_country_subtitle" to "お住まいの地域に合わせてアプリをカスタマイズします",
        "onboarding_name_title" to "お名前は?",
        "onboarding_name_hint" to "名前を入力してください",
        "onboarding_income_title" to "月収",
        "onboarding_income_hint" to "月収を入力してください",
        "onboarding_age_title" to "年齢",
        "onboarding_living_title" to "居住状況",
        "onboarding_living_with_parents" to "家族と同居",
        "onboarding_living_renting" to "賃貸",
        "onboarding_living_homeowner" to "持ち家",
        "onboarding_living_other" to "その他",
        "onboarding_accounts_title" to "現在の口座",
        "onboarding_main_account" to "メイン口座残高",
        "onboarding_savings_account" to "貯蓄口座残高",
        "onboarding_risk_title" to "リスクプロファイル",
        "onboarding_risk_conservative" to "保守的",
        "onboarding_risk_balanced" to "バランス型",
        "onboarding_risk_aggressive" to "積極的",
        "onboarding_debts_title" to "借金がありますか?",
        "onboarding_emergency_title" to "緊急資金",
        "onboarding_goal_title" to "主な財務目標",
        "continue" to "続ける",
        "skip" to "スキップ",
        "back" to "戻る",
        "save" to "保存",
        "cancel" to "キャンセル",
        "delete" to "削除",
        "edit" to "編集",
        "yes" to "はい",
        "no" to "いいえ"
    )
}

/**
 * Extension function for easy localization.
 */
fun String.localized(languageCode: String = "en"): String {
    return LocalizedStrings.get(this, languageCode)
}
