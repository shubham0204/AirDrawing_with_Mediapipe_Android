package com.shubham0204.ml.mediapipehandsdemo

import androidx.compose.ui.graphics.Color
import kotlin.math.atan
import kotlin.math.pow
import kotlin.math.sqrt

class BrushManager {

    private var newStrokeAdded = false
    private val strokes = ArrayList<BrushPath>()
    private var currentStroke = BrushPath()
    private val fingerDrawingThreshold = 70.0f
    var isDrawing = false

    fun nextPoints(positions : HandLandmarks, color : Color ) {
        val x1 = positions.middleFinger.x
        val y1 = positions.middleFinger.y
        val x2 = positions.index.x
        val y2 = positions.index.y
        if( distance( x1 , y1 , x2 , y2 ) < fingerDrawingThreshold  ) {
            val midX = ( x1 + x2 ) / 2
            val midY = ( y1 + y2 ) / 2
            if( newStrokeAdded ) {
                currentStroke.start( midX , midY )
                newStrokeAdded = false
            }
            if( x2 != 0 && y2 != 0 ) {
                isDrawing = true
                addPointToStroke( midX , midY )
            }
        }
        else {
            strokes.add( currentStroke )
            currentStroke = BrushPath()
            currentStroke.pathColor = color
            isDrawing = false
            newStrokeAdded = true
        }
    }


    fun getAllStrokes() : List<BrushPath> {
        return strokes
    }

    fun getCurrentStroke() : BrushPath {
        return currentStroke
    }

    fun clear() {
        strokes.clear()
        currentStroke.reset()
    }

    private fun addPointToStroke( x : Int , y : Int ) {
        currentStroke.addPoint( x , y )
    }

    private fun angle( x1 : Int , y1 : Int , x2 : Int , y2 : Int ) : Float {
        return Math.toDegrees(atan( ( y2 - y1 ).toFloat() / ( x2 - x1 ) ).toDouble() ).toFloat()
    }

    private fun distance( x1 : Int , y1 : Int , x2 : Int , y2 : Int ) : Float {
        return sqrt( ( x2 - x1 ).toFloat().pow(2) + ( y2 - y1 ).toFloat().pow(2) )
    }

}