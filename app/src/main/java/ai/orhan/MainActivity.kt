package ai.orhan

import ai.orhan.design.screens.OrhanAppSample
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge

/** Entry point: Home (chat list) <-> Chat, with the kit's theme toggle. */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { OrhanAppSample() }
    }
}
