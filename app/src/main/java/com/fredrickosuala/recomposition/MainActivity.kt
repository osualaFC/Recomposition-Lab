package com.fredrickosuala.recomposition

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.rememberNavController
import com.fredrickosuala.recomposition.navigation.NavGraph
import com.fredrickosuala.recomposition.ui.theme.RecompositionTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RecompositionTheme {
                val navController = rememberNavController()
                NavGraph(navController = navController)
            }
        }
    }
}
