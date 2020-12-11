package com.example.camera_face_detection

import android.graphics.Bitmap

data class MyDetectedFace(
        var smilingProbability: Float?,
        var leftEyeOpenProbability: Float?,
        var rightEyeOpenProbability: Float?,
        var headEulerAngleX: Float?,
        var headEulerAngleY: Float?,
        var headEulerAngleZ: Float?,
        var trackingId: Int?,
        var gender: String?,
        var ageRange: String?,
        var croppedBitmap: Bitmap
) {
    override fun toString(): String {
        return "smilingProbability: $smilingProbability \n" +
                "leftEyeOpenProbability: $leftEyeOpenProbability \n" +
                "rightEyeOpenProbability: $rightEyeOpenProbability \n" +
                "headEulerAngleX: $headEulerAngleX \n" +
                "headEulerAngleY: $headEulerAngleY \n" +
                "headEulerAngleZ: $headEulerAngleZ \n" +
                "trackingId: $trackingId \n" +
                "gender: $gender \n" +
                "ageRange: $ageRange"
    }

    fun toMap(): HashMap<String, Any?> {
        return hashMapOf(
                "smilingProbability" to smilingProbability,
                "leftEyeOpenProbability" to leftEyeOpenProbability,
                "rightEyeOpenProbability" to rightEyeOpenProbability,
                "headEulerAngleX" to headEulerAngleX,
                "headEulerAngleY" to headEulerAngleY,
                "headEulerAngleZ" to headEulerAngleZ,
                "trackingId" to trackingId,
                "gender" to (gender ?: "unknown"),
                "ageRange" to (ageRange ?: "unknown")
        )
    }
}