package com.example.sandbox_yandexmapkit

import android.content.Context
import android.graphics.PointF
import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.graphics.ColorUtils
import com.yandex.mapkit.GeoObject
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.layers.GeoObjectTapEvent
import com.yandex.mapkit.layers.GeoObjectTapListener
import com.yandex.mapkit.map.CameraListener
import com.yandex.mapkit.map.CameraPosition
import com.yandex.mapkit.map.ClusterListener
import com.yandex.mapkit.map.ClusterTapListener
import com.yandex.mapkit.map.GeoObjectSelectionMetadata
import com.yandex.mapkit.map.IconStyle
import com.yandex.mapkit.map.MapObject
import com.yandex.mapkit.map.MapObjectTapListener
import com.yandex.mapkit.mapview.MapView
import com.yandex.runtime.image.ImageProvider
import kotlin.math.roundToInt


private val initialPosition = Point(49.9476, 82.5891)
private val demoPoints = listOf(
    initialPosition,
    Point(49.9488, 82.5851),
    Point(49.9457, 82.5874),
    Point(49.9521, 82.5993),
    Point(49.9469, 82.5903),
    Point(49.9525, 82.5902),
)

private val EmptyCameraPosition = CameraPosition()

private data class State(
    val newCameraPosition: CameraPosition,
    val currentCameraPosition: CameraPosition,
    val points: Collection<Point>,
    val selection: GeoObjectSelectionMetadata?,
)

@Composable
fun ComposeMap(onClick: () -> Unit) {
    val context = LocalContext.current
    val imageProvider by remember { mutableStateOf(ImageProvider.fromResource(context, R.drawable.ic_mark)) }

    var initialized by remember { mutableStateOf(false) }
    var state by remember {
        mutableStateOf(
            State(
                newCameraPosition = CameraPosition(initialPosition, 16f, 0f, 0f),
                currentCameraPosition = EmptyCameraPosition,
                points = demoPoints,
                selection = null,
            )
        )
    }

    fun setNewCamera(target: Point, zoom: Float) {
        state = state.copy(
            newCameraPosition = CameraPosition(
                /* target = */ target,
                /* zoom = */ zoom,
                /* azimuth = */ state.currentCameraPosition.azimuth,
                /* tilt = */ state.currentCameraPosition.tilt,
            )
        )
    }

    val onObjectTap: GeoObjectTapListener by remember {
        mutableStateOf(GeoObjectTapListener { event ->
            Log.d("DBG:tap:obj", event.geoObject.debug())
            state = state.copy(selection = event.geoObject.metadataContainer.getItem(GeoObjectSelectionMetadata::class.java))
            true // consume event
        })
    }
    val placeMarkTapListener by remember {
        mutableStateOf(MapObjectTapListener { obj: MapObject, point: Point ->
            Log.d("DBG:tap:mark", "Tapped the mark (${point.debug()}) «${obj.userData}»")
            true // consume event
        })
    }
    val cameraListener by remember {
        mutableStateOf(CameraListener { _, cameraPosition, reason, moveFinished ->
            Log.d("DBG:camera", "camera: ${cameraPosition.debug()} $reason ${if (moveFinished) "finished" else ""}")
            // плохо, происходит рекомпозиция. Через вью модель будет норм, думаю
            state = state.copy(currentCameraPosition = cameraPosition)
        })
    }
    val clusterTapListener by remember {
        mutableStateOf(ClusterTapListener { cluster ->
            setNewCamera(cluster.appearance.geometry, (state.currentCameraPosition.zoom + 1).roundToInt().toFloat())
            true
        })
    }

    val clusterListener by remember {
        mutableStateOf(ClusterListener { // on cluster added
            Log.d("DBG:cluster", "add cluster: $it")
            it.appearance.setIcon(TintedImageProvider(imageProvider, Color.Black))
            it.appearance.setIconStyle(IconStyle().apply { anchor = PointF(0.25f, 1f); scale = 0.5f })
            it.addClusterTapListener(clusterTapListener)
        })
    }

    Column {
        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                Log.d("DBG:tap:zoomOut", "click")
                setNewCamera(state.currentCameraPosition.target, state.currentCameraPosition.zoom - 1)
            },
        ) {
            Text(text = "« — »")
        }

        AndroidView(
            modifier = Modifier.fillMaxSize(), // Occupy the max size in the Compose UI tree
            factory = { context ->
                // Creates view
                createMapView(context, imageProvider, onObjectTap, cameraListener, clusterListener, placeMarkTapListener)
            },
            update = { view ->
                // init
                if (!initialized) {
                    view.onStart() // fixme надо onStop как-то вызвать
                    initialized = true
                }

                // selection
                val selection = state.selection
                if (selection != null) {
                    view.mapWindow.map.selectGeoObject(selection)
                } else {
                    view.mapWindow.map.deselectGeoObject()
                }

                // camera
                val newPosition = state.newCameraPosition
                if (newPosition != EmptyCameraPosition) {
                    Log.d("DBG:camera:move", "camera ${view.mapWindow.map.cameraPosition.debug()} ↣ $newPosition")
                    view.mapWindow.map.move(newPosition)
                    state = state.copy(newCameraPosition = EmptyCameraPosition)
                }
            }
        )
    }
}

private fun createMapView(
    context: Context,
    imageProvider: ImageProvider,
    onObjectTap: GeoObjectTapListener,
    cameraListener: CameraListener,
    clusterListener: ClusterListener,
    placeMarkTapListener: MapObjectTapListener,
): MapView =
    MapView(context)
        .apply {
            mapWindow.map.addTapListener(onObjectTap)
            mapWindow.map.addCameraListener(cameraListener)

            val from = Color.Blue.toArgb()
            val to = Color.Red.toArgb()
            Log.d(
                "DBG:mark:create",
                "colors: ${from.toUInt().toString(0x10)} => ${to.toUInt().toString(0x10)}"
            )

            val collection = mapWindow.map.mapObjects.addCollection()
            val clusterizedCollection = collection.addClusterizedPlacemarkCollection(clusterListener)
            clusterizedCollection
                .addEmptyPlacemarks(demoPoints)
                .forEachIndexed { i, p ->
                    val color = Color(ColorUtils.blendARGB(from, to, i / (demoPoints.size - 1.0f)))
                    p.setIcon(TintedImageProvider(imageProvider, color))
                    p.setIconStyle(IconStyle().apply { anchor = PointF(0.25f, 1.0f); scale=0.25f })
                    p.userData = "point #$i, #${color.toArgb().toUInt().toString(16)}"
                    p.addTapListener(placeMarkTapListener)
                    Log.d("DBG:mark:create", "#$i: ${p.geometry.debug()} ${color.toArgb().toUInt().toString(0x10)}")
                }
            clusterizedCollection.clusterPlacemarks(/* cluster radius */ 30.0, 15 /* min zoom */)
        }

private fun Point.debug(): String = "P[%.4f, %.4f]".format(latitude, longitude)

private fun CameraPosition.debug(): String = "CP(${target.debug()} z:$zoom a:$azimuth t:$tilt)"

private fun GeoObjectTapEvent.debug(): String = "${if (!isValid) "INVALID " else ""}${geoObject.debug()}"

private fun GeoObject.debug(): String {
    val sb = StringBuilder()
    sb.append("«$name» ")
    if (aref.isNotEmpty()) sb.append("aref:$aref ")
    if (attributionMap.isNotEmpty()) sb.append("attrs:$attributionMap ")
    if (metadataContainer.allItems.isNotEmpty()) sb.append("meta:${metadataContainer.allItems} ")
    return sb.toString()
}

@Preview
@Composable
private fun Preview() = ComposeMap {}
