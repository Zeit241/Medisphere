package com.example.kursovaya.model

import androidx.annotation.DrawableRes

data class MenuItem(
    @DrawableRes val iconRes: Int,
    val label: String,
    val badgeCount: Int? = null,
    val action: () -> Unit,
)
