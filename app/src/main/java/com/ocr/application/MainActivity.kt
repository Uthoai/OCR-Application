package com.ocr.application

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.speech.tts.TextToSpeech
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.ocr.application.databinding.ActivityMainBinding
import java.util.Locale

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    private val PERMISSION_REQUEST_CODE = 100
    private val GALLERY_REQUEST_CODE = 201

    private var bitmap: Bitmap? = null

    private var textToSpeech: TextToSpeech? = null

    private var permissionList = if (Build.VERSION.SDK_INT >= 33){
        arrayOf(
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_VIDEO,
            Manifest.permission.READ_MEDIA_AUDIO,
        )
    }else{
        arrayOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        enableEdgeToEdge()
        setContentView(binding.root)

        binding.btnChoosePicture.setOnClickListener {
            if(checkPermission()){
                openGallery()
            }
        }

        textToSpeech = TextToSpeech(applicationContext, TextToSpeech.OnInitListener {
            if (it == TextToSpeech.SUCCESS) {
                textToSpeech?.language = Locale.US
            }
        })

        binding.btnSpeech.setOnClickListener {
            speakOutText()
        }

    }

    private fun speakOutText() {
        textToSpeech?.speak(binding.textView.text.toString(), TextToSpeech.QUEUE_FLUSH, null, null)
    }

    private fun detectText() {
        val InputImage = InputImage.fromBitmap(bitmap!!, 0)
        val textRecognition = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        textRecognition.process(InputImage)
            .addOnSuccessListener { visionText ->
                val text = visionText.text
                binding.textView.text = text
            }
    }

    @SuppressLint("IntentReset")
    private fun openGallery() {
        val galleryIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        galleryIntent.type = "image/*"
        startActivityForResult(galleryIntent, GALLERY_REQUEST_CODE)
    }

    private fun checkPermission(): Boolean {
        val needPermissionList = mutableListOf<String>()
        for (permission in permissionList) {
            if (ContextCompat.checkSelfPermission(this,permission) != PackageManager.PERMISSION_GRANTED) {
                needPermissionList.add(permission)
            }
        }
        if (needPermissionList.isNotEmpty()) {
            requestPermissions(needPermissionList.toTypedArray(),PERMISSION_REQUEST_CODE)
            return false
        }
        return true
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == PERMISSION_REQUEST_CODE){
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                openGallery()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == GALLERY_REQUEST_CODE && resultCode == RESULT_OK){
            data?.data?.let {
                bitmap = MediaStore.Images.Media.getBitmap(contentResolver,it) as Bitmap
                //binding.ivPlaceHolder.setImageBitmap(bitmap)
                Glide.with(this)
                    .load(bitmap)
                    .into(binding.ivPlaceHolder)

                detectText()
            }
        }
    }

    override fun onPause() {
        if (!textToSpeech?.isSpeaking!!){
            super.onPause()
        }
    }

    override fun onDestroy() {
        textToSpeech?.stop()
        textToSpeech?.shutdown()
        super.onDestroy()
    }

}