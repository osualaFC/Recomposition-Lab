package com.fredrickosuala.recomposition.navigation

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Tooling : Screen("tooling")
    data class LabDetail(val labId: String) : Screen("lab/$labId") {
        companion object {
            const val ROUTE = "lab/{labId}"
        }
    }
}
