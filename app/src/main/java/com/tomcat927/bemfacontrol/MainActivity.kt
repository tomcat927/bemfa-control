package com.tomcat927.bemfacontrol

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tomcat927.bemfacontrol.ui.DevicesScreen
import com.tomcat927.bemfacontrol.ui.DevicesViewModel
import com.tomcat927.bemfacontrol.ui.theme.BemfaControlTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as BemfaControlApp).container

        setContent {
            BemfaControlTheme {
                val viewModel: DevicesViewModel = viewModel(
                    factory = DevicesViewModel.Factory(
                        settingsStore = container.settingsStore,
                        outletRepository = container.outletRepository,
                    ),
                )
                DevicesScreen(viewModel = viewModel)
            }
        }
    }
}
