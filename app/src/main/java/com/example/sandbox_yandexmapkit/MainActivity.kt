package com.example.sandbox_yandexmapkit

import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.sandbox_yandexmapkit.ui.theme.SandboxYandexMapKitTheme
import com.yandex.mapkit.MapKitFactory

typealias OnClickListener = (View) -> Unit

class MainActivity : ComponentActivity() {

    init {
        MapKitFactory.setApiKey("d3d6e2a5-2283-4cb3-8995-e9d6b96476c8")
    }

// todo?   private lateinit var mapView: MapView


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (checkSelfPermission("android.permission.ACCESS_FINE_LOCATION") != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf("android.permission.ACCESS_FINE_LOCATION"), 123)
        }

        MapKitFactory.initialize(this)
//        mapView = MapView(this)

        enableEdgeToEdge()
        setContent {
            SandboxYandexMapKitTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    var count by remember { mutableIntStateOf(0) }

                    Column(
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        Greeting(name = "Android", count)
                        ComposeMap { count++ }
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        MapKitFactory.getInstance().onStart()
// TODO        mapView.onStart()
    }

    override fun onStop() {
// TODO       mapView.onStop()
        MapKitFactory.getInstance().onStop()
        super.onStop()
    }
}

@Composable
fun Greeting(name: String, count: Int, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name! $count times clicked 😒",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    SandboxYandexMapKitTheme {
        Greeting("Android", 0)
    }
}
