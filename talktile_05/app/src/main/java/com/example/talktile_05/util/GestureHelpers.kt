package com.example.talktile_05.util

import android.view.MotionEvent
import kotlin.math.hypot

/**
 * Helpers for detecting two-finger double-tap and long-press semantics.
 * In Compose, you can use pointerInput and this helper to construct gestures.
 *
 * This file exposes one small helper used by the Compose screens.
 */
object GestureHelpers {

    /**
     * Quick two-finger distance check for debugging
     */
    fun distanceBetweenPointers(event: MotionEvent): Float {
        if (event.pointerCount < 2) return 0f
        val dx = event.getX(0) - event.getX(1)
        val dy = event.getY(0) - event.getY(1)
        return hypot(dx, dy)
    }

    // For Compose, prefer pointerInput and detectTapGestures / detectTransformGestures for custom gestures.
}
