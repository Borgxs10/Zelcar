package com.zelcar.app

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.zelcar.app.ui.ZelcarRoot
import com.zelcar.app.ui.ZelcarViewModel
import com.zelcar.app.ui.theme.ZelcarTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val vm: ZelcarViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val dark = isSystemInDarkTheme()
            DisposableEffect(dark) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { dark },
                    navigationBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { dark },
                )
                onDispose { }
            }
            ZelcarTheme(dark = dark) { ZelcarRoot(vm) }
        }
        // O fluxo de compra precisa de uma Activity; o ViewModel só pede.
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                vm.purchaseRequests.collect { annual ->
                    (application as ZelcarApp).billing.launch(this@MainActivity, annual)
                }
            }
        }
    }
}
