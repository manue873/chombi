// app/src/main/java/com/negocio/chombi/prefs/UserPrefs.kt
package com.negocio.chombi.prefs

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore("chombi_prefs")

object UserPrefs {
    private val KEY_LAST_LINE = stringPreferencesKey("last_line_id")

    suspend fun getLastLineId(ctx: Context): String? =
        ctx.dataStore.data.map { it[KEY_LAST_LINE] }.first()

    suspend fun setLastLineId(ctx: Context, lineId: String) {
        ctx.dataStore.edit { it[KEY_LAST_LINE] = lineId }
    }
}
