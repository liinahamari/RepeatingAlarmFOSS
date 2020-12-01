package com.example.repeatingalarmfoss.helper.extensions

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import androidx.preference.PreferenceManager
import com.example.repeatingalarmfoss.R
import java.util.*

fun Context.provideUpdatedContextWithNewLocale(
    language: String? = kotlin.runCatching { getDefaultSharedPreferences().getStringOf(PREF_APP_LANG) }.getOrNull(),
    defaultLocale: String = Locale.getDefault().language
): Context {
    val locales = resources.getStringArray(R.array.supported_locales)
    val newLocale = Locale(locales.firstOrNull { it == language || it == defaultLocale } ?: Locale.UK.language)
    getDefaultSharedPreferences().writeStringOf(PREF_APP_LANG, newLocale.language)
    Locale.setDefault(newLocale)
    return createConfigurationContext(Configuration().apply { setLocale(newLocale) })
}

fun Context.getDefaultSharedPreferences(): SharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)