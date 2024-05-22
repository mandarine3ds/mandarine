package org.citra.citra_emu.view

import android.content.Context
import android.content.res.Configuration
import android.util.AttributeSet
import android.util.Rational
import android.view.SurfaceView

class SurfaceView @JvmOverloads constructor(
    context: Context, 
    attrs: AttributeSet? = null, 
    defStyleAttr: Int = 0
) : SurfaceView(context, attrs, defStyleAttr) {

    private var desiredWidth = 1280
    private var desiredHeight = 720
    
    fun setDimensions(width: Int, height: Int, configuration: Configuration) {
        if (configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            desiredWidth = width
            desiredHeight = height
            requestLayout()
            holder.setFixedSize(width, height)
        } else {
            holder.setSizeFromLayout() // Default layout aspect ratio 
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        // Calculate width and height based on desired width and height, and the mode
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val height = MeasureSpec.getSize(heightMeasureSpec)

        val finalWidth = resolveSize(desiredWidth, widthMeasureSpec)
        val finalHeight = resolveSize(desiredHeight, heightMeasureSpec)

        // Set the measured dimensions
        setMeasuredDimension(finalWidth, finalHeight)
    }
}
