package com.example.naedafront.ui.screen.facepay

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FaceHeadPoseHeuristicsTest {
    @Test
    fun leftDirection_usesPositiveYawForFrontCamera() {
        assertTrue(matchesFaceCaptureDirection(11f, 1f, 0f, FaceCaptureDirection.LEFT))
        assertFalse(matchesFaceCaptureDirection(-11f, 1f, 0f, FaceCaptureDirection.LEFT))
    }

    @Test
    fun rightDirection_usesNegativeYawForFrontCamera() {
        assertTrue(matchesFaceCaptureDirection(-11f, 1f, 0f, FaceCaptureDirection.RIGHT))
        assertFalse(matchesFaceCaptureDirection(11f, 1f, 0f, FaceCaptureDirection.RIGHT))
    }

    @Test
    fun upDirection_acceptsModeratePitchWhenPitchDominates() {
        assertTrue(matchesFaceCaptureDirection(3f, 8f, 0f, FaceCaptureDirection.UP))
        assertFalse(matchesFaceCaptureDirection(12f, 8f, 0f, FaceCaptureDirection.UP))
    }

    @Test
    fun downDirection_acceptsModeratePitchWhenPitchDominates() {
        assertTrue(matchesFaceCaptureDirection(2f, -8f, 0f, FaceCaptureDirection.DOWN))
        assertFalse(matchesFaceCaptureDirection(-12f, -8f, 0f, FaceCaptureDirection.DOWN))
    }
}
