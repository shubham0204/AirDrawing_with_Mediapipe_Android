package com.shubham0204.ml.mediapipehandsdemo

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.framework.image.MPImage
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.core.Delegate
import com.google.mediapipe.tasks.core.ErrorListener
import com.google.mediapipe.tasks.core.OutputHandler
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarker
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult

class FrameAnalyzer( context : Context , private val handLandmarksResult : ( IntArray ) -> Unit )
    : ImageAnalysis.Analyzer {

    private var isProcessing = false
    private val handLandmarker : HandLandmarker
    private var layoutHeight = 0
    private var layoutWidth = 0
    private var isOverlayTransformInitialized = false
    private val overlayTransform = Matrix()

    private val resultListener = OutputHandler.ResultListener<HandLandmarkerResult, MPImage>{
            result, input ->
        isProcessing = false
        for( handResult in result.landmarks() ) {
            val landmark1 = handResult[8]
            val landmark2 = handResult[4]
            handLandmarksResult( intArrayOf(
                ( landmark2.x() * layoutWidth  ).toInt() ,
                ( landmark2.y() * layoutHeight ).toInt() ,
                ( landmark1.x() * layoutWidth  ).toInt() ,
                ( landmark1.y() * layoutHeight ).toInt() )  )
        }
        if( result.landmarks().size == 0 ) {
            handLandmarksResult( IntArray( 4 ) )
        }
    }

    private val errorListener = ErrorListener {

    }

    init {
        val baseOptionsBuilder = BaseOptions.builder()
            .setModelAssetPath( "hand_landmarker.task" )
            .setDelegate( Delegate.GPU )
        val baseOptions = baseOptionsBuilder.build()
        val optionsBuilder =
            HandLandmarker.HandLandmarkerOptions.builder()
                .setBaseOptions(baseOptions)
                .setNumHands( 1 )
                .setResultListener( resultListener )
                .setErrorListener( errorListener )
                .setRunningMode(RunningMode.LIVE_STREAM)
        val options = optionsBuilder.build()
        handLandmarker = HandLandmarker.createFromOptions( context , options )
    }

    override fun analyze(image: ImageProxy) {
        if( isProcessing ) {
            image.close()
            return
        }
        isProcessing = true
        var bitmapBuffer = Bitmap.createBitmap( image.width , image.height, Bitmap.Config.ARGB_8888 )
        image.use{ bitmapBuffer.copyPixelsFromBuffer(image.planes[0].buffer) }
        if( !isOverlayTransformInitialized ) {
            overlayTransform.apply {
                postRotate( image.imageInfo.rotationDegrees.toFloat())
                postScale(-1f, 1f, image.width.toFloat(), image.height.toFloat() )
            }
            isOverlayTransformInitialized = true
        }
        image.close()
        bitmapBuffer = Bitmap.createBitmap(
            bitmapBuffer, 0, 0, bitmapBuffer.width, bitmapBuffer.height,
            overlayTransform, true)
        handLandmarker.detectAsync( BitmapImageBuilder( bitmapBuffer ).build() , System.currentTimeMillis() )
    }

    fun setLayoutDims( width : Int , height : Int ) {
        layoutHeight = height
        layoutWidth = width
    }

}