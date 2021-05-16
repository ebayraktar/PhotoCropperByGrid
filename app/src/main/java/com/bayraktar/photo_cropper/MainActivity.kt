package com.bayraktar.photo_cropper

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.ViewTreeObserver
import android.widget.Button
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat


class MainActivity : AppCompatActivity(), ViewTreeObserver.OnGlobalLayoutListener {

    private lateinit var rlImage: RelativeLayout
    private lateinit var ivFileUpload: ImageView
    private lateinit var imageView: ImageView
    private lateinit var btnCrop: Button

    private var maxWidth: Int = 0
    private var maxHeight: Int = 0

    private var widthRatio: Float = 0f
    private var heightRatio: Float = 0f

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        rlImage = findViewById(R.id.rlImage)
        ivFileUpload = findViewById(R.id.ivFileUpload)
        imageView = findViewById(R.id.imageView)
        btnCrop = findViewById(R.id.btnCrop)

        findViewById<ImageView>(R.id.ivFileUpload).setOnClickListener {
            chooseImage()
        }

        findViewById<Button>(R.id.btnCrop).setOnClickListener {
            crop()
        }
        init()
    }

    private fun init() {
//        if (imageView.width != 0 && imageView.height != 0)

        rlImage.viewTreeObserver.addOnGlobalLayoutListener(this)
    }

    override fun onStart() {
        super.onStart()
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED -> {
                // You can use the API that requires the permission.
            }
            shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE) -> {
                // In an educational UI, explain to the user why your app requires this
                // permission for a specific feature to behave as expected. In this UI,
                // include a "cancel" or "no thanks" button that allows the user to
                // continue using your app without granting the permission.
                showInContextUI()
            }
            else -> {
                // You can directly ask for the permission.
                // The registered ActivityResultCallback gets the result of this request.
                requestPermissionLauncher.launch(
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
            }
        }
    }

    private fun chooseImage() {
        val pickPhoto = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        resultLauncher.launch(pickPhoto)
    }

    private fun crop() {
        val bitmapList = cropImageByGrid((imageView.drawable as BitmapDrawable).bitmap)

        val imageRepository = ImageRepository(this)

        var message = "HATA OLUŞTU"
        if (bitmapList.isNotEmpty()) {
            (bitmapList.indices).forEach { i ->
                imageRepository.save(bitmapList[i])
            }
            message = "İşlem başarılı"
        }

        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()

    }

    private fun showInContextUI() {
        AlertDialog.Builder(this)
            .setTitle("UYARI")
            .setMessage("Uygulama çalışmak için dosya okuma/yazma iznine ihtiyaç duymaktadır")
            .setPositiveButton("İZİN İSTE") { _, _ ->
                run {
                    requestPermissionLauncher.launch(
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                    )
                }
            }
    }

    private val requestPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                // Permission is granted. Continue the action or workflow in your
                // app.
                Log.d("TAG", ": Granted")
            } else {
                // Explain to the user that the feature is unavailable because the
                // features requires a permission that the user has denied. At the
                // same time, respect the user's decision. Don't link to system
                // settings in an effort to convince the user to change their
                // decision.
                Toast.makeText(this, "İZİN ALINAMADI", Toast.LENGTH_SHORT).show()
            }
        }

    private var resultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                // There are no request codes
                val data: Intent? = result.data
                imageView.setImageURI(data!!.data)
                imageView.setImageBitmap(getResultBitmap((imageView.drawable as BitmapDrawable).bitmap))
            }
        }

    private fun cropImageByGrid(bitmap: Bitmap): List<Bitmap> {
        val width = bitmap.width
        val height = bitmap.height
        val bitmapList: ArrayList<Bitmap> = ArrayList()
        val freqWidth = width / 3
        val freqHeight = height / 2
        for (i in 0..2) {
            for (j in 0..1) {
                val startX = freqWidth * i
                val startY = freqHeight * j
                val tempBitmap = Bitmap.createBitmap(bitmap, startX, startY, freqWidth, freqHeight)
                bitmapList.add(tempBitmap)
            }
        }

        return bitmapList
    }

    private fun getResultBitmap(bm: Bitmap): Bitmap {
        val backgroundBitmap = Bitmap.createBitmap(
            maxWidth,
            maxHeight,
            Bitmap.Config.ARGB_8888
        )
        val bitmapToDrawInTheCenter = scaleBitmap(bm)
        val resultBitmap = Bitmap.createBitmap(
            backgroundBitmap.width,
            backgroundBitmap.height,
            backgroundBitmap.config
        )
        val canvas = Canvas(resultBitmap)
        canvas.drawColor(Color.WHITE)
        canvas.drawBitmap(backgroundBitmap, Matrix(), null)
        canvas.drawBitmap(
            bitmapToDrawInTheCenter,
            (backgroundBitmap.width - bitmapToDrawInTheCenter.width).toFloat() / 2,
            (backgroundBitmap.height - bitmapToDrawInTheCenter.height).toFloat() / 2,
            Paint()
        )
        return resultBitmap
    }

    private fun scaleBitmap(bm: Bitmap): Bitmap {
        var width = bm.width
        var height = bm.height
        when {
            width > height -> {
                // landscape
                val ratio = width.toFloat() / maxWidth
                width = maxWidth
                height = (height / ratio).toInt()
            }
            height > width -> {
                // portrait
                val ratio = height.toFloat() / maxHeight
                height = maxHeight
                width = (width / ratio).toInt()
            }
            else -> {
                // square
                height = maxHeight
                width = maxWidth
            }
        }
        return Bitmap.createScaledBitmap(bm, width, height, true)
    }


    override fun onGlobalLayout() {
        rlImage.viewTreeObserver.removeOnGlobalLayoutListener(this)
        val width: Int = rlImage.measuredWidth
        val height: Int = rlImage.measuredHeight
        if (width != 0 && height != 0) {
            val params = if (width > height) {
                val newHeight = height * .666
                maxWidth = height
                maxHeight = newHeight.toInt()
                RelativeLayout.LayoutParams(height, newHeight.toInt())
            } else {
                val newHeight = width * .666
                maxWidth = width
                maxHeight = newHeight.toInt()
                RelativeLayout.LayoutParams(width, newHeight.toInt())
            }
            params.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE)
            rlImage.layoutParams = params
        }
    }

}