package com.example.expensetracker

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class ManageCategoriesActivity : AppCompatActivity() {

    private lateinit var listContainer  : LinearLayout
    private lateinit var previewCollapsed: LinearLayout
    private var isPreviewExpanded = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manage_categories)

        listContainer   = findViewById(R.id.categoryListContainer)
        previewCollapsed= findViewById(R.id.previewCollapsed)

        CategoryManager.initialize(this)

        findViewById<ImageView>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<ImageView>(R.id.btnArchive).setOnClickListener {
            startActivity(Intent(this, ArchivedCategoriesActivity::class.java))
        }
        findViewById<TextView>(R.id.btnAddNew).setOnClickListener { showAddCategoryDialog() }

        refreshUI()
    }

    override fun onResume() { super.onResume(); refreshUI() }

    private fun refreshUI() {
        buildPreview()
        buildCategoryList()
    }

    private fun buildPreview() {
        previewCollapsed.removeAllViews()
        val active = CategoryManager.activeCategories
        val top4   = active.take(4)

        top4.forEach { cat ->
            val chip = TextView(this).apply {
                text = "${getCategoryEmoji(cat.name)} ${cat.name}"
                textSize = 12f
                setTextColor(Color.parseColor("#1A1A2E"))
                background = getDrawable(R.drawable.bg_category_chip)
                setPadding(16, 8, 16, 8)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { setMargins(0, 0, 8, 0) }
            }
            previewCollapsed.addView(chip)
        }

        // + button
        val plusBtn = TextView(this).apply {
            text = "+"
            textSize = 18f
            setTextColor(Color.parseColor("#2D6A4F"))
            background = getDrawable(R.drawable.bg_category_chip)
            setPadding(16, 8, 16, 8)
            gravity = android.view.Gravity.CENTER
        }
        plusBtn.setOnClickListener {
            isPreviewExpanded = !isPreviewExpanded
            // Simple toggle — show/hide hint for now
            Toast.makeText(this, if (isPreviewExpanded) "All ${CategoryManager.activeCategories.size} categories" else "Showing top 4", Toast.LENGTH_SHORT).show()
        }
        previewCollapsed.addView(plusBtn)
    }

    private fun buildCategoryList() {
        listContainer.removeAllViews()
        CategoryManager.activeCategories.forEachIndexed { index, cat ->
            val row = layoutInflater.inflate(R.layout.item_category_row, null)
            val isTop4 = index < 4

            row.background = getDrawable(
                if (isTop4) R.drawable.bg_top4_highlight else R.drawable.bg_normal_row
            )

            // Icon
            val iconBg = row.findViewById<LinearLayout>(R.id.iconContainer)
            iconBg.background = android.graphics.drawable.GradientDrawable().apply {
                shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                cornerRadius = 12f
                setColor(Color.parseColor(cat.colorHex))
            }
            row.findViewById<TextView>(R.id.tvCategoryName).text = cat.name

            // Actions
            row.findViewById<ImageView>(R.id.btnMinus).setOnClickListener {
                CategoryManager.archiveCategory(this, cat.id)
                refreshUI()
                Toast.makeText(this, "${cat.name} archived", Toast.LENGTH_SHORT).show()
            }

            row.findViewById<ImageView>(R.id.btnEdit).setOnClickListener {
                showRenameDialog(cat.id, cat.name)
            }

            row.findViewById<ImageView>(R.id.btnDelete).setOnClickListener {
                showDeleteConfirmation(cat.id, cat.name)
            }

            row.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 0, 0, 8) }

            listContainer.addView(row)
        }
    }

    private fun showDeleteConfirmation(id: String, name: String) {
        AlertDialog.Builder(this)
            .setTitle("Delete Category")
            .setMessage(getString(R.string.delete_confirmation))
            .setPositiveButton("Delete") { _, _ ->
                CategoryManager.deleteCategory(this, id)
                refreshUI()
                Toast.makeText(this, "$name deleted", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .create().apply {
                setOnShowListener {
                    getButton(AlertDialog.BUTTON_POSITIVE)
                        .setTextColor(Color.parseColor("#EF4444"))
                }
            }.show()
    }

    private fun showRenameDialog(id: String, currentName: String) {
        val input = EditText(this).apply {
            setText(currentName); selectAll()
            setPadding(32, 16, 32, 16)
        }
        AlertDialog.Builder(this)
            .setTitle("Rename Category")
            .setView(input)
            .setPositiveButton("Save") { _, _ ->
                val newName = input.text.toString().trim()
                if (newName.isNotEmpty()) {
                    CategoryManager.renameCategory(this, id, newName)
                    refreshUI()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showAddCategoryDialog() {
        val input = EditText(this).apply {
            hint = "Category Name"
            setPadding(32, 16, 32, 16)
        }
        AlertDialog.Builder(this)
            .setTitle("Add New Category")
            .setView(input)
            .setPositiveButton("Add") { _, _ ->
                val name = input.text.toString().trim()
                if (name.isNotEmpty()) {
                    val newCat = Category(
                        id        = "custom_${System.currentTimeMillis()}",
                        name      = name,
                        iconName  = "restaurant",
                        colorHex  = "#FDBA74",
                        isActive  = true,
                        sortOrder = CategoryManager.activeCategories.size
                    )
                    CategoryDbHelper(this).insertCategory(newCat)
                    CategoryManager.initialize(this)
                    refreshUI()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun getCategoryEmoji(name: String) = when (name.lowercase()) {
        "food" -> "🍽️"; "tea/coffee" -> "☕"; "fuel" -> "⛽"
        "shopping" -> "🛍️"; "transport" -> "🚌"; "grocery" -> "🛒"
        else -> "💰"
    }
}
