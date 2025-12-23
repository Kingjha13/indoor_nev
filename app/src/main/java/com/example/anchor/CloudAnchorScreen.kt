package com.example.anchor

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.google.ar.core.Anchor
import com.google.ar.core.Config
import com.google.ar.core.Frame
import com.google.ar.core.Session
import io.github.sceneview.ar.ARScene
import io.github.sceneview.ar.arcore.createAnchorOrNull
import io.github.sceneview.ar.arcore.isValid
import io.github.sceneview.ar.node.CloudAnchorNode
import io.github.sceneview.ar.rememberARCameraStream
import io.github.sceneview.math.Position
import io.github.sceneview.model.ModelInstance
import io.github.sceneview.node.ModelNode
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberMaterialLoader
import io.github.sceneview.rememberModelLoader
import io.github.sceneview.rememberNodes
import io.github.sceneview.rememberOnGestureListener

@Composable
fun CloudAnchorScreen() {
    val context = LocalContext.current

    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)
    val materialLoader = rememberMaterialLoader(engine)
    val cameraStream = rememberARCameraStream(materialLoader)
    val childNodes = rememberNodes()

    var frame by remember { mutableStateOf<Frame?>(null) }
    var session by remember { mutableStateOf<Session?>(null) }

    var mode by remember { mutableStateOf(CloudMode.HOME) }
    var cloudAnchorId by remember { mutableStateOf(TextFieldValue("")) }
    var isLoading by remember { mutableStateOf(false) }

    var currentCloudNode by remember { mutableStateOf<CloudAnchorNode?>(null) }

    var extraMarkerPositions by remember { mutableStateOf<List<Position>>(emptyList()) }

    Box(modifier = Modifier.fillMaxSize()) {

        ARScene(
            modifier = Modifier.fillMaxSize(),
            engine = engine,
            modelLoader = modelLoader,
            cameraStream = cameraStream,
            childNodes = childNodes,

            sessionConfiguration = { _, config ->
                config.cloudAnchorMode = Config.CloudAnchorMode.ENABLED
                config.planeFindingMode = Config.PlaneFindingMode.HORIZONTAL
            },

            onSessionUpdated = { arSession, updatedFrame ->
                session = arSession
                frame = updatedFrame
            },

            onGestureListener = rememberOnGestureListener(
                onSingleTapConfirmed = { motionEvent, _ ->
                    val currentFrame = frame ?: return@rememberOnGestureListener
                    val currentSession = session ?: return@rememberOnGestureListener

                    when (mode) {
                        CloudMode.HOST_SELECT -> {
                            // Tap to choose anchor position for HOST
                            val hitResult = currentFrame.hitTest(motionEvent.x, motionEvent.y)
                                .firstOrNull { it.isValid(depthPoint = false, point = false) }
                                ?: return@rememberOnGestureListener

                            val anchor = hitResult.createAnchorOrNull()
                                ?: return@rememberOnGestureListener

                            currentCloudNode?.let {
                                childNodes -= it
                                it.detachAnchor()
                                it.destroy()
                            }

                            val modelInstance: ModelInstance? =
                                modelLoader.createModelInstance("spiderbot.glb")

                            val cloudNode = CloudAnchorNode(engine, anchor).apply {
                                modelInstance?.let {
                                    addChildNode(
                                        ModelNode(modelInstance = it).apply {
                                            position = Position(z = -0.2f)
                                        }
                                    )
                                }
                            }

                            currentCloudNode = cloudNode
                            childNodes += cloudNode
                            extraMarkerPositions = emptyList()


                            isLoading = true
                            cloudNode.host(currentSession) { id, state ->
                                isLoading = false
                                if (state == Anchor.CloudAnchorState.SUCCESS && id != null) {
                                    cloudAnchorId = TextFieldValue(id)
                                    mode = CloudMode.RESET
                                    saveMarkers(context, id, extraMarkerPositions)

                                    Toast.makeText(
                                        context,
                                        "Hosted! ID: $id",
                                        Toast.LENGTH_LONG
                                    ).show()
                                } else {
                                    mode = CloudMode.HOME
                                    Toast.makeText(
                                        context,
                                        "Hosting failed: ${state.name}",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            }
                        }

                        CloudMode.ADD_MARKER_SELECT -> {
                            val cloudNode = currentCloudNode ?: return@rememberOnGestureListener

                            val hitResult = currentFrame.hitTest(motionEvent.x, motionEvent.y)
                                .firstOrNull { it.isValid(depthPoint = false, point = false) }
                                ?: return@rememberOnGestureListener

                            val pose = hitResult.hitPose
                            val hitWorldPos = Position(
                                pose.tx(),
                                pose.ty(),
                                pose.tz()
                            )

                            val anchorWorldPos = cloudNode.worldPosition
                            val localPos = Position(
                                hitWorldPos.x - anchorWorldPos.x,
                                hitWorldPos.y - anchorWorldPos.y,
                                hitWorldPos.z - anchorWorldPos.z
                            )

                            val markerInstance =
                                modelLoader.createModelInstance("spiderbot.glb")

                            markerInstance?.let {
                                cloudNode.addChildNode(
                                    ModelNode(modelInstance = it).apply {
                                        position = localPos
                                    }
                                )
                            }

                            extraMarkerPositions = extraMarkerPositions + localPos
                            val id = cloudAnchorId.text.trim()
                            if (id.isNotEmpty()) {
                                saveMarkers(context, id, extraMarkerPositions)
                            }

                            mode = CloudMode.RESET

                            Toast.makeText(
                                context,
                                "Extra marker added",
                                Toast.LENGTH_SHORT
                            ).show()
                        }

                        else -> Unit
                    }
                }
            )
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            OutlinedTextField(
                value = cloudAnchorId,
                onValueChange = { cloudAnchorId = it },
                label = { Text("Cloud Anchor ID") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(8.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = {
                        mode = CloudMode.HOST_SELECT
                        Toast.makeText(
                            context,
                            "Tap on a plane to host anchor",
                            Toast.LENGTH_SHORT
                        ).show()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Host")
                }

                Button(
                    onClick = {
                        val id = cloudAnchorId.text.trim()
                        val currentSession = session
                        if (id.isEmpty() || currentSession == null) {
                            Toast.makeText(
                                context,
                                "No ID / no AR session yet",
                                Toast.LENGTH_SHORT
                            ).show()
                            return@Button
                        }

                        isLoading = true
                        mode = CloudMode.RESOLVE_RUNNING

                        CloudAnchorNode.resolve(
                            engine = engine,
                            session = currentSession,
                            cloudAnchorId = id
                        ) { state, node ->
                            isLoading = false
                            if (!state.isError && node != null) {
                                // remove previous
                                currentCloudNode?.let {
                                    childNodes -= it
                                    it.detachAnchor()
                                    it.destroy()
                                }
                                currentCloudNode = node
                                childNodes += node

                                extraMarkerPositions = loadMarkers(context, id)
                                extraMarkerPositions.forEach { pos ->
                                    val inst =
                                        modelLoader.createModelInstance("spiderbot.glb")
                                    inst?.let {
                                        node.addChildNode(
                                            ModelNode(modelInstance = it).apply {
                                                position = pos
                                            }
                                        )
                                    }
                                }

                                mode = CloudMode.RESET
                                Toast.makeText(
                                    context,
                                    "Resolved with ${extraMarkerPositions.size} extra markers",
                                    Toast.LENGTH_LONG
                                ).show()
                            } else {
                                mode = CloudMode.HOME
                                Toast.makeText(
                                    context,
                                    "Resolve failed: ${state.name}",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    },
                    enabled = cloudAnchorId.text.isNotBlank(),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Resolve")
                }
            }

            if (isLoading) {
                Spacer(Modifier.height(9.dp))
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            Spacer(Modifier.height(4.dp))

            Text(
                text = when (mode) {
                    CloudMode.HOME ->
                        "Host: create anchor. Resolve: load existing ID.\nAfter host/resolve, use + to add extra markers."
                    CloudMode.HOST_SELECT ->
                        "Move device until planes are detected, then tap to host anchor."
                    CloudMode.RESOLVE_RUNNING ->
                        "Resolving Cloud Anchor..."
                    CloudMode.RESET ->
                        "Anchor active. You can add extra markers with +, or host/resolve another ID."
                    CloudMode.ADD_MARKER_SELECT ->
                        "Tap where you want to place an extra 3D marker."
                },
                style = MaterialTheme.typography.bodySmall,
                color = Color.White
            )
        }

        if (currentCloudNode != null && !isLoading
        ) {
            FloatingActionButton(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
                onClick = {
                    mode = CloudMode.ADD_MARKER_SELECT
                    Toast.makeText(
                        context,
                        "Tap on a plane to add extra marker",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add marker")
            }
        }
    }
}