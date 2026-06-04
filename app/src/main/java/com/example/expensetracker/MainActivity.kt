package com.example.expensetracker

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var db          : ExpenseDbHelper
    private lateinit var tvGreeting  : TextView
    private lateinit var tvTotalSpent: TextView
    private lateinit var pieChart    : PieChartView
    private lateinit var legendContainer  : LinearLayout
    private lateinit var transactionContainer: LinearLayout
    private lateinit var tvNoTxn     : TextView

    private var currentFilter = "day"
    private var customFrom    = 0L
    private var customTo      = 0L
    private val currency get() = AppPreferences.getCurrencySymbol(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Apply night mode
        if (AppPreferences.isNightMode(this))
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        else
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

        // Redirect to onboarding if not done
        if (!AppPreferences.isOnboarded(this)) {
            startActivity(Intent(this, OnboardingActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
            finish()
            return
        }

        // Check permissions
        if (!isNotifGranted() || !isOverlayGranted()) {
            startActivity(Intent(this, OnboardingActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
            finish()
            return
        }

        setContentView(R.layout.activity_main)

        db               = ExpenseDbHelper(this)
        tvGreeting       = findViewById(R.id.tvGreeting)
        tvTotalSpent     = findViewById(R.id.tvTotalSpent)
        pieChart         = findViewById(R.id.pieChart)
        legendContainer  = findViewById(R.id.legendContainer)
        transactionContainer = findViewById(R.id.transactionContainer)
        tvNoTxn          = findViewById(R.id.tvNoTransactions)

        CategoryManager.initialize(this)
        TransactionDeduplicator(this).cleanup()

        setupGreeting()
        setupFilterTabs()
        setupBottomNav()
        setupSeeAllButtons()
        refreshDashboard()
    }

    override fun onResume() {
        super.onResume()
        setupGreeting()
        refreshDashboard()
    }

    private fun setupGreeting() {
        tvGreeting.text = "Hello, ${AppPreferences.getUsername(this)}"
    }

    private fun setupFilterTabs() {
        val tabs = mapOf(
            R.id.tabDay    to "day",
            R.id.tabWeek   to "week",
            R.id.tabMonth  to "month",
            R.id.tabCustom to "custom"
        )
        tabs.forEach { (id, filter) ->
            findViewById<TextView>(id).setOnClickListener {
                if (filter == "custom") showDatePicker()
                else { currentFilter = filter; updateTabUI(); refreshDashboard() }
            }
        }
        updateTabUI()
    }

    private fun updateTabUI() {
        val tabs = listOf(R.id.tabDay, R.id.tabWeek, R.id.tabMonth, R.id.tabCustom)
        val filters = listOf("day", "week", "month", "custom")
        tabs.forEachIndexed { i, id ->
            val tv = findViewById<TextView>(id)
            if (filters[i] == currentFilter) {
                tv.setBackgroundResource(R.drawable.bg_filter_active)
                tv.setTextColor(Color.WHITE)
            } else {
                tv.setBackgroundResource(R.drawable.bg_filter_inactive)
                tv.setTextColor(Color.parseColor("#6B7280"))
            }
        }
    }

    private fun showDatePicker() {
        val cal = Calendar.getInstance()
        DatePickerDialog(this, { _, y, m, d ->
            val from = Calendar.getInstance().apply {
                set(y, m, d, 0, 0, 0); set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            DatePickerDialog(this, { _, y2, m2, d2 ->
                val to = Calendar.getInstance().apply {
                    set(y2, m2, d2, 23, 59, 59); set(Calendar.MILLISECOND, 999)
                }.timeInMillis
                customFrom = from; customTo = to
                currentFilter = "custom"
                updateTabUI(); refreshDashboard()
            }, y, m, d).show()
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun getFromTs(): Long {
        val cal = Calendar.getInstance()
        return when (currentFilter) {
            "day"    -> { cal.set(Calendar.HOUR_OF_DAY,0); cal.set(Calendar.MINUTE,0); cal.set(Calendar.SECOND,0); cal.timeInMillis }
            "week"   -> { cal.add(Calendar.DAY_OF_YEAR, -7); cal.timeInMillis }
            "month"  -> { cal.set(Calendar.DAY_OF_MONTH,1); cal.set(Calendar.HOUR_OF_DAY,0); cal.timeInMillis }
            "custom" -> customFrom
            else     -> 0L
        }
    }

    private fun getToTs(): Long = if (currentFilter == "custom") customTo else System.currentTimeMillis()

    private fun refreshDashboard() {
        val expenses  = db.getExpenses(getFromTs(), getToTs())
        val catTotals = db.getTotalByCategory(getFromTs(), getToTs())
        val total     = expenses.sumOf { it.amount }

        tvTotalSpent.text = "$currency${String.format("%.0f", total)}"

        // Pie chart data
        val slices = catTotals.map { (name, amt) ->
            val cat = CategoryManager.getCategoryByName(name)
            PieSlice(
                label = name,
                value = amt.toFloat(),
                color = Color.parseColor(cat?.colorHex ?: "#9CA3AF")
            )
        }
        pieChart.setData(slices, currency)

        // Top 3 legend
        legendContainer.removeAllViews()
        slices.sortedByDescending { it.value }.take(3).forEach { slice ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity     = android.view.Gravity.CENTER_VERTICAL
                setPadding(0, 6, 0, 6)
            }
            val dot = View(this).apply {
                layoutParams = LinearLayout.LayoutParams(10, 10).apply { setMargins(0,0,8,0) }
                setBackgroundColor(slice.color)
            }
            val label = TextView(this).apply {
                text     = "${slice.label}: $currency${String.format("%.0f", slice.value)}"
                textSize = 12f
                setTextColor(Color.parseColor("#6B7280"))
            }
            row.addView(dot); row.addView(label)
            legendContainer.addView(row)
        }

        // Recent transactions
        transactionContainer.removeAllViews()
        if (expenses.isEmpty()) {
            tvNoTxn.visibility = View.VISIBLE
        } else {
            tvNoTxn.visibility = View.GONE
            expenses.take(5).forEach { addTransactionRow(it) }
        }
    }

    private fun addTransactionRow(expense: Expense) {
        val cat = CategoryManager.getCategoryByName(expense.category)
        val sdf = SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault())

        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity     = android.view.Gravity.CENTER_VERTICAL
            setPadding(0, 12, 0, 12)
        }

        // Icon circle
        val iconBg = LinearLayout(this).apply {
            val lp = LinearLayout.LayoutParams(44, 44).apply { setMargins(0,0,12,0) }
            layoutParams = lp; gravity = android.view.Gravity.CENTER
            background = android.graphics.drawable.GradientDrawable().apply {
                shape = android.graphics.drawable.GradientDrawable.OVAL
                setColor(Color.parseColor(cat?.colorHex ?: "#F0F2F5"))
            }
        }
        iconBg.addView(TextView(this).apply {
            text     = getCategoryEmoji(expense.category)
            textSize = 16f
        })

        // Info
        val info = LinearLayout(this).apply {
            orientation  = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        info.addView(TextView(this).apply {
            text     = expense.category
            textSize = 14f
            setTextColor(Color.parseColor("#1A1A2E"))
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        })
        info.addView(TextView(this).apply {
            text     = sdf.format(Date(expense.timestamp))
            textSize = 11f
            setTextColor(Color.parseColor("#9CA3AF"))
        })

        // Amount
        val amt = TextView(this).apply {
            text     = if (expense.amount > 0) "$currency${String.format("%.0f", expense.amount)}" else "$currency-"
            textSize = 15f
            setTextColor(Color.parseColor("#EF4444"))
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }

        row.addView(iconBg); row.addView(info); row.addView(amt)
        transactionContainer.addView(row)

        // Divider
        transactionContainer.addView(View(this).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1)
            setBackgroundColor(Color.parseColor("#F0F0F0"))
        })
    }

    private fun getCategoryEmoji(name: String): String {
        return when (name.lowercase()) {
            "food"              -> "🍽️"
            "tea/coffee"        -> "☕"
            "fuel"              -> "⛽"
            "shopping"          -> "🛍️"
            "transport"         -> "🚌"
            "grocery"           -> "🛒"
            "medicine"          -> "💊"
            "movies"            -> "🎬"
            "ott"               -> "📺"
            "snacks"            -> "🍪"
            "mutual funds"      -> "📈"
            "loan emi"          -> "💳"
            "online order"      -> "📦"
            "personal grooming" -> "✂️"
            "internet"          -> "📶"
            "electricity"       -> "⚡"
            "gas"               -> "🔥"
            "house rent"        -> "🏠"
            "insurance premium" -> "🛡️"
            else                -> "💰"
        }
    }

    private fun setupSeeAllButtons() {
        findViewById<TextView>(R.id.btnSeeAllChart).setOnClickListener {
            startActivity(Intent(this, ExpenseBreakdownActivity::class.java).apply {
                putExtra("filter", currentFilter)
                putExtra("from", customFrom)
                putExtra("to", customTo)
            })
        }
        findViewById<TextView>(R.id.btnSeeAllTxn).setOnClickListener {
            startActivity(Intent(this, TransactionsActivity::class.java).apply {
                putExtra("entry", "stack")
            })
        }
    }

    private fun setupBottomNav() {
        setActiveNav(R.id.navHomeIcon, R.id.navHomeLabel)
        findViewById<LinearLayout>(R.id.navHome).setOnClickListener { /* already here */ }
        findViewById<LinearLayout>(R.id.navTransactions).setOnClickListener {
            startActivity(Intent(this, TransactionsActivity::class.java))
        }
        findViewById<LinearLayout>(R.id.navCategory).setOnClickListener {
            startActivity(Intent(this, ManageCategoriesActivity::class.java))
        }
        findViewById<LinearLayout>(R.id.navSettings).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
    }

    private fun setActiveNav(iconId: Int, labelId: Int) {
        listOf(
            R.id.navHomeIcon to R.id.navHomeLabel,
            R.id.navTransactionsIcon to R.id.navTransactionsLabel,
            R.id.navCategoryIcon to R.id.navCategoryLabel,
            R.id.navSettingsIcon to R.id.navSettingsLabel
        ).forEach { (ic, lb) ->
            val isActive = ic == iconId
            findViewById<ImageView>(ic).alpha  = if (isActive) 1f else 0.5f
            findViewById<TextView>(lb).setTextColor(
                if (isActive) Color.parseColor("#2D6A4F")
                else          Color.parseColor("#9CA3AF")
            )
        }
    }

    private fun isNotifGranted(): Boolean {
        val flat = Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
        return flat?.contains(packageName) == true
    }

    private fun isOverlayGranted() = Settings.canDrawOverlays(this)
}
