package com.example.anchor

import android.content.Context
import io.github.sceneview.math.Position

enum class CloudMode {
    HOME,
    HOST_SELECT,
    RESOLVE_RUNNING,
    RESET,
    ADD_MARKER_SELECT
}

//NEED TO EXPERIMENT
private const val PREF_MARKERS = "cloud_markers"

fun saveMarkers(context: Context, cloudId: String, positions: List<Position>) {
    val encoded = positions.joinToString("|") { p -> "${p.x},${p.y},${p.z}" }
    context.getSharedPreferences(PREF_MARKERS, Context.MODE_PRIVATE)
        .edit()
        .putString("markers_$cloudId", encoded)
        .apply()
}

fun loadMarkers(context: Context, cloudId: String): List<Position> {
    val pref = context.getSharedPreferences(PREF_MARKERS, Context.MODE_PRIVATE)
    val encoded = pref.getString("markers_$cloudId", null) ?: return emptyList()
    return encoded.split("|")
        .mapNotNull { triplet ->
            val parts = triplet.split(",")
            if (parts.size == 3) {
                val x = parts[0].toFloatOrNull() ?: return@mapNotNull null
                val y = parts[1].toFloatOrNull() ?: return@mapNotNull null
                val z = parts[2].toFloatOrNull() ?: return@mapNotNull null
                Position(x, y, z)
            } else null
        }
}
