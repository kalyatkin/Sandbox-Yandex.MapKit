package com.example.sandbox_yandexmapkit

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.yandex.runtime.image.ImageProvider

class TintedImageProvider(imageProvider: ImageProvider, tintColor: Color) : ImageProvider(true) {
    private val id = imageProvider.id + tintColor
    private val bitmap = imageProvider.image.tint(tintColor)
    override fun getId(): String = id
    override fun getImage(): Bitmap = bitmap
}

fun Bitmap.tint(color: Color): Bitmap {
    val paint = Paint().apply { colorFilter = PorterDuffColorFilter(color.toArgb(), PorterDuff.Mode.SRC_IN) }
    val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(result)
    canvas.drawBitmap(this, 0f, 0f, paint)
    return result
}