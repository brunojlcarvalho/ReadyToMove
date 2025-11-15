package pt.ipp.estg.readytomove.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory

fun bitmapDescriptorFromVector(
    context: Context,
    @DrawableRes vectorResId: Int,
    @ColorInt tintColor: Int,
    scaleFactor: Float = 1.0f
): BitmapDescriptor? {
    return ContextCompat.getDrawable(context, vectorResId)?.let { drawable ->
        DrawableCompat.setTint(drawable, tintColor)

        val originalWidth = drawable.intrinsicWidth
        val originalHeight = drawable.intrinsicHeight
        val scaledWidth = (originalWidth * scaleFactor).toInt()
        val scaledHeight = (originalHeight * scaleFactor).toInt()

        drawable.setBounds(0, 0, scaledWidth, scaledHeight)

        val bitmap = Bitmap.createBitmap(scaledWidth, scaledHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.draw(canvas)

        BitmapDescriptorFactory.fromBitmap(bitmap)
    }
}
