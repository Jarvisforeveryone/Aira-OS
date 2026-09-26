package com.example.data

import com.example.R

data class ThemeOption(
    val name: String,
    val colorResId: Int,
    val description: String,
    val isDark: Boolean
)

object ThemeRepository {
    val themes = listOf(
        ThemeOption(
            name = "Premium Blue",
            colorResId = R.color.aira_primary,
            description = "The flagship elegant cobalt blue theme. Clean, bright, and modern.",
            isDark = false
        ),
        ThemeOption(
            name = "Stripe Blue",
            colorResId = R.color.aira_primary_variant,
            description = "A deep and professional corporate brand blue.",
            isDark = false
        ),
        ThemeOption(
            name = "Aether Focus",
            colorResId = R.color.aira_primary_variant_dark,
            description = "A deep night theme featuring a bright, beautiful sky blue offering refined contrast.",
            isDark = true
        )
    )
}
