package com.example.data.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.ui.theme.CategoryBillsColor
import com.example.ui.theme.CategoryEducationColor
import com.example.ui.theme.CategoryEntertainmentColor
import com.example.ui.theme.CategoryFoodColor
import com.example.ui.theme.CategoryHealthColor
import com.example.ui.theme.CategoryOtherColor
import com.example.ui.theme.CategoryShoppingColor
import com.example.ui.theme.CategoryTravelColor

enum class ExpenseCategory(
    val displayName: String,
    val icon: ImageVector,
    val color: Color
) {
    FOOD("Food", Icons.Filled.Fastfood, CategoryFoodColor),
    TRAVEL("Travel", Icons.Filled.DirectionsCar, CategoryTravelColor),
    EDUCATION("Education", Icons.Filled.School, CategoryEducationColor),
    SHOPPING("Shopping", Icons.Filled.ShoppingBag, CategoryShoppingColor),
    ENTERTAINMENT("Entertainment", Icons.Filled.Movie, CategoryEntertainmentColor),
    BILLS("Bills", Icons.Filled.AccountBalance, CategoryBillsColor),
    HEALTH("Health", Icons.Filled.LocalHospital, CategoryHealthColor),
    OTHER("Other", Icons.Filled.MoreHoriz, CategoryOtherColor);

    companion object {
        fun fromString(value: String): ExpenseCategory {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) || it.displayName.equals(value, ignoreCase = true) }
                ?: OTHER
        }
    }
}
