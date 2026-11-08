package de.goepfarth.kalenderassistent

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import de.goepfarth.kalenderassistent.ui.KalenderTheme
import de.goepfarth.kalenderassistent.ui.TestScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            KalenderTheme {
                TestScreen()
            }
        }
    }
}
