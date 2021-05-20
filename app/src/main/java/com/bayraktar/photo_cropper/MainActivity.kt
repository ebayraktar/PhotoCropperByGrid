package com.bayraktar.photo_cropper

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import android.view.ViewTreeObserver
import android.widget.Button
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import com.google.android.material.snackbar.Snackbar
import com.squareup.picasso.Picasso
import kotlinx.coroutines.*
import java.io.IOException


class MainActivity : AppCompatActivity(), ViewTreeObserver.OnGlobalLayoutListener {

    private lateinit var rlImage: RelativeLayout
    private lateinit var rlContent: RelativeLayout
    private lateinit var viewContent: View
    private lateinit var ivFileUpload: ImageView
    private lateinit var imageView: ImageView

    private lateinit var cl3x3: ConstraintLayout
    private lateinit var cl3x2: ConstraintLayout

    private lateinit var tv3x2: TextView
    private lateinit var tv3x3: TextView

    private lateinit var btnCrop: Button

    private var maxWidth: Int = 0
    private var maxHeight: Int = 0

    private var is3x3: Boolean = false

    private var dataUri: Uri? = null

    private val ratio: Float
        get() {
            return if (is3x3) 1f else .666f
        }

    private val completableJob = Job()
    private val coroutineScope = CoroutineScope(Dispatchers.IO + completableJob)

    private lateinit var progressBar: View

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        rlImage = findViewById(R.id.rlImage)
        rlContent = findViewById(R.id.rlContent)
        viewContent = findViewById(R.id.viewContent)
        cl3x3 = findViewById(R.id.cl3x3)
        cl3x2 = findViewById(R.id.cl3x2)

        tv3x2 = findViewById(R.id.tv3x2)
        tv3x3 = findViewById(R.id.tv3x3)

        ivFileUpload = findViewById(R.id.ivFileUpload)
        imageView = findViewById(R.id.imageView)
        btnCrop = findViewById(R.id.btnCrop)

        progressBar = findViewById(R.id.progressBar)

        findViewById<ImageView>(R.id.ivFileUpload).setOnClickListener {
            chooseImage()
        }

        tv3x2.setOnClickListener {
            addGrid(false)
        }
        tv3x3.setOnClickListener {
            addGrid(true)
        }

        findViewById<Button>(R.id.btnCrop).setOnClickListener {
            crop()
        }
        if (savedInstanceState != null && savedInstanceState.containsKey("DATA")) {
            dataUri = Uri.parse(savedInstanceState.getString("DATA", ""))
            is3x3 = savedInstanceState.getBoolean("GRID", false)
        }

        initLayout()
        addGrid(is3x3)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        if (dataUri != null)
            outState.putString("DATA", dataUri.toString())
        outState.putBoolean("GRID", is3x3)
        super.onSaveInstanceState(outState)
    }

    private fun addGrid(is3x3: Boolean) {

        this.is3x3 = is3x3
        if (is3x3) {
            cl3x3.visibility = View.VISIBLE
            cl3x2.visibility = View.GONE
            tv3x3.background =
                ContextCompat.getDrawable(this, R.drawable.bakcground_button_selected)
            tv3x2.background = ContextCompat.getDrawable(this, R.drawable.background_button_default)
        } else {
            cl3x3.visibility = View.GONE
            cl3x2.visibility = View.VISIBLE
            tv3x3.background = ContextCompat.getDrawable(this, R.drawable.background_button_default)
            tv3x2.background =
                ContextCompat.getDrawable(this, R.drawable.bakcground_button_selected)
        }
        initLayout()
    }

    private fun initLayout() {
        rlImage.viewTreeObserver.addOnGlobalLayoutListener(this)
    }

    private fun chooseImage() {
        val pickPhoto = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        resultLauncher.launch(pickPhoto)
    }

    private fun crop() {
        if (!isExternalStorageAvailable() || isExternalStorageReadOnly()) {
            AlertDialog.Builder(this)
                .setMessage("Kayıt yeri bulunamadı")
                .setPositiveButton("TAMAM", null)
                .create().show()
            return
        }

        if (imageView.drawable !is BitmapDrawable) {
            chooseImage()
            return
        }
        saveImageToUrl()
    }


    private fun cropImageByGrid(bitmap: Bitmap): List<Bitmap> {
        val width = bitmap.width
        val height = bitmap.height
        val bitmapList: ArrayList<Bitmap> = ArrayList()
        val horizontal = 3
        val vertical = if (is3x3) 3 else 2
        val freqWidth = width / horizontal
        val freqHeight = height / vertical
        for (i in 0 until horizontal) {
            for (j in 0 until vertical) {
                val startX = freqWidth * i
                val startY = freqHeight * j
                val tempBitmap = Bitmap.createBitmap(bitmap, startX, startY, freqWidth, freqHeight)
                bitmapList.add(tempBitmap)
            }
        }

        return bitmapList
    }

    private fun getResultBitmap(bm: Bitmap): Bitmap {
        val backgroundBitmap = getBackground()
        val resultBitmap = Bitmap.createBitmap(
            backgroundBitmap.width,
            backgroundBitmap.height,
            backgroundBitmap.config
        )
        val canvas = Canvas(resultBitmap)
        canvas.drawColor(Color.WHITE)
        canvas.drawBitmap(backgroundBitmap, Matrix(), null)
        canvas.drawBitmap(
            bm,
            (backgroundBitmap.width - bm.width).toFloat() / 2,
            (backgroundBitmap.height - bm.height).toFloat() / 2,
            Paint()
        )
        return resultBitmap
    }

    private fun getBackground(): Bitmap {
        return Bitmap.createBitmap(
            maxWidth,
            maxHeight,
            Bitmap.Config.ARGB_8888
        )
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
            .setNegativeButton("İPTAL") { _, _ ->
                run {
                    Snackbar.make(rlContent, "İzin alınamadı", Snackbar.LENGTH_LONG).show()
                }
            }
            .create().show()
    }

    private val requestPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                // Permission is granted. Continue the action or workflow in your
                // app.
                writeImageToFile()
            } else {
                // Explain to the user that the feature is unavailable because the
                // features requires a permission that the user has denied. At the
                // same time, respect the user's decision. Don't link to system
                // settings in an effort to convince the user to change their
                // decision.
                Snackbar.make(
                    rlContent,
                    "Yazma izni alınamadı. Fotoğraf kaydedilemez",
                    Snackbar.LENGTH_LONG
                ).show()
            }
        }

    private var resultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                // There are no request codes
                val data: Intent? = result.data
                dataUri = data!!.data
                Picasso.get()
                    .load(dataUri)
                    .resize(maxWidth, maxHeight)
                    .centerCrop()
                    .into(imageView)
            }
        }

    override fun onGlobalLayout() {
        rlImage.viewTreeObserver.removeOnGlobalLayoutListener(this)
//        rlImage.visibility = View.INVISIBLE
//        imageView.visibility = View.INVISIBLE
        val width: Int = viewContent.measuredWidth
        val height: Int = viewContent.measuredHeight
        if (width != 0 && height != 0) {
            val params = if (width > height) {
                val newHeight = height * ratio
                maxWidth = height
                maxHeight = newHeight.toInt()
                RelativeLayout.LayoutParams(height, newHeight.toInt())
            } else {
                val newHeight = width * ratio
                maxWidth = width
                maxHeight = newHeight.toInt()
                RelativeLayout.LayoutParams(width, newHeight.toInt())
            }
            params.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE)
            rlImage.layoutParams = params
        }
//        rlImage.visibility = View.VISIBLE
//        imageView.visibility = View.VISIBLE
        if (dataUri == null) return
        Picasso.get()
            .load(dataUri)
            .resize(maxWidth, maxHeight)
            .centerCrop()
            .into(imageView)
    }

    private fun isExternalStorageReadOnly(): Boolean {
        val extStorageState = Environment.getExternalStorageState()
        return Environment.MEDIA_MOUNTED_READ_ONLY == extStorageState
    }

    private fun isExternalStorageAvailable(): Boolean {
        val extStorageState = Environment.getExternalStorageState()
        return Environment.MEDIA_MOUNTED == extStorageState
    }

    private fun saveImageToUrl() {
        try {
            checkForPermission()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun checkForPermission() {
        when {
            ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED -> {
                // You can use the API that requires the permission.
                writeImageToFile()
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


    private fun writeImageToFile() {
        coroutineScope.launch(Dispatchers.IO) {
            withContext(Dispatchers.Main) {
                progressBar.visibility = View.VISIBLE
            }

            val bitmap = (imageView.drawable as BitmapDrawable).bitmap
            val bitmapList = cropImageByGrid(bitmap)
            for (tempBitmap in bitmapList) {
                initImageSaving(tempBitmap)
            }

            withContext(Dispatchers.Main) {
                progressBar.visibility = View.GONE
                Snackbar.make(rlContent, R.string.image_saved, Snackbar.LENGTH_LONG).show()
            }
        }
    }

    private fun initImageSaving(bitmap: Bitmap) {
        val relativeLocation = Environment.DIRECTORY_PICTURES
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, System.currentTimeMillis().toString())
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, relativeLocation)
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
        }

        val resolver = contentResolver

        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

        try {

            uri?.let { uri ->
                val stream = resolver.openOutputStream(uri)

                stream?.let {
                    if (!bitmap.compress(Bitmap.CompressFormat.JPEG, 80, it)) {
                        throw IOException("Failed to save bitmap.")
                    }
                } ?: throw IOException("Failed to get output stream.")

            } ?: throw IOException("Failed to create new MediaStore record")

        } catch (e: IOException) {
            if (uri != null) {
                resolver.delete(uri, null, null)
            }
            throw IOException(e)
        } finally {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
        }
    }

}