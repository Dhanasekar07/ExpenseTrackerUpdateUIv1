package com.example.expensetracker

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

data class Expense(
    val id        : String = "",
    val category  : String,
    val amount    : Double,
    val source    : String,
    val channel   : String,
    val timestamp : Long
)

class ExpenseDbHelper(context: Context) :
    SQLiteOpenHelper(context, "expenses.db", null, 2) {

    companion object {
        const val TABLE = "expenses"
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE $TABLE (
                id        TEXT PRIMARY KEY,
                category  TEXT NOT NULL,
                amount    REAL,
                source    TEXT,
                channel   TEXT,
                timestamp INTEGER NOT NULL
            )
        """.trimIndent())
    }

    override fun onUpgrade(db: SQLiteDatabase, old: Int, new: Int) {
        // Preserve data on upgrade
        if (old < 2) {
            try {
                db.execSQL("ALTER TABLE $TABLE ADD COLUMN id TEXT")
            } catch (e: Exception) { /* already exists */ }
        }
    }

    fun insertExpense(
        category: String, amount: Double,
        source: String, channel: String = "unknown"
    ): Long {
        val id = java.util.UUID.randomUUID().toString()
        val cv = ContentValues().apply {
            put("id",       id)
            put("category", category)
            put("amount",   amount)
            put("source",   source)
            put("channel",  channel)
            put("timestamp", System.currentTimeMillis())
        }
        return writableDatabase.insert(TABLE, null, cv)
    }

    fun getExpenses(fromTs: Long = 0L, toTs: Long = Long.MAX_VALUE): List<Expense> {
        val list = mutableListOf<Expense>()
        val c: Cursor = readableDatabase.query(
            TABLE, null,
            "timestamp >= ? AND timestamp <= ?",
            arrayOf(fromTs.toString(), toTs.toString()),
            null, null, "timestamp DESC"
        )
        c.use {
            while (it.moveToNext()) {
                list.add(Expense(
                    id        = it.getString(it.getColumnIndexOrThrow("id")) ?: "",
                    category  = it.getString(it.getColumnIndexOrThrow("category")),
                    amount    = it.getDouble(it.getColumnIndexOrThrow("amount")),
                    source    = it.getString(it.getColumnIndexOrThrow("source")) ?: "",
                    channel   = it.getString(it.getColumnIndexOrThrow("channel")) ?: "",
                    timestamp = it.getLong(it.getColumnIndexOrThrow("timestamp"))
                ))
            }
        }
        return list
    }

    fun getTotalByCategory(fromTs: Long = 0L, toTs: Long = Long.MAX_VALUE): Map<String, Double> {
        val map = mutableMapOf<String, Double>()
        val c   = readableDatabase.rawQuery(
            "SELECT category, SUM(amount) FROM $TABLE WHERE timestamp >= ? AND timestamp <= ? GROUP BY category",
            arrayOf(fromTs.toString(), toTs.toString())
        )
        c.use { while (it.moveToNext()) map[it.getString(0)] = it.getDouble(1) }
        return map
    }

    fun getTransactionCount(category: String, fromTs: Long, toTs: Long): Int {
        val c = readableDatabase.rawQuery(
            "SELECT COUNT(*) FROM $TABLE WHERE category=? AND timestamp>=? AND timestamp<=?",
            arrayOf(category, fromTs.toString(), toTs.toString())
        )
        c.use { return if (it.moveToFirst()) it.getInt(0) else 0 }
    }

    fun deleteExpense(id: String) {
        writableDatabase.delete(TABLE, "id = ?", arrayOf(id))
    }

    fun updateExpenseCategory(id: String, newCategory: String) {
        writableDatabase.update(TABLE,
            ContentValues().apply { put("category", newCategory) },
            "id = ?", arrayOf(id))
    }

    fun clearAll() {
        writableDatabase.delete(TABLE, null, null)
    }
}
