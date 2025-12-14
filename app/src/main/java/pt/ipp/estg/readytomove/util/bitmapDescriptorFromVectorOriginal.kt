package pt.ipp.estg.readytomove.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.core.content.ContextCompat
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory

/**
 * Converts a vector drawable resource into a [BitmapDescriptor] for use with Google Maps markers.
 *
 * This function uses the original intrinsic dimensions of the drawable without any scaling or tinting.
 *
 * @param context The context used to retrieve the drawable resources.
 * @param vectorResId The resource ID of the vector drawable.
 * @return A [BitmapDescriptor] containing the bitmap representation of the vector, or null if the resource is invalid.
 */
fun bitmapDescriptorFromVectorOriginal(context: Context, vectorResId: Int): BitmapDescriptor? {
    return ContextCompat.getDrawable(context, vectorResId)?.run {
        // Set bounds to the intrinsic size of the vector drawable
        setBounds(0, 0, intrinsicWidth, intrinsicHeight)

        // Create a bitmap with the same dimensions
        val bitmap = Bitmap.createBitmap(intrinsicWidth, intrinsicHeight, Bitmap.Config.ARGB_8888)

        // Draw the vector onto the bitmap
        draw(Canvas(bitmap))

        BitmapDescriptorFactory.fromBitmap(bitmap)
    }
}