package com.mrguven.smartrecycling.ui.fragments

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.RectF
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.util.Size
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.addCallback
import androidx.camera.core.AspectRatio
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.Navigation
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.mrguven.smartrecycling.ml.ObjectDetectorHelper
import com.mrguven.smartrecycling.R
import com.mrguven.smartrecycling.activities.LiveDetectionActivity
import com.mrguven.smartrecycling.ui.adapters.PackagingAdapter
import com.mrguven.smartrecycling.data.model.CoordinateChangeInfo
import com.mrguven.smartrecycling.data.model.Packaging
import com.mrguven.smartrecycling.data.model.PackagingTypes
import com.mrguven.smartrecycling.databinding.FragmentCameraBinding
import org.tensorflow.lite.task.vision.detector.Detection
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.LinkedList
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.sqrt

class CameraFragment : Fragment(), ObjectDetectorHelper.DetectorListener {
    //------------------------------------------------------------------------------------//
//------------------------------------------------------------------------------------//
    private val TAG = "ObjectDetection"
    private lateinit var objectDetectorHelper: ObjectDetectorHelper
    private lateinit var bitmapBuffer: Bitmap
    private var preview: Preview? = null
    private var imageAnalyzer: ImageAnalysis? = null
    private var camera: Camera? = null
    private var cameraProvider: ProcessCameraProvider? = null

    /** Blocking camera operations are performed using this executor */
    private lateinit var cameraExecutor: ExecutorService

    //------------------------------------------------------------------------------------//
//------------------------------------------------------------------------------------//
    private var _fragmentCameraBinding: FragmentCameraBinding? = null
    private val fragmentCameraBinding
        get() = _fragmentCameraBinding!!

    //------------------------------------------------------------------------------------//
//------------------------------------------------------------------------------------//
    override fun onResume() {
        super.onResume()
        // Make sure that all permissions are still present, since the
        // user could have removed them while the app was in paused state.
        if (!PermissionsFragment.hasPermissions(requireContext())) {
            Navigation.findNavController(requireActivity(), R.id.fragment_container)
                .navigate(CameraFragmentDirections.actionCameraToPermissions())
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Shut down our background executor
        cameraExecutor.shutdown()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _fragmentCameraBinding = FragmentCameraBinding.inflate(inflater, container, false)
        readNamesFromFile()
        return fragmentCameraBinding.root
    }

    @SuppressLint("MissingPermission")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initObjectDetectorConfigs()
        initBottomSheetControls()
        initActivityRelatedConfigs()
        initDetectionListRecyclerView()
        startAccelerometer()

        fragmentCameraBinding.recycleButton.setOnClickListener {
            navigateToFeedbackFragment()
        }
    }

    private lateinit var detectionResultRecyclerView: RecyclerView
    private lateinit var packagingAdapter: PackagingAdapter
    private lateinit var containerName: String
    private var detectionMap: MutableMap<String, Packaging> = mutableMapOf()
    private var packagingList: MutableList<Packaging> = mutableListOf()
    private var detectionList: MutableList<Packaging> = mutableListOf()
    private var containerCoordinateChangeInfo = CoordinateChangeInfo(
        title = CONTAINER_LABEL,
        firstSeenBox = RectF().apply { setEmpty() },
        lastSeenBox = RectF().apply { setEmpty() },
    )
    private val packagingCoordinateChangeInfoMap = mutableMapOf<String, CoordinateChangeInfo>()

    private fun initObjectDetectorConfigs() {
        objectDetectorHelper = ObjectDetectorHelper(
            context = requireContext(),
            objectDetectorListener = this
        )
        // Initialize our background executor
        cameraExecutor = Executors.newSingleThreadExecutor()

        // Wait for the views to be properly laid out
        fragmentCameraBinding.viewFinder.post {
            // Set up the camera and its use cases
            setUpCamera()
        }
    }

    private fun initActivityRelatedConfigs() {
        val activity = activity

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            if (activity != null) {
                fragmentManager?.beginTransaction()?.remove(this@CameraFragment)?.commit()
                activity.finish()
            }
        }

        if (activity != null) {
            if (activity is LiveDetectionActivity) {
                containerName = activity.getContainerName()
            }
        }
    }

    private fun initDetectionListRecyclerView() {
        detectionResultRecyclerView = fragmentCameraBinding.detectionResultRecyclerView
        detectionResultRecyclerView.layoutManager = LinearLayoutManager(context)
        packagingAdapter = PackagingAdapter(detectionList)
        detectionResultRecyclerView.adapter = packagingAdapter
    }

    private fun navigateToFeedbackFragment() {
        val argumentArray = detectionList.toTypedArray()
        val action = CameraFragmentDirections.actionCameraFragmentToFeedbackFragment(
            detectionList = argumentArray,
            containerName = containerName
        )
        findNavController().navigate(action)
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun updateData() {
        packagingAdapter.notifyDataSetChanged()
    }

    private var lastAcceleration: FloatArray? = null
    private val shakeThreshold = 1.0// Threshold value to detect significant movement
    private var isCameraSteady = true
    private var isShakeWarningVisible = false
    private var isContainerSeen = false

    // Listen for changes in the accelerometer sensor
    private val accelerometerListener = object : SensorEventListener {
        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

        override fun onSensorChanged(event: SensorEvent) {
            if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                val acceleration = event.values.clone()
                if (lastAcceleration != null) {
                    // Check the overall magnitude and direction of the movement
                    val movementMagnitude = calculateMovementMagnitude(acceleration)
                    if (movementMagnitude > shakeThreshold) {
                        if (isCameraSteady) {
                            isCameraSteady = false
                            showShakeWarning()
                        }
                    }
                }
                lastAcceleration = acceleration
            }
        }
    }

    // Calculate the overall magnitude of the movement
    private fun calculateMovementMagnitude(acceleration: FloatArray): Float {
        val deltaAcceleration = FloatArray(3)
        deltaAcceleration[0] = acceleration[0] - lastAcceleration!![0]
        deltaAcceleration[1] = acceleration[1] - lastAcceleration!![1]
        deltaAcceleration[2] = acceleration[2] - lastAcceleration!![2]

        return sqrt(
            (deltaAcceleration[0] * deltaAcceleration[0] +
                    deltaAcceleration[1] * deltaAcceleration[1] +
                    deltaAcceleration[2] * deltaAcceleration[2]).toDouble()
        ).toFloat()
    }

    private fun showShakeWarning() {
        if (!isShakeWarningVisible) {
            isShakeWarningVisible = true
            fragmentCameraBinding.bar.visibility = View.VISIBLE

            Handler().postDelayed({
                fragmentCameraBinding.bar.visibility = View.GONE
                isShakeWarningVisible = false
                isCameraSteady = true
            }, 1000) //
        }
    }

    // Start the accelerometer sensor
    private fun startAccelerometer() {
        val sensorManager =
            requireContext().getSystemService(Context.SENSOR_SERVICE) as SensorManager

        val accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        if (accelerometerSensor != null) {
            sensorManager.registerListener(
                accelerometerListener,
                accelerometerSensor,
                SensorManager.SENSOR_DELAY_NORMAL
            )
        } else {
            // Display a warning if the device does not have an accelerometer sensor
            Log.e(TAG, "Device doesn't have an accelerometer sensor.")
        }
    }

    // Stop the accelerometer sensor
    private fun stopAccelerometer() {
        val sensorManager =
            requireContext().getSystemService(Context.SENSOR_SERVICE) as SensorManager
        sensorManager.unregisterListener(accelerometerListener)
    }

    private fun processDetection(results: MutableList<Detection>) {
        if (!isCameraSteady) {
            clearCoordinateInfos()
            return
        }
        if (results.isEmpty()) {
            return
        }
        updateCoordinateInfo(results)
        updateNotSeenCounts(results)

        if (isContainerSeen) {
            fragmentCameraBinding.isContainerSeenBar.visibility = View.VISIBLE
            checkIsThrowingDone()
            updateIntersections(results)
        }
    }

    private fun clearCoordinateInfos() {
        containerCoordinateChangeInfo.firstSeenBox.setEmpty()
        containerCoordinateChangeInfo.lastSeenBox.setEmpty()
        containerCoordinateChangeInfo.seenCount = 0

        isContainerSeen = false
        fragmentCameraBinding.isContainerSeenBar.visibility = View.INVISIBLE

        packagingCoordinateChangeInfoMap.clear()
    }

    private fun updateCoordinateInfo(results: MutableList<Detection>) {
        for (result in results) {
            val label = result.categories[0].label
            if (label == CONTAINER_LABEL) {
                if (containerCoordinateChangeInfo.firstSeenBox.isEmpty) {
                    containerCoordinateChangeInfo.firstSeenBox = result.boundingBox
                } else {
                    val count = containerCoordinateChangeInfo.seenCount + 1
                    val weight = 1.0f / count.toFloat()

                    containerCoordinateChangeInfo.lastSeenBox.left =
                        (containerCoordinateChangeInfo.lastSeenBox.left + result.boundingBox.left) / 2.0f
                    containerCoordinateChangeInfo.lastSeenBox.right =
                        (containerCoordinateChangeInfo.lastSeenBox.right + result.boundingBox.right) / 2.0f
                    containerCoordinateChangeInfo.lastSeenBox.top =
                        (containerCoordinateChangeInfo.lastSeenBox.top + result.boundingBox.top) / 2.0f
                    containerCoordinateChangeInfo.lastSeenBox.bottom =
                        (containerCoordinateChangeInfo.lastSeenBox.bottom + result.boundingBox.bottom) / 2.0f

                }

                containerCoordinateChangeInfo.seenCount += 1
                isContainerSeen = true
            } else {
                val coordinateChangeInfo = packagingCoordinateChangeInfoMap.getOrPut(label) {
                    CoordinateChangeInfo(
                        title = result.categories[0].label,
                        firstSeenBox = result.boundingBox,
                        lastSeenBox = result.boundingBox,
                    )
                }
                coordinateChangeInfo.lastSeenBox = result.boundingBox
                coordinateChangeInfo.seenCount += 1
            }
        }
    }

    private fun updateNotSeenCounts(results: MutableList<Detection>) {
        val currentLabels = results.map { it.categories[0].label }.toSet()

        packagingCoordinateChangeInfoMap.forEach { (label, coordinateChangeInfo) ->
            coordinateChangeInfo.notSeenCount = if (label !in currentLabels) {
                coordinateChangeInfo.notSeenCount + 1
            } else {
                0
            }
        }
    }

    private fun checkIsThrowingDone() {
        val containerBox = containerCoordinateChangeInfo.lastSeenBox
        packagingCoordinateChangeInfoMap.values.filter { packagingCoordinate ->
            packagingCoordinate.isReadyThrowing && (!isPackagingInsideContainer(
                containerBox,
                packagingCoordinate.firstSeenBox
            ))
        }.forEach { packagingCoordinate ->
            if (packagingCoordinate.notSeenCount == 3) {
                val detection = detectionList.find { packagingCoordinate.title == it.id }
                if (detection == null) {
                    val newDetection =
                        packagingList.find { it.id == packagingCoordinate.title }!!
                    newDetection.count = 1
                    detectionList.add(newDetection)
                } else {
                    val detectionIndex = detectionList.indexOf(detection)
                    detectionList[detectionIndex].count += 1
                }
                packagingCoordinateChangeInfoMap.remove(packagingCoordinate.title)
            }
        }
        updateData()
    }

    private fun updateIntersections(results: MutableList<Detection>) {
        for (result in results) {
            val label = result.categories.getOrNull(0)?.label ?: continue

            if (label != CONTAINER_LABEL) {
                packagingCoordinateChangeInfoMap[label]?.let {
                    updateIsReadyThrowing(containerCoordinateChangeInfo, it)
                }
            }
        }
    }

    private fun updateIsReadyThrowing(
        containerCoordinateChangeInfo: CoordinateChangeInfo,
        packagingCoordinateChangeInfo: CoordinateChangeInfo
    ) {
        val containerBox = containerCoordinateChangeInfo.lastSeenBox
        val packagingBox = packagingCoordinateChangeInfo.lastSeenBox

        packagingCoordinateChangeInfo.isReadyThrowing =
            isPackagingInsideContainer(containerBox, packagingBox)
    }

    private fun isPackagingInsideContainer(containerBox: RectF, packagingBox: RectF): Boolean {
        return containerBox.top <= packagingBox.top
                && containerBox.left <= packagingBox.left
                && containerBox.bottom >= packagingBox.bottom
                && containerBox.right >= packagingBox.right
    }

    private fun readNamesFromFile(fileName: String = "packagingNameMap.txt") {
        try {
            val inputStream = requireActivity().assets.open(fileName)
            val reader = BufferedReader(InputStreamReader(inputStream))
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                val parts = line!!.split(", ")
                if (parts.size == 3) {
                    val type = PackagingTypes.valueOf(parts[0].uppercase())
                    val id = parts[1]
                    val name = parts[2]
                    val packaging = Packaging(type, id, name)
                    packagingList.add(packaging)
                }
            }
            reader.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    companion object {
        const val CONTAINER_LABEL = "atik_kutusu"
        const val CLOSENESS_THRESHOLD = 0.2
    }

    //------------------------------------------------------------------------------------//
//------------------------------------------------------------------------------------//
    private fun initBottomSheetControls() {
        // When clicked, lower detection score threshold floor
        fragmentCameraBinding.bottomSheetLayout.thresholdMinus.setOnClickListener {
            if (objectDetectorHelper.threshold >= 0.1) {
                objectDetectorHelper.threshold -= 0.1f
                updateControlsUi()
            }
        }

        // When clicked, raise detection score threshold floor
        fragmentCameraBinding.bottomSheetLayout.thresholdPlus.setOnClickListener {
            if (objectDetectorHelper.threshold <= 0.8) {
                objectDetectorHelper.threshold += 0.1f
                updateControlsUi()
            }
        }

        // When clicked, reduce the number of objects that can be detected at a time
        fragmentCameraBinding.bottomSheetLayout.maxResultsMinus.setOnClickListener {
            if (objectDetectorHelper.maxResults > 1) {
                objectDetectorHelper.maxResults--
                updateControlsUi()
            }
        }

        // When clicked, increase the number of objects that can be detected at a time
        fragmentCameraBinding.bottomSheetLayout.maxResultsPlus.setOnClickListener {
            if (objectDetectorHelper.maxResults < 5) {
                objectDetectorHelper.maxResults++
                updateControlsUi()
            }
        }

    }

    // Update the values displayed in the bottom sheet. Reset detector.
    private fun updateControlsUi() {
        fragmentCameraBinding.bottomSheetLayout.maxResultsValue.text =
            objectDetectorHelper.maxResults.toString()
        fragmentCameraBinding.bottomSheetLayout.thresholdValue.text =
            String.format("%.2f", objectDetectorHelper.threshold)

        // Needs to be cleared instead of reinitialized because the GPU
        // delegate needs to be initialized on the thread using it when applicable
        objectDetectorHelper.clearObjectDetector()
        fragmentCameraBinding.overlay.clear()
    }

    // Initialize CameraX, and prepare to bind the camera use cases
    private fun setUpCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener(
            {
                // CameraProvider
                cameraProvider = cameraProviderFuture.get()

                // Build and bind the camera use cases
                bindCameraUseCases()
            },
            ContextCompat.getMainExecutor(requireContext())
        )
    }

    // Declare and bind preview, capture and analysis use cases
    @SuppressLint("UnsafeOptInUsageError")
    private fun bindCameraUseCases() {

        // CameraProvider
        val cameraProvider =
            cameraProvider ?: throw IllegalStateException("Camera initialization failed.")

        // CameraSelector - makes assumption that we're only using the back camera
        val cameraSelector =
            CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_BACK).build()

        // Preview. Only using the 4:3 ratio because this is the closest to our models
        preview =
            Preview.Builder()
                .setTargetResolution(Size(900, 1200))
                //.setTargetAspectRatio(AspectRatio.RATIO_4_3)
                .setTargetRotation(fragmentCameraBinding.viewFinder.display.rotation)
                .build()

        // ImageAnalysis. Using RGBA 8888 to match how our models work
        imageAnalyzer =
            ImageAnalysis.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                .setTargetRotation(fragmentCameraBinding.viewFinder.display.rotation)
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(OUTPUT_IMAGE_FORMAT_RGBA_8888)
                .build()
                // The analyzer can then be assigned to the instance
                .also {
                    it.setAnalyzer(cameraExecutor) { image ->
                        if (!::bitmapBuffer.isInitialized) {
                            // The image rotation and RGB image buffer are initialized only once
                            // the analyzer has started running
                            bitmapBuffer = Bitmap.createBitmap(
                                image.width,
                                image.height,
                                Bitmap.Config.ARGB_8888
                            )
                        }
                        detectObjects(image)
                    }
                }

        // Must unbind the use-cases before rebinding them
        cameraProvider.unbindAll()

        try {
            // A variable number of use-cases can be passed here -
            // camera provides access to CameraControl & CameraInfo
            camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalyzer)

            // Attach the viewfinder's surface provider to preview use case
            preview?.setSurfaceProvider(fragmentCameraBinding.viewFinder.surfaceProvider)
        } catch (exc: Exception) {
            Log.e(TAG, "Use case binding failed", exc)
        }
    }

    private fun detectObjects(image: ImageProxy) {
        // Copy out RGB bits to the shared bitmap buffer
        image.use { bitmapBuffer.copyPixelsFromBuffer(image.planes[0].buffer) }

        val imageRotation = image.imageInfo.rotationDegrees
        // Pass Bitmap and rotation to the object detector helper for processing and detection
        objectDetectorHelper.detect(bitmapBuffer, imageRotation)
    }

    override fun onResults(
        results: MutableList<Detection>?,
        inferenceTime: Long,
        imageHeight: Int,
        imageWidth: Int
    ) {
        activity?.runOnUiThread {
            fragmentCameraBinding.fpsBar.text =
                String.format("%d fps", 1000 / inferenceTime)

            if (results != null) {
                processDetection(results)
            }

            drawBoundingBoxes(results, inferenceTime, imageHeight, imageWidth)
        }
    }

    private fun drawBoundingBoxes(
        results: MutableList<Detection>?,
        inferenceTime: Long,
        imageHeight: Int,
        imageWidth: Int){
        fragmentCameraBinding.bottomSheetLayout.inferenceTimeVal.text =
            String.format("%d ms", inferenceTime)

        fragmentCameraBinding.overlay.setResults(
             results ?: LinkedList<Detection>(),
             imageHeight,
             imageWidth
         )

         fragmentCameraBinding.overlay.invalidate()}

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        imageAnalyzer?.targetRotation = fragmentCameraBinding.viewFinder.display.rotation
    }

    override fun onError(error: String) {
        activity?.runOnUiThread {
            Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show()
        }
    }
}
