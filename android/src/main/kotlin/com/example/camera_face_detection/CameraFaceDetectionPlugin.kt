package com.example.camera_face_detection

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import android.util.Size
import android.widget.Toast
import androidx.annotation.NonNull
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageProxy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import io.flutter.embedding.android.FlutterActivity

import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.EventChannel
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import io.flutter.plugin.common.PluginRegistry
import java.lang.Exception
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

typealias FaceDetectionListener = (faces: List<Face>) -> Unit
typealias EventSinkGetter = (eventSink: EventChannel.EventSink?) -> Unit

/** CameraFaceDetectionPlugin */
class CameraFaceDetectionPlugin : FlutterPlugin, MethodCallHandler, ActivityAware, PluginRegistry.RequestPermissionsResultListener {
    /// The MethodChannel that will the communication between Flutter and native Android
    ///
    /// This local reference serves to register the plugin with the Flutter Engine and unregister it
    /// when the Flutter Engine is detached from the Activity
    private lateinit var channel: MethodChannel
    private lateinit var eventChannel: EventChannel

    private var cameraProvider: ProcessCameraProvider? = null
    private var isReady = false
    private var isDetecting = false

    private lateinit var cameraExecutor: ExecutorService
    private var activity: FlutterActivity? = null
    private var eventSink: EventChannel.EventSink? = null

    override fun onAttachedToEngine(@NonNull flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
        channel = MethodChannel(flutterPluginBinding.binaryMessenger, "camera_face_detection")
        channel.setMethodCallHandler(this)

        eventChannel = EventChannel(flutterPluginBinding.binaryMessenger, "camera_face_detection_stream")

        eventChannel.setStreamHandler(object : EventChannel.StreamHandler {
            override fun onListen(arguments: Any?, events: EventChannel.EventSink?) {
                eventSink = events;
            }

            override fun onCancel(arguments: Any?) {

            }

        })
    }

    override fun onMethodCall(@NonNull call: MethodCall, @NonNull result: Result) {
        when (call.method) {
            "getPlatformVersion" -> result.success("Android ${android.os.Build.VERSION.RELEASE}")
            "startDetection" -> {
                if (!isReady || activity == null) {
                    Toast.makeText(activity, "cannot start detection", Toast.LENGTH_SHORT).show()
                    return result.success(false)
                }
                if (allPermissionsGranted()) {
                    startDetection()
                    result.success(true)
                } else {
                    ActivityCompat.requestPermissions(activity!!, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
                    result.success(false)
                }
            }
            "stopDetection" -> {
                stopDetection()
                result.success(true)
            }
            "isReady" -> result.success(isReady)
            "isDetecting" -> result.success(isDetecting)
            "hasPermissions" -> result.success(allPermissionsGranted())
            else -> {
                result.notImplemented()
            }
        }
    }

    override fun onDetachedFromEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
        channel.setMethodCallHandler(null)
    }

    private fun startDetection() {
        Toast.makeText(activity, "Starting detection...", Toast.LENGTH_SHORT).show()
        val cameraProviderFuture = ProcessCameraProvider.getInstance(activity!!.context)

        cameraProviderFuture.addListener(Runnable {
            cameraProvider = cameraProviderFuture.get()

            val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

            val imageAnalyzer = ImageAnalysis.Builder()
                    .setTargetResolution(Size(640, 480))
                    .build()
                    .also {
                        it.setAnalyzer(
                                cameraExecutor,
                                FaceDetectionAnalyzer { faces ->
                                    run {
                                        if (faces.isNotEmpty()) {
                                            eventSink?.success(faces.map { it ->
                                                hashMapOf(
                                                        "smilingProbability" to it.smilingProbability,
                                                        "leftEyeOpenProbability" to it.leftEyeOpenProbability,
                                                        "rightEyeOpenProbability" to it.rightEyeOpenProbability,
                                                        "headEulerAngleX" to it.headEulerAngleX,
                                                        "headEulerAngleY" to it.headEulerAngleY,
                                                        "headEulerAngleZ" to it.headEulerAngleZ,
                                                        "trackingId" to it.trackingId
                                                )
                                            })
                                        }
                                    }
                                }
                        )
                    }

            try {
                cameraProvider?.unbindAll()
                isDetecting = true

                cameraProvider?.bindToLifecycle(activity!!, cameraSelector, imageAnalyzer)
                Toast.makeText(activity, "Started detection", Toast.LENGTH_SHORT).show()
            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }
        }, ContextCompat.getMainExecutor(activity))


    }

    private fun stopDetection() {
        cameraProvider?.unbindAll()
        isDetecting = false
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
                activity!!.baseContext, it
        ) == PackageManager.PERMISSION_GRANTED
    }


    companion object {
        private const val TAG = "CameraFaceDetectionPlug"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private const val RATIO_4_3_VALUE = 4.0 / 3.0
        private const val RATIO_16_9_VALUE = 16.0 / 9.0
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }

    private class FaceDetectionAnalyzer(private var listener: FaceDetectionListener) :
            ImageAnalysis.Analyzer {
        @SuppressLint("UnsafeExperimentalUsageError")
        override fun analyze(imageProxy: ImageProxy) {
            val mediaImage = imageProxy.image ?: return

            val faceDetectionOptions = FaceDetectorOptions.Builder()
                    .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                    .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                    .build()

            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

            val detector = FaceDetection.getClient(faceDetectionOptions)

            detector.process(image)
                    .addOnSuccessListener { faces -> listener(faces) }
                    .addOnFailureListener { e -> Log.e(TAG, "Error : ${e.message}", e) }
                    .addOnCompleteListener { imageProxy.close() }
        }

    }


    private class OnEventSinkHandler(private var getter: EventSinkGetter) : EventChannel.StreamHandler {
        override fun onListen(arguments: Any?, events: EventChannel.EventSink?) {
            getter(events)
        }

        override fun onCancel(arguments: Any?) {

        }

    }

    override fun onAttachedToActivity(binding: ActivityPluginBinding) {
        activity = binding.activity as FlutterActivity

        binding.addRequestPermissionsResultListener(this)
        cameraExecutor = Executors.newSingleThreadExecutor()
        isReady = true
    }

    override fun onDetachedFromActivityForConfigChanges() {
        isReady = false
        activity = null
    }

    override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {
        activity = binding.activity as FlutterActivity
        binding.addRequestPermissionsResultListener(this)
        isReady = true
    }

    override fun onDetachedFromActivity() {
        cameraExecutor.shutdown()
        isReady = false
        activity = null
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>?, grantResults: IntArray?): Boolean {
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startDetection()
                return true
            } else {
                Toast.makeText(activity, "Permissions not granted by the user", Toast.LENGTH_SHORT)
                        .show()
            }
        }
        return false
    }


}
