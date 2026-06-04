package com.example.expensetracker

import android.content.Context
import android.content.SharedPreferences

object AppPreferences {
    private const val PREF_NAME        = "expense_tracker_prefs"
    private const val KEY_USERNAME     = "pref_username"
    private const val KEY_COUNTRY      = "pref_country"
    private const val KEY_CURRENCY     = "pref_currency"
    private const val KEY_CURRENCY_SYM = "pref_currency_symbol"
    private const val KEY_NIGHT_MODE   = "pref_night_mode"
    private const val KEY_MIN_AMOUNT   = "pref_min_amount"
    private const val KEY_MAX_AMOUNT   = "pref_max_amount"
    private const val KEY_ONBOARDED    = "pref_onboarded"

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    // Onboarding
    var isOnboarded: Boolean
        get() = false
        set(value) {}
    fun isOnboarded(ctx: Context)  = prefs(ctx).getBoolean(KEY_ONBOARDED, false)
    fun setOnboarded(ctx: Context) = prefs(ctx).edit().putBoolean(KEY_ONBOARDED, true).apply()

    // Username
    fun getUsername(ctx: Context)        = prefs(ctx).getString(KEY_USERNAME, "User") ?: "User"
    fun setUsername(ctx: Context, v: String) = prefs(ctx).edit().putString(KEY_USERNAME, v).apply()

    // Country
    fun getCountry(ctx: Context)         = prefs(ctx).getString(KEY_COUNTRY, "India") ?: "India"
    fun setCountry(ctx: Context, v: String) = prefs(ctx).edit().putString(KEY_COUNTRY, v).apply()

    // Currency
    fun getCurrencySymbol(ctx: Context)  = prefs(ctx).getString(KEY_CURRENCY_SYM, "₹") ?: "₹"
    fun setCurrencySymbol(ctx: Context, v: String) =
        prefs(ctx).edit().putString(KEY_CURRENCY_SYM, v).apply()

    // Night mode
    fun isNightMode(ctx: Context)        = prefs(ctx).getBoolean(KEY_NIGHT_MODE, false)
    fun setNightMode(ctx: Context, v: Boolean) =
        prefs(ctx).edit().putBoolean(KEY_NIGHT_MODE, v).apply()

    // Thresholds
    fun getMinAmount(ctx: Context)       = prefs(ctx).getFloat(KEY_MIN_AMOUNT, 0f)
    fun setMinAmount(ctx: Context, v: Float) =
        prefs(ctx).edit().putFloat(KEY_MIN_AMOUNT, v).apply()

    fun getMaxAmount(ctx: Context)       = prefs(ctx).getFloat(KEY_MAX_AMOUNT, -1f)
    fun setMaxAmount(ctx: Context, v: Float) =
        prefs(ctx).edit().putFloat(KEY_MAX_AMOUNT, v).apply()
}
