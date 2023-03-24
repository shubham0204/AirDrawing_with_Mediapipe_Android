package com.shubham0204.ml.mediapipehandsdemo

import android.graphics.Point

// Data class which holds landmarks for the thumb and index finger
data class HandLandmarks(
    val middleFinger : Point,
    val index : Point
) {

    constructor() : this( Point( 0 , 0 ) , Point( 0 , 0 ))

    override fun toString(): String {
        return "Middle Finger: $middleFinger  Index: $index"
    }

}