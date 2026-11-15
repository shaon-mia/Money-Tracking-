package com.example.data.repository

import android.content.Context

object StorageModeStore {
    private const val PREF_NAME = "moneytracker_storage_prefs"
    private const val KEY_MODE = "storage_mode"
    private const val KEY_MIGRATED_UIDS = "migrated_local_uids"

    const val MODE_CLOUD = "cloud"
    const val MODE_LOCAL = "local"

    fun getMode(context: Context): String {
        val sp = context.applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return sp.getString(KEY_MODE, MODE_CLOUD) ?: MODE_CLOUD
    }

    fun setMode(context: Context, mode: String) {
        val normalized = if (mode == MODE_LOCAL) MODE_LOCAL else MODE_CLOUD
        val sp = context.applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        sp.edit().putString(KEY_MODE, normalized).apply()
    }

    fun isLocalMigratedTo(context: Context, uid: String): Boolean {
        val sp = context.applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val set = sp.getStringSet(KEY_MIGRATED_UIDS, emptySet()) ?: emptySet()
        return uid in set
    }

    fun markLocalMigrated(context: Context, uid: String) {
        val sp = context.applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val existing = (sp.getStringSet(KEY_MIGRATED_UIDS, emptySet()) ?: emptySet()).toMutableSet()
        existing.add(uid)
        sp.edit().putStringSet(KEY_MIGRATED_UIDS, existing).apply()
    }

    fun clearLocalMigratedFlags(context: Context) {
        val sp = context.applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        sp.edit().remove(KEY_MIGRATED_UIDS).apply()
    }

    fun needsCatalogUpgrade(context: Context, scopeId: String, targetVersion: Int = 2): Boolean {
        val sp = context.applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return sp.getInt("catalog_ver_$scopeId", 1) < targetVersion
    }

    fun markCatalogUpgraded(context: Context, scopeId: String, targetVersion: Int = 2) {
        val sp = context.applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        sp.edit().putInt("catalog_ver_$scopeId", targetVersion).apply()
    }
}
