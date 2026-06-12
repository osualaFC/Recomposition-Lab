package com.fredrickosuala.recomposition.model

import androidx.compose.runtime.Composable

// Regular class, not data class — @Composable lambdas are not comparable
class Lab(
    val id: String,
    val title: String,
    val category: Category,
    val difficulty: Difficulty,
    val description: String,
    val problemStatement: String,
    val howToDetect: String,
    val theFix: String,
    val content: @Composable (optimized: Boolean) -> Unit,
)
