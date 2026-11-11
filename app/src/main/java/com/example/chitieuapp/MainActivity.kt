package com.example.chitieuapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.chitieuapp.ui.navigation.ChiTieuApp
import com.example.chitieuapp.ui.theme.ChiTieuAppTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ChiTieuAppTheme {
                ChiTieuApp()
            }
        }
    }
}
