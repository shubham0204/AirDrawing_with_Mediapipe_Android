package com.shubham0204.ml.mediapipehandsdemo

import android.util.Log
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import kotlin.math.absoluteValue
import kotlin.math.pow
import kotlin.math.sqrt

class BrushPath {

    private var prevX = 0
    private var prevY = 0
    private val distanceThreshold = 10.0f
    var path = Path()
    var path2 = android.graphics.Path()
    var color = Color.Blue

    fun start( x : Int , y : Int ) {
        prevX = x
        prevY = y
        path.moveTo( x.toFloat() , y.toFloat() )
    }

    fun addPoint( x : Int , y : Int ) {
        val dx = ( x - prevX )
        val dy = ( y - prevY )
        val distance = sqrt( dx.toFloat().pow(2) + dy.toFloat().pow(2) )
        Log.e( "Distance" , "Distance: " + distance )
        if ( distance > distanceThreshold ) {
            // path.lineTo( x.toFloat() , y.toFloat() )
            val midX = ( prevX + x ) / 2
            val midY = ( prevY + y ) / 2
            path.quadraticBezierTo( prevX.toFloat(), prevY.toFloat(), midX.toFloat(), midY.toFloat())
            prevX = x
            prevY = y
        }
    }

    fun reset() {
        path = Path()
        prevX = 0
        prevY = 0
    }

}