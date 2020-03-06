package com.frc1678.pit_collection

import android.app.ActivityOptions
import android.content.Intent
import android.graphics.Matrix
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.util.Size
import android.view.Surface
import android.view.TextureView
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.lifecycle.LifecycleOwner
import kotlinx.android.synthetic.main.camera_preview_activity.*
import java.io.File
import java.util.*
import java.util.concurrent.Executors

class CameraActivity : AppCompatActivity(), LifecycleOwner {
    private lateinit var teamNum: String
    private var pictureNumber = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.camera_preview_activity)

        toolbarText(actionBar, supportActionBar, this)

        createSpinner(picture_type, R.array.picture_types, this)

        teamNum = intent.getStringExtra("teamNumber")!!.toString()

        finishButton(teamNum)

        viewFinder = findViewById(R.id.view_finder)
        viewFinder.post { startCamera(teamNum) }

        // Every time the provided texture view changes, recompute layout
        viewFinder.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            updateTransform()
        }
    }

    private fun putExtras(intentToNextActivity: Intent): Intent {
        intentToNextActivity.putExtra(
            "teamNumber", teamNum
        ).putExtra("can_cross_trench", intent.getBooleanExtra("can_cross_trench", false))
            .putExtra("has_ground_intake", intent.getBooleanExtra("has_ground_intake", false))
            .putExtra("drivetrain_pos", intent.getIntExtra("drivetrain_pos", -1))
            .putExtra("drivetrain_motor_pos", intent.getIntExtra("drivetrain_motor_pos", -1))
            .putExtra("num_motors", intent.getIntExtra("num_motors", 0))
            .putExtra("after_camera", true)
        return intentToNextActivity
    }

    private fun finishButton(teamNum: String) {
        btn_return.setOnClickListener {
            startActivity(
                putExtras(Intent(this, CollectionObjectiveActivity::class.java)),
                ActivityOptions.makeSceneTransitionAnimation(
                    this,
                    btn_return, "proceed_button"
                ).toBundle()
            )
        }
    }

    override fun onBackPressed() {}

    private val executor = Executors.newSingleThreadExecutor()
    private lateinit var viewFinder: TextureView

    private fun startCamera(teamNum: String) {
        // Create configuration object for the viewfinder use case
        val previewConfig = PreviewConfig.Builder().apply {
            setTargetResolution(Size(640, 480))
        }.build()


        // Build the viewfinder use case
        val preview = Preview(previewConfig)

        // Every time the viewfinder is updated, recompute layout
        preview.setOnPreviewOutputUpdateListener {

            // To update the SurfaceTexture, we have to remove it and re-add it
            val parent = viewFinder.parent as ViewGroup
            parent.removeView(viewFinder)
            parent.addView(viewFinder, 0)

            viewFinder.surfaceTexture = it.surfaceTexture
            updateTransform()
        }

        // Create configuration object for the image capture use case
        val imageCaptureConfig = ImageCaptureConfig.Builder()
            .apply {
                // We don't set a resolution for image capture; instead, we
                // select a capture mode which will infer the appropriate
                // resolution based on aspect ration and requested mode
                setCaptureMode(ImageCapture.CaptureMode.MIN_LATENCY)
            }.build()

        // Build the image capture use case and attach button click listener
        val imageCapture = ImageCapture(imageCaptureConfig)

        capture_button.setOnClickListener {
            val pictureType = picture_type.selectedItem.toString().toLowerCase(Locale.US)
            var fileName = "${teamNum}_${formatPictureType(pictureType)}"
            val file = File(
                "/storage/emulated/0/${Environment.DIRECTORY_DOWNLOADS}/",
                "$fileName.jpg"
            )

            imageCapture.takePicture(file, executor,
                object : ImageCapture.OnImageSavedListener {
                    override fun onError(
                        imageCaptureError: ImageCapture.ImageCaptureError,
                        message: String,
                        exc: Throwable?
                    ) {
                        val msg = "Photo capture failed: $message"
                        Log.e("CameraXApp", msg, exc)
                        viewFinder.post {
                            Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
                        }
                    }

                    override fun onImageSaved(file: File) {
                        this@CameraActivity.runOnUiThread(object : Runnable {
                            override fun run() {
                                startActivity(
                                    putExtras(
                                        Intent(
                                            this@CameraActivity,
                                            CameraConfirmationActivity::class.java
                                        )
                                    ).putExtra("fileName", file.toString()),
                                    ActivityOptions.makeSceneTransitionAnimation(
                                        this@CameraActivity,
                                        capture_button, "proceed_button"
                                    ).toBundle()
                                )
                            }
                        })
                    }
                })
        }

        // Bind use cases to lifecycle
        // If Android Studio complains about "this" being not a LifecycleOwner
        // try rebuilding the project or updating the appcompat dependency to
        // version 1.1.0 or higher.
        CameraX.bindToLifecycle(this, preview, imageCapture)
    }

    //deletes the space in the "full robot" picture type, replacing it with an "_"
    private fun formatPictureType(pictureType: String): String {
        var pictureName = ""
        if (pictureType == "full robot") {
            pictureName = "full_robot"
            return pictureName

        } else {
            return pictureType
        }
    }

    private fun updateTransform() {
        val matrix = Matrix()

        // Compute the center of the view finder
        val centerX = viewFinder.width / 2f
        val centerY = viewFinder.height / 2f

        // Correct preview output to account for display rotation
        val rotationDegrees = when (viewFinder.display.rotation) {
            Surface.ROTATION_0 -> 0
            Surface.ROTATION_90 -> 90
            Surface.ROTATION_180 -> 180
            Surface.ROTATION_270 -> 270
            else -> return
        }
        matrix.postRotate(-rotationDegrees.toFloat(), centerX, centerY)

        // Finally, apply transformations to our TextureView
        viewFinder.setTransform(matrix)
    }
}