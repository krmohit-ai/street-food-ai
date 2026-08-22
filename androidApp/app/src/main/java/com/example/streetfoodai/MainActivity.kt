package com.example.streetfoodai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.streetfoodai.ui.navigation.AppNavigation
import com.example.streetfoodai.ui.theme.StreetFoodAITheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            StreetFoodAITheme {
                AppNavigation()
            }
        }
    }
}
