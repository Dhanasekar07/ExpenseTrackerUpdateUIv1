package com.example.expensetracker

import android.app.DatePickerDialog
import android.graphics.Color
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import java.util.*

class ExpenseBreakdownActivity : AppCompatActivity() {

    private lateinit var db            : ExpenseDbHelper
    private lateinit var tvTotalSpent  : TextView
    private lateinit var tvPeriodLabel : TextView
    private lateinit var breakdownContainer: LinearLayout

    private var currentFilter = "month"
    private var customFrom    = 0L
    private var customTo      = 0L
    private val currency get() = AppPreferences.getCurrencySymbol(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_expense_breakdown)

        db                  = ExpenseDbHelper(this)
        tvTotalSpent        = findViewById(R.id.tvTotalSpent)
        tvPeriodLabel       = findViewById(R.id.tvPeriodLabel)
        breakdownContainer  = findViewById(R.id.breakdownContainer)

        // Get filter from intent
        currentFilter = intent.getStringExtra("filter") ?: "month"
        customFrom    = intent.getLongExtra("from", 0L)
        customTo      = intent.getLongExtra("to", 0L)

        CategoryManager.initialize(this)
        findViewById<ImageView>(R.id.btnBack).setOnClickListener { finish() }
        setupFilterTabs()
        setupChartPager()
        refreshData()
    }

    private fun setupFilterTabs() {
        val tabs = mapOf(
            R.id.tabDay to "day", R.id.tabWeek to "week",
            R.id.tabMonth to "month", R.id.tabCustom to "custom"
        )
        tabs.forEach { (id, filter) ->
            findViewById<TextView>(id).setOnClickListener {
                if (filter == "custom") showDatePicker()
                else { currentFilter = filter; updateTabUI(); refreshData() }
            }
        }
        updateTabUI()
    }

    private fun updateTabUI() {
        listOf(
            R.id.tabDay to "day", R.id.tabWeek to "week",
            R.id.tabMonth to "month", R.id.tabCustom to "custom"
        ).forEach { (id, f) ->
            val tv = findViewById<TextView>(id)
            if (f == currentFilter) {
                tv.setBackgroundResource(R.drawable.bg_filter_active)
                tv.setTextColor(Color.WHITE)
            } else {
                tv.setBackgroundResource(R.drawable.bg_filter_inactive)
                tv.setTextColor(Color.parseColor("#6B7280"))
            }
        }
        tvPeriodLabel.text = when (currentFilter) {
            "day"    -> "Today"
            "week"   -> "This Week"
            "month"  -> "This Month"
            "custom" -> "Custom Range"
            else     -> "This Month"
        }
    }

    private fun setupChartPager() {
        // Simple 2-dot indicator for now
        val dotsContainer = findViewById<LinearLayout>(R.id.dotsContainer)
        dotsContainer.removeAllViews()
        repeat(2) { i ->
            val dot = android.view.View(this).apply {
                layoutParams = LinearLayout.LayoutParams(10, 10).apply { setMargins(4,0,4,0) }
                background = android.graphics.drawable.GradientDrawable().apply {
                    shape = android.graphics.drawable.GradientDrawable.OVAL
                    setColor(if (i == 0) Color.parseColor("#2D6A4F") else Color.parseColor("#D1D5DB"))
                }
            }
            dotsContainer.addView(dot)
        }
    }

    private fun getFromTs(): Long {
        val cal = Calendar.getInstance()
        return when (currentFilter) {
            "day"    -> { cal.set(Calendar.HOUR_OF_DAY,0); cal.set(Calendar.MINUTE,0); cal.set(Calendar.SECOND,0); cal.timeInMillis }
            "week"   -> { cal.add(Calendar.DAY_OF_YEAR,-7); cal.timeInMillis }
            "month"  -> { cal.set(Calendar.DAY_OF_MONTH,1); cal.set(Calendar.HOUR_OF_DAY,0); cal.timeInMillis }
            "custom" -> customFrom
            else     -> 0L
        }
    }

    private fun getToTs() = if (currentFilter=="custom") customTo else System.currentTimeMillis()

    private fun refreshData() {
        val catTotals = db.getTotalByCategory(getFromTs(), getToTs())
        val total     = catTotals.values.sum()

        tvTotalSpent.text = "$currency${String.format("%.0f", total)}"

        breakdownContainer.removeAllViews()
        catTotals.entries.sortedByDescending { it.value }.forEach { (name, amt) ->
            val cat   = CategoryManager.getCategoryByName(name)
            val count = db.getTransactionCount(name, getFromTs(), getToTs())
            addBreakdownRow(name, amt, count, cat?.colorHex ?: "#9CA3AF")
        }
    }

    private fun addBreakdownRow(name: String, amount: Double, count: Int, colorHex: String) {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity     = android.view.Gravity.CENTER_VERTICAL
            background  = getDrawable(R.drawable.bg_normal_row)
            setPadding(16, 14, 16, 14)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 0, 0, 8) }
        }

        val iconBg = LinearLayout(this).apply {
            val lp = LinearLayout.LayoutParams(44, 44).apply { setMargins(0,0,12,0) }
            layoutParams = lp; gravity = android.view.Gravity.CENTER
            background = android.graphics.drawable.GradientDrawable().apply {
                shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                cornerRadius = 12f
                setColor(Color.parseColor(colorHex))
            }
        }
        iconBg.addView(TextView(this).apply {
            text = getCategoryEmoji(name); textSize = 16f
        })

        val info = LinearLayout(this).apply {
            orientation  = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        info.addView(TextView(this).apply {
            text = name; textSize = 14f
            setTextColor(Color.parseColor("#1A1A2E"))
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        })
        info.addView(TextView(this).apply {
            text = "$count Transactions"; textSize = 11f
            setTextColor(Color.parseColor("#9CA3AF"))
        })

        val amt = TextView(this).apply {
            text = "-$currency${String.format("%.2f", amount)}"
            textSize = 14f
            setTextColor(Color.parseColor("#EF4444"))
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }

        row.addView(iconBg); row.addView(info); row.addView(amt)
        breakdownContainer.addView(row)
    }

    private fun showDatePicker() {
        val cal = Calendar.getInstance()
        DatePickerDialog(this, { _, y, m, d ->
            val from = Calendar.getInstance().apply {
                set(y,m,d,0,0,0); set(Calendar.MILLISECOND,0)
            }.timeInMillis
            DatePickerDialog(this, { _, y2, m2, d2 ->
                val to = Calendar.getInstance().apply {
                    set(y2,m2,d2,23,59,59); set(Calendar.MILLISECOND,999)
                }.timeInMillis
                customFrom=from; customTo=to; currentFilter="custom"
                updateTabUI(); refreshData()
            }, y,m,d).show()
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun getCategoryEmoji(name: String) = when (name.lowercase()) {
        "food" -> "🍽️"; "tea/coffee" -> "☕"; "fuel" -> "⛽"
        "shopping" -> "🛍️"; "transport" -> "🚌"; "grocery" -> "🛒"
        "medicine" -> "💊"; "movies" -> "🎬"; "ott" -> "📺"
        else -> "💰"
    }
}
