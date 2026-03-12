package com.example.naedafront.ui.screen.facepay

import kotlin.math.abs

internal enum class FaceCaptureDirection {
    FRONT,
    LEFT,
    RIGHT,
    UP,
    DOWN
}

private const val FRONT_YAW_MAX = 14f
private const val FRONT_PITCH_MAX = 12f
private const val FRONT_ROLL_MAX = 12f
private const val SIDE_YAW_MIN = 9f
private const val VERTICAL_PITCH_MIN = 7f

internal fun matchesFaceCaptureDirection(
    yaw: Float,
    pitch: Float,
    roll: Float,
    direction: FaceCaptureDirection
): Boolean {
    val yawAbs = abs(yaw)
    val pitchAbs = abs(pitch)

    return when (direction) {
        FaceCaptureDirection.FRONT -> {
            yawAbs < FRONT_YAW_MAX &&
                pitchAbs < FRONT_PITCH_MAX &&
                abs(roll) < FRONT_ROLL_MAX
        }

        FaceCaptureDirection.LEFT -> {
            val yawScore = yawAbs / SIDE_YAW_MIN
            val pitchScore = pitchAbs / VERTICAL_PITCH_MIN
            yaw >= SIDE_YAW_MIN && yawScore >= pitchScore
        }

        FaceCaptureDirection.RIGHT -> {
            val yawScore = yawAbs / SIDE_YAW_MIN
            val pitchScore = pitchAbs / VERTICAL_PITCH_MIN
            yaw <= -SIDE_YAW_MIN && yawScore >= pitchScore
        }

        FaceCaptureDirection.UP -> {
            val yawScore = yawAbs / SIDE_YAW_MIN
            val pitchScore = pitchAbs / VERTICAL_PITCH_MIN
            pitch <= -VERTICAL_PITCH_MIN && pitchScore > yawScore
        }

        FaceCaptureDirection.DOWN -> {
            val yawScore = yawAbs / SIDE_YAW_MIN
            val pitchScore = pitchAbs / VERTICAL_PITCH_MIN
            pitch >= VERTICAL_PITCH_MIN && pitchScore > yawScore
        }
    }
}
