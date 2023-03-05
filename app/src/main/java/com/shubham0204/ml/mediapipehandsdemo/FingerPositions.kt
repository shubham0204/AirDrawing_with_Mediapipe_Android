package com.shubham0204.ml.mediapipehandsdemo

import android.graphics.Point

data class FingerPositions(
    val thumb : Point,
    val index : Point
) {

    constructor() : this( Point( 0 , 0 ) , Point( 0 , 0 ))

    override fun toString(): String {
        return "${thumb.toString()} ${index.toString()}"
    }

}