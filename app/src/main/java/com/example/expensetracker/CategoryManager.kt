package com.example.expensetracker

import android.content.Context
import android.graphics.Color

data class Category(
    val id        : String,
    val name      : String,
    val iconName  : String,
    val colorHex  : String,
    var isActive  : Boolean = false,
    var sortOrder : Int = 0
) {
    fun color() = Color.parseColor(colorHex)
}

object CategoryManager {

    // Master catalog — 19 categories
    val CATALOG = listOf(
        Category("cat_01", "House Rent",          "home",                 "#93C5FD"),
        Category("cat_02", "Internet",             "wifi",                 "#7DD3FC"),
        Category("cat_03", "Insurance Premium",    "verified_user",        "#A5B4FC"),
        Category("cat_04", "Electricity",          "bolt",                 "#C7D2FE"),
        Category("cat_05", "Gas",                  "local_fire_department","#BAE6FD"),
        Category("cat_06", "Food",                 "restaurant",           "#FDBA74"),
        Category("cat_07", "Tea/Coffee",           "coffee",               "#CAA47E"),
        Category("cat_08", "Snacks",               "bakery_dining",        "#FDE047"),
        Category("cat_09", "Grocery",              "shopping_cart",        "#FED7AA"),
        Category("cat_10", "Medicine",             "medical_services",     "#FCA5A5"),
        Category("cat_11", "Mutual Funds",         "trending_up",          "#86EFAC"),
        Category("cat_12", "Loan EMI",             "credit_card",          "#A7F3D0"),
        Category("cat_13", "Shopping",             "shopping_bag",         "#D6BBFA"),
        Category("cat_14", "Online Order",         "local_shipping",       "#F472B6"),
        Category("cat_15", "Movies",               "local_movies",         "#E9D5FF"),
        Category("cat_16", "OTT",                  "live_tv",              "#FBCFE8"),
        Category("cat_17", "Personal Grooming",    "content_cut",          "#FDA4AF"),
        Category("cat_18", "Fuel",                 "local_gas_station",    "#5EEAD4"),
        Category("cat_19", "Transport",            "commute",              "#67E8F9")
    )

    // Default active on first launch (top 6 in order)
    val DEFAULT_ACTIVE_IDS = listOf(
        "cat_06", // Food
        "cat_07", // Tea/Coffee
        "cat_18", // Fuel
        "cat_13", // Shopping
        "cat_19", // Transport
        "cat_09"  // Grocery
    )

    // In-memory state (populated from DB or defaults)
    private val _activeCategories   = mutableListOf<Category>()
    private val _archivedCategories = mutableListOf<Category>()

    val activeCategories   get() = _activeCategories.toList()
    val archivedCategories get() = _archivedCategories.toList()

    fun initialize(context: Context) {
        val db = CategoryDbHelper(context)
        val stored = db.getAllCategories()
        if (stored.isEmpty()) {
            // First launch — set defaults
            CATALOG.forEach { cat ->
                val isActive  = cat.id in DEFAULT_ACTIVE_IDS
                val sortOrder = if (isActive) DEFAULT_ACTIVE_IDS.indexOf(cat.id) else 0
                val c = cat.copy(isActive = isActive, sortOrder = sortOrder)
                db.insertCategory(c)
                if (isActive) _activeCategories.add(c)
                else          _archivedCategories.add(c)
            }
            _activeCategories.sortBy { it.sortOrder }
        } else {
            stored.forEach { cat ->
                if (cat.isActive) _activeCategories.add(cat)
                else              _archivedCategories.add(cat)
            }
            _activeCategories.sortBy { it.sortOrder }
        }
    }

    fun getCategoryById(id: String) =
        (_activeCategories + _archivedCategories).find { it.id == id }

    fun getCategoryByName(name: String) =
        (_activeCategories + _archivedCategories).find {
            it.name.equals(name, ignoreCase = true)
        }

    fun archiveCategory(context: Context, id: String) {
        val cat = _activeCategories.find { it.id == id } ?: return
        _activeCategories.remove(cat)
        val archived = cat.copy(isActive = false)
        _archivedCategories.add(archived)
        CategoryDbHelper(context).updateCategoryActive(id, false)
        reorderActive(context)
    }

    fun activateCategory(context: Context, id: String) {
        val cat = _archivedCategories.find { it.id == id } ?: return
        _archivedCategories.remove(cat)
        val active = cat.copy(isActive = true, sortOrder = _activeCategories.size)
        _activeCategories.add(active)
        CategoryDbHelper(context).updateCategoryActive(id, true)
    }

    fun deleteCategory(context: Context, id: String) {
        _activeCategories.removeAll  { it.id == id }
        _archivedCategories.removeAll { it.id == id }
        CategoryDbHelper(context).deleteCategory(id)
    }

    fun renameCategory(context: Context, id: String, newName: String) {
        val activeIdx = _activeCategories.indexOfFirst { it.id == id }
        if (activeIdx >= 0)
            _activeCategories[activeIdx] = _activeCategories[activeIdx].copy(name = newName)
        else {
            val archIdx = _archivedCategories.indexOfFirst { it.id == id }
            if (archIdx >= 0)
                _archivedCategories[archIdx] = _archivedCategories[archIdx].copy(name = newName)
        }
        CategoryDbHelper(context).renameCategory(id, newName)
    }

    fun reorderActive(context: Context) {
        _activeCategories.forEachIndexed { i, cat ->
            _activeCategories[i] = cat.copy(sortOrder = i)
            CategoryDbHelper(context).updateSortOrder(cat.id, i)
        }
    }

    fun getIconDrawableId(iconName: String): Int {
        return when (iconName) {
            "home"                  -> R.drawable.ic_nav_home
            "wifi"                  -> R.drawable.ic_nav_home
            "verified_user"         -> R.drawable.ic_lock
            "bolt"                  -> R.drawable.ic_nav_home
            "local_fire_department" -> R.drawable.ic_nav_home
            "restaurant"            -> R.drawable.ic_nav_home
            "coffee"                -> R.drawable.ic_nav_home
            "bakery_dining"         -> R.drawable.ic_nav_home
            "shopping_cart"         -> R.drawable.ic_nav_home
            "medical_services"      -> R.drawable.ic_nav_home
            "trending_up"           -> R.drawable.ic_nav_home
            "credit_card"           -> R.drawable.ic_nav_home
            "shopping_bag"          -> R.drawable.ic_nav_home
            "local_shipping"        -> R.drawable.ic_nav_home
            "local_movies"          -> R.drawable.ic_nav_home
            "live_tv"               -> R.drawable.ic_nav_home
            "content_cut"           -> R.drawable.ic_nav_home
            "local_gas_station"     -> R.drawable.ic_nav_home
            "commute"               -> R.drawable.ic_nav_home
            else                    -> R.drawable.ic_nav_home
        }
    }
}
