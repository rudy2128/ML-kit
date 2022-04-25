package com.google.codelab.mlkit.camera

import android.content.Intent
import android.content.pm.PackageManager
import android.gesture.GestureLibraries.fromFile
import android.graphics.Bitmap
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.codelab.mlkit.MainActivity
import com.google.codelab.mlkit.R
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class CamActivity : AppCompatActivity() {
    private lateinit var btnTake: ImageButton
    private lateinit var vPreviewView: PreviewView
    private var imageCapture: ImageCapture?=null
    private lateinit var outputDirectory: File
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cam)
        vPreviewView = findViewById(R.id.viewfinder)
        btnTake = findViewById(R.id.btn_takePicture)
        outputDirectory = getOutputDirectory()

        if (allPermissionGranted()){
            startCamera()

        }else{
            ActivityCompat.requestPermissions(this,
                Constants.REQUIRED_PERMISSIONS,Constants.REQUEST_CODE_PERMISSIONS)
        }

        btnTake.setOnClickListener {

            takePhoto()

        }
    }
    private fun getOutputDirectory():File{
        val mediaDir =
            externalMediaDirs.firstOrNull()?.let { mFile->
                File(mFile,resources.getString(R.string.app_name)).apply {
                    mkdirs()
                }

            }
        return if(mediaDir !=null && mediaDir.exists())
            mediaDir else filesDir
    }



    private fun takePhoto() {
        val ring = MediaPlayer.create(applicationContext,R.raw.cam)
        ring.start()
        val imageCapture = imageCapture?:return
        val photoFile = File(
            outputDirectory,
            SimpleDateFormat(Constants.FILE_NAME_FORMAT,
                Locale.getDefault())
                .format(System
                    .currentTimeMillis())+ ".jpg")

        val outputOption = ImageCapture
            .OutputFileOptions
            .Builder(photoFile)
            .build()

        imageCapture.takePicture(
            outputOption, ContextCompat.getMainExecutor(this@CamActivity),
            object : ImageCapture.OnImageSavedCallback{
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    val savedUri = Uri.fromFile(photoFile)
                    val msg = "Photo saved"
                    Toast.makeText(this@CamActivity,"$msg $savedUri",Toast.LENGTH_SHORT).show()
                    intent.putExtra("ImgPerson",savedUri.toString())
                    intent.putExtra("Image",photoFile)
                    setResult(RESULT_OK, intent)
                    finish()
                }

                override fun onError(exception: ImageCaptureException){
                    Log.e(Constants.TAG,"onError:${exception.message}",exception)
                }

            }


        )
    }


    private fun startCamera(){
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({

            val  cameraProvider:ProcessCameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder()
                .build()
                .also {mPreview->
                    mPreview.setSurfaceProvider(
                        vPreviewView.surfaceProvider
                    )
                }
            imageCapture = ImageCapture.Builder()
                .build()

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this,cameraSelector,preview,imageCapture
                )

            }catch (e:Exception){
                Log.d(Constants.TAG,"Camera Fail:",e)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == Constants.REQUEST_CODE_PERMISSIONS){
            if (allPermissionGranted()){
                startCamera()

            }else{
                Toast.makeText(this,"Permission not granted by user", Toast.LENGTH_SHORT).show()
                finish()
            }
        }

    }

    private fun allPermissionGranted()=
        Constants.REQUIRED_PERMISSIONS.all{
            ContextCompat.checkSelfPermission(
                baseContext,it
            ) == PackageManager.PERMISSION_GRANTED
        }

}
