package com.misidoro.app_savemetric

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.misidoro.app_savemetric.ui.login.LoginScreen
import com.misidoro.app_savemetric.ui.theme.App_savemetricTheme
import android.preference.PreferenceManager

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val registerMessage = intent.getStringExtra("register_message")
        PreferenceManager.getDefaultSharedPreferences(this).edit().clear().apply()
        setContent {
            App_savemetricTheme {
                LoginScreen(
                    registerMessage = registerMessage,
                    onLoginSuccess = { token ->
                        val intent = Intent(this@MainActivity, InicioActivity::class.java)
                        startActivity(intent)
                    },
                    onRegisterClick = {
                        val intent = Intent(this@MainActivity, RegistroActivity::class.java)
                        startActivity(intent)
                    }
                )
            }
        }
    }
}