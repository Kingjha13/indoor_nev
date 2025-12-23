//package com.example.anchor
//
//import android.os.Bundle
//import android.util.Log
//import androidx.activity.ComponentActivity
//import androidx.activity.compose.setContent
//import androidx.compose.foundation.layout.fillMaxSize
//import androidx.compose.runtime.*
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.platform.LocalContext
//import com.google.ar.core.*
//import io.github.sceneview.ar.ARScene
//import io.github.sceneview.ar.node.AnchorNode
//import io.github.sceneview.node.ModelNode
//import io.github.sceneview.model.ModelInstance
//import io.github.sceneview.rememberEngine
//import io.github.sceneview.rememberModelLoader
//import kotlinx.coroutines.*
//import kotlin.math.min
//import kotlin.math.sqrt
//
//class MainActivity : ComponentActivity() {
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        setContent { ARNavigationScreen() }
//    }
//}
//
//@Composable
//fun ARNavigationScreen() {
//
//    val context = LocalContext.current
//    val engine = rememberEngine()
//    val modelLoader = rememberModelLoader(engine)
//
//    // ✅ detected image poses
//    val detectedImages = remember { mutableMapOf<String, Pose>() }
//
//    // ✅ arrow model (SceneView type)
//    var arrowModel by remember { mutableStateOf<ModelInstance?>(null) }
//
//    var pathPlaced by remember { mutableStateOf(false) }
//
//    // ✅ load model ONCE (CORRECT WAY)
//    LaunchedEffect(Unit) {
//        arrowModel = modelLoader.loadModelInstance("arrow.glb")
//        Log.d("AR", "✅ Arrow model loaded")
//    }
//
//    ARScene(
//        modifier = Modifier.fillMaxSize(),
//        engine = engine,
//
//        onSessionCreated = { session ->
//            try {
//                val config = Config(session)
//
//                config.augmentedImageDatabase =
//                    AugmentedImageDatabase.deserialize(
//                        session,
//                        context.assets.open("room_markers.imgdb")
//                    )
//
//                config.focusMode = Config.FocusMode.AUTO
//                config.updateMode = Config.UpdateMode.LATEST_CAMERA_IMAGE
//
//                session.configure(config)
//                Log.d("AR", "✅ Session configured")
//
//            } catch (e: Exception) {
//                Log.e("AR", "❌ Failed to configure session", e)
//            }
//        },
//
//        onSessionUpdated = { session, frame ->
//
//            val images = frame.getUpdatedTrackables(AugmentedImage::class.java)
//
//            for (image in images) {
//
//                if (image.trackingState != TrackingState.TRACKING) continue
//
//                if (!detectedImages.containsKey(image.name)) {
//
//                    detectedImages[image.name] = image.centerPose
//                    Log.d("AR", "🎯 Detected: ${image.name}")
//
//                    val anchor = image.createAnchor(image.centerPose)
//                    placeArrow(engine, arrowModel, anchor)
//                }
//            }
//
//            if (detectedImages.size == 2 && !pathPlaced && arrowModel != null) {
//                pathPlaced = true
//
//                val poses = detectedImages.values.toList()
//                drawArrowPath(
//                    engine,
//                    arrowModel!!,
//                    session,
//                    poses[0],
//                    poses[1]
//                )
//            }
//        }
//    )
//}
//
//fun placeArrow(
//    engine: com.google.android.filament.Engine,
//    model: ModelInstance?,
//    anchor: Anchor
//) {
//    if (model == null) return
//
//    val anchorNode = AnchorNode(engine, anchor)
//    val node = ModelNode(
//        modelInstance = model,
//        scaleToUnits = 0.25f
//    )
//    anchorNode.addChildNode(node)
//}
//
//fun drawArrowPath(
//    engine: com.google.android.filament.Engine,
//    model: ModelInstance,
//    session: Session,
//    start: Pose,
//    end: Pose
//) {
//    val dx = end.tx() - start.tx()
//    val dz = end.tz() - start.tz()
//
//    val distance = sqrt(dx * dx + dz * dz)
//    val step = 0.8f
//    val count = min((distance / step).toInt(), 25)
//
//    CoroutineScope(Dispatchers.Main).launch {
//        for (i in 1 until count) {
//            val t = i.toFloat() / count
//
//            val pose = Pose.makeTranslation(
//                start.tx() + dx * t,
//                start.ty(),
//                start.tz() + dz * t
//            )
//
//            val anchor = session.createAnchor(pose)
//            placeArrow(engine, model, anchor)
//        }
//    }
//}



package com.example.anchor

//import android.os.Bundle
//import android.util.Log
//import androidx.activity.ComponentActivity
//import androidx.activity.compose.setContent
//import androidx.compose.foundation.layout.fillMaxSize
//import androidx.compose.runtime.*
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.platform.LocalContext
//import com.google.ar.core.*
//import io.github.sceneview.ar.ARScene
//import io.github.sceneview.ar.node.AnchorNode
//import io.github.sceneview.node.ModelNode
//import io.github.sceneview.model.ModelInstance
//import io.github.sceneview.rememberEngine
//import io.github.sceneview.rememberModelLoader
//import kotlinx.coroutines.CoroutineScope
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.launch
//import kotlin.math.min
//import kotlin.math.sqrt
//
//class MainActivity : ComponentActivity() {
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        setContent { ARNavigationScreen() }
//    }
//}
//
//@Composable
//fun ARNavigationScreen() {
//
//    val context = LocalContext.current
//    val engine = rememberEngine()
//    val modelLoader = rememberModelLoader(engine)
//
//    // detected image poses
//    val detectedPoses = remember { mutableMapOf<String, Pose>() }
//
//    var arrowModel by remember { mutableStateOf<ModelInstance?>(null) }
//    var routePlaced by remember { mutableStateOf(false) }
//
//    // load model ONCE
//    LaunchedEffect(Unit) {
//        arrowModel = modelLoader.loadModelInstance("arrow.glb")
//        Log.d("AR", "✅ Arrow model loaded")
//    }
//
//    ARScene(
//        modifier = Modifier.fillMaxSize(),
//        engine = engine,
//
//        onSessionCreated = { session ->
//            val config = Config(session)
//
//            config.augmentedImageDatabase =
//                AugmentedImageDatabase.deserialize(
//                    session,
//                    context.assets.open("room_markers.imgdb")
//                )
//
//            config.focusMode = Config.FocusMode.AUTO
//            session.configure(config)
//
//            Log.d("AR", "✅ AR Session configured")
//        },
//
//        onSessionUpdated = { session, frame ->
//
//            val images = frame.getUpdatedTrackables(AugmentedImage::class.java)
//
//            for (image in images) {
//
//                if (image.trackingState != TrackingState.TRACKING) continue
//                if (detectedPoses.containsKey(image.name)) continue
//
//                detectedPoses[image.name] = image.centerPose
//                Log.d("AR", "🎯 Image detected: ${image.name}")
//
//                val anchor = image.createAnchor(image.centerPose)
//                placeArrow(engine, arrowModel, anchor)
//            }
//
//            if (!routePlaced && arrowModel != null && detectedPoses.isNotEmpty()) {
//                routePlaced = true
//
//                val startPose = detectedPoses.values.first()
//
//                // ✅ FAKE DESTINATION (2 meters forward)
//                val fakeEndPose = startPose.compose(
//                    Pose.makeTranslation(0f, 0f, -2f)
//                )
//
//                drawArrowPath(
//                    engine = engine,
//                    model = arrowModel!!,
//                    session = session,
//                    start = startPose,
//                    end = fakeEndPose
//                )
//            }
//        }
//    )
//}
//
///* ---------------- HELPERS ---------------- */
//
//fun placeArrow(
//    engine: com.google.android.filament.Engine,
//    model: ModelInstance?,
//    anchor: Anchor
//) {
//    if (model == null) return
//
//    val anchorNode = AnchorNode(engine, anchor)
//
//    val node = ModelNode(
//        modelInstance = model,
//        scaleToUnits = 0.25f
//    )
//
//    anchorNode.addChildNode(node)
//}
//
//fun drawArrowPath(
//    engine: com.google.android.filament.Engine,
//    model: ModelInstance,
//    session: Session,
//    start: Pose,
//    end: Pose
//) {
//    val dx = end.tx() - start.tx()
//    val dz = end.tz() - start.tz()
//
//    val distance = sqrt(dx * dx + dz * dz)
//    val step = 0.6f
//    val count = min((distance / step).toInt(), 20)
//
//    CoroutineScope(Dispatchers.Main).launch {
//        for (i in 1..count) {
//            val t = i.toFloat() / count
//
//            val pose = Pose.makeTranslation(
//                start.tx() + dx * t,
//                start.ty(),
//                start.tz() + dz * t
//            )
//
//            val anchor = session.createAnchor(pose)
//            placeArrow(engine, model, anchor)
//        }
//    }
//}













import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.Toast
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.runtime.Composable
import androidx.activity.compose.setContent

class MainActivity : ComponentActivity() {

    private lateinit var indoorMapView: IndoorMapView

    private val imagePicker =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                result.data?.data?.let { loadMap(it) }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MapScreen()
        }

        addButtons()
    }

    @Composable
    fun MapScreen() {
        AndroidView(factory = {
            indoorMapView = IndoorMapView(it)
            indoorMapView
        })
    }

    private fun addButtons() {
        val loadBtn = Button(this).apply {
            text = "Load Map"
            setOnClickListener { openGallery() }
        }

        val moveBtn = Button(this).apply {
            text = "Fake Move"
            setOnClickListener { indoorMapView.fakeMove() }
        }

        addContentView(loadBtn,
            android.widget.FrameLayout.LayoutParams(
                android.widget.FrameLayout.LayoutParams.WRAP_CONTENT,
                android.widget.FrameLayout.LayoutParams.WRAP_CONTENT
            )
        )

        addContentView(moveBtn,
            android.widget.FrameLayout.LayoutParams(
                android.widget.FrameLayout.LayoutParams.WRAP_CONTENT,
                android.widget.FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = 150 }
        )
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        imagePicker.launch(intent)
    }

    private fun loadMap(uri: Uri) {
        val stream = contentResolver.openInputStream(uri)
        val bitmap = BitmapFactory.decodeStream(stream)
        indoorMapView.setMap(bitmap)
        Toast.makeText(this, "Tap map to set start", Toast.LENGTH_SHORT).show()
    }
}






class IndoorMapView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private var mapBitmap: Bitmap? = null
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val pathPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLUE
        strokeWidth = 5f
        style = Paint.Style.STROKE
    }

    private var x = -1f
    private var y = -1f
    private val path = Path()

    /** ✅ called from activity */
    fun setMap(bitmap: Bitmap) {
        mapBitmap = bitmap
        invalidate()
    }

    /** ✅ fake movement for testing */
    fun fakeMove() {
        if (x < 0 || y < 0) return
        x += 20
        y += 10
        path.lineTo(x, y)
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        mapBitmap?.let {
            canvas.drawBitmap(it, null, Rect(0, 0, width, height), null)
        }

        canvas.drawPath(path, pathPaint)

        if (x >= 0 && y >= 0) {
            paint.color = Color.RED
            canvas.drawCircle(x, y, 12f, paint)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            x = event.x
            y = event.y
            path.moveTo(x, y)
            invalidate()
            return true
        }
        return super.onTouchEvent(event)
    }
}
