package com.example.voidshop

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.runtime.*
import com.example.voidshop.ui.theme.VoidShopTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        //aca ta la inf que mediante el splash se logro crear la imagen al inicio pol favor hagan un save cualquier vaina que estuvo largo el proceso 😔
        installSplashScreen()
        super.onCreate(savedInstanceState)
        setContent {
            VoidShopTheme {
                var showSplash by remember { mutableStateOf(true) }
                if (showSplash) {
                    SplashScreen(
                        onTimeout = { showSplash = false },
                        durationMillis = 1200L //esto es lo que va durar la pantalla de carga mas o menos ya que es preestablecido ustede saben tiren prueba cualquier vaian
                    )
                } else {
                    RootApp()
                }
            }
        }
    }
}
