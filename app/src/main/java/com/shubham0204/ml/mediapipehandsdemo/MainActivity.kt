package com.shubham0204.ml.mediapipehandsdemo

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Point
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowInsets
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.AspectRatio
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.Executors

class MainActivity : ComponentActivity() {


    private val cameraPermissionEnabled = MutableLiveData( false )
    private val drawColorName = MutableLiveData( Color.Blue )
    private val fingerPosition = MutableLiveData<IntArray>()
    private val indexPosition = MutableLiveData<Point>()
    private var layoutWidth  = 0
    private var layoutHeight = 0
    val path = Path()
    val brushPath = BrushPath()
    private val brushManager = BrushManager()
    private lateinit var frameAnalyzer : FrameAnalyzer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ActivityUI()
        }

        frameAnalyzer = FrameAnalyzer( this , resultCallback )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.decorView.windowInsetsController!!
                .hide( WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
        }
        else {
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN
        }

        if ( ActivityCompat.checkSelfPermission( this , Manifest.permission.CAMERA )
            != PackageManager.PERMISSION_GRANTED ) {
            requestCameraPermission()
        }
        else {
            cameraPermissionEnabled.value = true
        }

    }

    private val resultCallback = { pos : IntArray ->
        val job = CoroutineScope( Dispatchers.Main ).launch {
            Log.e( "State" , "Livedata updated " + pos.contentToString() )
            fingerPosition.value = pos
        }
    }

    @Composable
    private fun ActivityUI() {
        ConstraintLayout {
            val ( preview , colorPicker , exportButton ) = createRefs()
            CameraPreview( modifier = Modifier.constrainAs( preview ) {
                absoluteLeft.linkTo( parent.absoluteLeft )
                absoluteRight.linkTo( parent.absoluteRight )
                top.linkTo( parent.top )
                bottom.linkTo( parent.bottom )
            } )
            ColorPicker(
                modifier = Modifier
                    .constrainAs(colorPicker) {
                        absoluteRight.linkTo(parent.absoluteRight)
                        top.linkTo(parent.top)
                    }
                    .padding(16.dp)
            )
            Button(
                onClick = { exportImage() } ,
                modifier = Modifier
                    .constrainAs(exportButton) {
                        absoluteLeft.linkTo(parent.absoluteLeft)
                        top.linkTo(parent.top)
                    }
                    .padding(16.dp)
            ) {
                Text(text = "Export" )
            }
        }
    }

    @Composable
    private fun ColorPicker( modifier: Modifier ) {

        Row( modifier = modifier ) {
            ColorPatch(color = Color.Red )
            ColorPatch(color = Color.Yellow )
            ColorPatch(color = Color.Blue )
            ColorPatch(color = Color.Black)
            ColorPatch(color = Color.Green)
        }
    }

    @Composable
    private fun ColorPatch(color : Color ) {
        Canvas(modifier = Modifier
            .size(32.dp)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        drawColorName.value = color
                    }
                )
            },
            onDraw =  {
            if( color == drawColorName.value ) {
                drawCircle( color = Color.White , radius = 10.dp.toPx() )
            }
            drawCircle( color = color , radius = 8.dp.toPx() )
        })
    }

    @Composable
    private fun CameraPreview( modifier: Modifier ) {
        val cameraPermissionState by cameraPermissionEnabled.observeAsState()
        AnimatedVisibility(visible = cameraPermissionState!! ) {
            Preview(  modifier.fillMaxSize() )
            DrawingBackground( modifier )
            DrawingOverlay()
        }
        AnimatedVisibility(visible = !cameraPermissionState!!) {
            Box( modifier = Modifier.fillMaxSize() ) {
                Column( modifier = Modifier.align( Alignment.Center )) {
                    Text( text = "Allow Camera Permissions" )
                    Text( text = "The app cannot work without the camera permission." )
                    Button(onClick = { requestCameraPermission() }) {
                        Text(text = "Allow")
                    }
                }
            }
        }
    }

    @Composable
    private fun DrawingBackground( modifier: Modifier ) {
        // https://stackoverflow.com/a/66942801/13546426
        val context = LocalContext.current
        Surface(
            modifier = modifier
                .fillMaxSize()
                .pointerInput( Unit ) {
                    detectTapGestures(
                        onDoubleTap = {
                            brushManager.clear()
                            fingerPosition.value = IntArray(4)
                            Toast.makeText(context, "Screen cleared.", Toast.LENGTH_SHORT).show()
                        }
                    )
                },
            color = Color( 232 , 190 , 172 , 128 )
        ) {}
    }

    @Composable
    private fun DrawingOverlay() {
        val position by fingerPosition.observeAsState()
        Log.e( "State" , "Moving Point recomposed" )
        Spacer(
            modifier= Modifier
                .fillMaxSize()
                .onGloballyPositioned {
                    layoutHeight = it.size.height
                    layoutWidth = it.size.width
                    frameAnalyzer.setLayoutDims(layoutWidth, layoutHeight)
                }
                .drawWithCache {
                    Log.e("State", "Moving Point recomposed")
                    val drawPos = position ?: IntArray(4)
                    val fingerPosition = FingerPositions(
                        Point(drawPos[0], drawPos[1]),
                        Point(drawPos[2], drawPos[3])
                    )
                    brushManager.nextPoints(fingerPosition, drawColorName.value ?: Color.Blue)
                    onDrawBehind {
                        for (brushPath in brushManager.getAllStrokes()) {
                            drawPath(
                                path = brushPath.path,
                                color = brushPath.color,
                                style = Stroke(5.dp.toPx() , pathEffect = PathEffect.cornerPathEffect( 10.0f ))
                            )
                        }
                        drawPath(
                            brushManager
                                .getCurrentStroke()
                                .path,
                            color = drawColorName.value ?: Color.Blue,
                            style = Stroke(5.dp.toPx())
                        )
                        drawCircle(
                            color = Color.White,
                            radius = 10.0f,
                            center = Offset(drawPos[0].toFloat(), drawPos[1].toFloat())
                        )
                        drawCircle(
                            color = Color.White,
                            radius = 10.0f,
                            center = Offset(drawPos[2].toFloat(), drawPos[3].toFloat())
                        )
                    }
                } ,
        )
    }


    @Composable
    private fun Preview( modifier: Modifier ) {
        val lifecycleOwner = LocalLifecycleOwner.current
        val context = LocalContext.current
        val cameraProviderFuture = remember{ ProcessCameraProvider.getInstance( context ) }
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView( ctx )
                val executor = ContextCompat.getMainExecutor(ctx)
                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }
                    val cameraSelector = CameraSelector.Builder()
                        .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                        .build()
                    val handAnalysis = ImageAnalysis.Builder()
                        .setTargetAspectRatio( AspectRatio.RATIO_16_9 )
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .setOutputImageFormat( ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888 )
                        .build()
                    handAnalysis.setAnalyzer( Executors.newSingleThreadExecutor() , frameAnalyzer )
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview ,
                        handAnalysis
                    )
                }, executor)
                previewView
            } ,
            modifier = modifier
        )
    }

    private fun exportImage() {
        val outputBitmap = Bitmap.createBitmap( layoutWidth , layoutHeight , Bitmap.Config.ARGB_8888 )
        val canvas = Canvas( outputBitmap )
        val paint = Paint().apply {
            style = Paint.Style.STROKE
            strokeWidth = 5.0f
        }
        canvas.drawColor( android.graphics.Color.WHITE )
        for( brushPath in brushManager.getAllStrokes() ) {
            paint.color = android.graphics.Color.rgb(
                ( brushPath.color.red * 255 ).toInt() ,
                ( brushPath.color.green * 255 ).toInt() ,
                ( brushPath.color.blue * 255 ).toInt()
            )
            canvas.drawPath( brushPath.path.asAndroidPath() , paint)
        }
        val tempFile = File( filesDir , "image.png" )
        FileOutputStream( tempFile ).apply {
            outputBitmap.compress( Bitmap.CompressFormat.PNG , 100 , this )
            close()
        }
    }


    private fun requestCameraPermission() {
        cameraPermissionLauncher.launch( Manifest.permission.CAMERA )
    }

    private val cameraPermissionLauncher = registerForActivityResult( ActivityResultContracts.RequestPermission() ) {
            isGranted ->
        if ( isGranted ) {
            cameraPermissionEnabled.value = true
        }
    }

}


