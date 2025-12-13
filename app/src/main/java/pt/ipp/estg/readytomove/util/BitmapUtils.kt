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

/**
 * Converts a vector drawable resource into a [BitmapDescriptor] with support for tinting and scaling.
 *
 * This is useful for creating map markers that need to change color dynamically (e.g., based on theme)
 * or need to be resized relative to their original vector definition.
 *
 * @param context The context used to retrieve the drawable resources.
 * @param vectorResId The resource ID of the vector drawable.
 * @param tintColor The color to tint the drawable.
 * @param scaleFactor The factor by which to scale the drawable (default is 1.0f).
 * @return A [BitmapDescriptor] containing the modified bitmap, or null if the resource is invalid.
 */
fun bitmapDescriptorFromVector(
    context: Context,
    @DrawableRes vectorResId: Int,
    @ColorInt tintColor: Int,
    scaleFactor: Float = 1.0f
): BitmapDescriptor? {
    return ContextCompat.getDrawable(context, vectorResId)?.let { drawable ->
        // Apply the requested tint color to the drawable
        DrawableCompat.setTint(drawable, tintColor)

        // Calculate new dimensions based on the scale factor
        val originalWidth = drawable.intrinsicWidth
        val originalHeight = drawable.intrinsicHeight
        val scaledWidth = (originalWidth * scaleFactor).toInt()
        val scaledHeight = (originalHeight * scaleFactor).toInt()

        drawable.setBounds(0, 0, scaledWidth, scaledHeight)

        // Create the bitmap with the scaled dimensions
        val bitmap = Bitmap.createBitmap(scaledWidth, scaledHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.draw(canvas)

        BitmapDescriptorFactory.fromBitmap(bitmap)
    }
}