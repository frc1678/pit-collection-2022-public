package com.example.pit_collection_2020

import android.content.Context

fun putIntoStorage(context: Context, key: String, value: Any) {
    context.getSharedPreferences("PREFS", 0).edit().putString(key, value.toString()).apply()
}

fun retrieveFromStorage(context: Context, key: String): String {
    return context.getSharedPreferences("PREFS", 0).getString(key, "").toString()
}