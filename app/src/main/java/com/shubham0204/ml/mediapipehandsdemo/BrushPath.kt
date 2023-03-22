package com.shubham0204.ml.mediapipehandsdemo

import android.util.Log
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import kotlin.math.pow
import kotlin.math.sqrt

// BrushPath -> A wrapper class around androidx.compose.ui.graphics.Path
// to provide quadratic curve approximation and eliminate drawing noise
class BrushPath {

    private var prevPosX = 0
    private var prevPosY = 0

    // Threshold which determines whether a curve should be drawn from ( prevPosX , prevPosY ) to ( x , y )
    // Smaller the value, greater is the freedom to draw intricate strokes
    private val distanceThreshold = 10.0f

    var path = Path()
    var pathColor = Color.Blue


    fun start( x : Int , y : Int ) {
        prevPosX = x
        prevPosY = y
        path.moveTo( x.toFloat() , y.toFloat() )
    }

    fun addPoint( x : Int , y : Int ) {
        val distance = sqrt( ( x - prevPosX ).toFloat().pow(2) + ( y - prevPosY ).toFloat().pow(2) )
        Log.e( "Distance" , "Distance: " + distance )
        // Check if distance from previous point is greater than a predefined threshold
        // It asserts that random fluctuations in MediaPipe predictions are not drawn on
        // the screen - eliminate jitterness
        if ( distance > distanceThreshold ) {
            val midX = ( prevPosX + x ) / 2
            val midY = ( prevPosY + y ) / 2
            // Perform Bezier interpolation to achieve smoother curves
            path.quadraticBezierTo( prevPosX.toFloat(), prevPosY.toFloat(), midX.toFloat(), midY.toFloat())
            prevPosX = x
            prevPosY = y
        }
    }

    fun reset() {
        path = Path()
        prevPosX = 0
        prevPosY = 0
    }

}