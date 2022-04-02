package com.bayraktar.photo_cropper.ui

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.view.ViewTreeObserver
import android.widget.RelativeLayout
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.bayraktar.photo_cropper.R
import com.bayraktar.photo_cropper.core.BaseActivity
import com.bayraktar.photo_cropper.core.markers.ViewEvent
import com.bayraktar.photo_cropper.databinding.ActivityMainBinding
import com.bayraktar.photo_cropper.repositories.ImageRepository
import com.squareup.picasso.Picasso
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : BaseActivity<ActivityMainBinding, MainViewModel>(
    layoutId = R.layout.activity_main
), ViewTreeObserver.OnGlobalLayoutListener {

    private var maxWidth: Int = 0
    private var maxHeight: Int = 0

    private var is3x3: Boolean = false

    private var dataUri: Uri? = null

    private val ratio: Float
        get() {
            return if (is3x3) 1f else .666f
        }

    @Inject
    lateinit var imageRepository: ImageRepository

    private val requestPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                // Permission is granted. Continue the action or workflow in your
                // app.
                writeToExternal()
            } else {
                // Explain to the user that the feature is unavailable because the
                // features requires a permission that the user has denied. At the
                // same time, respect the user's decision. Don't link to system
                // settings in an effort to convince the user to change their
                // decision.
                showError("Yazma izni alınamadı. Fotoğraf kaydedilemez")
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
                    .into(binding.imageView)
                viewModel.hasImage.postValue(true)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState != null && savedInstanceState.containsKey(DATA)) {
            dataUri = Uri.parse(savedInstanceState.getString(DATA, ""))
            is3x3 = savedInstanceState.getBoolean(GRID, false)
        }

        initLayout()
        addGrid(is3x3)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        if (dataUri != null)
            outState.putString(DATA, dataUri.toString())
        outState.putBoolean(GRID, is3x3)
        super.onSaveInstanceState(outState)
    }

    private fun addGrid(is3x3: Boolean) {
        this.is3x3 = is3x3
        if (is3x3) {
            binding.cl3x3.visibility = View.VISIBLE
            binding.cl3x2.visibility = View.GONE
            binding.tv3x3.background =
                ContextCompat.getDrawable(this, R.drawable.bakcground_button_selected)
            binding.tv3x2.background =
                ContextCompat.getDrawable(this, R.drawable.background_button_default)
        } else {
            binding.cl3x3.visibility = View.GONE
            binding.cl3x2.visibility = View.VISIBLE
            binding.tv3x3.background =
                ContextCompat.getDrawable(this, R.drawable.background_button_default)
            binding.tv3x2.background =
                ContextCompat.getDrawable(this, R.drawable.bakcground_button_selected)
        }
        initLayout()
    }

    private fun initLayout() {
        binding.rlImage.viewTreeObserver.addOnGlobalLayoutListener(this)
    }

    private fun chooseImage() {
        val pickPhoto = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        resultLauncher.launch(pickPhoto)
    }

    override fun onViewEvent(viewEvent: ViewEvent) {
        super.onViewEvent(viewEvent)
        when (viewEvent) {
            is MainViewEvent.WriteImage -> {
                writeToExternal()
            }
            is MainViewEvent.CheckPermission -> {
                checkForPermission()
            }
            is MainViewEvent.ChooseImage -> {
                chooseImage()
            }
            is MainViewEvent.Grid -> {
                addGrid(viewEvent.is3x3)
            }
        }
    }

    private fun showInContextUI() {
        AlertDialog.Builder(this)
            .setTitle("UYARI")
            .setMessage("Uygulama çalışmak için dosya okuma/yazma iznine ihtiyaç duymaktadır")
            .setPositiveButton("İZİN İSTE") { _, _ ->
                requestPermissionLauncher.launch(
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
            }
            .setNegativeButton("İPTAL") { _, _ ->
                showError("İzin alınamadı")
            }
            .create().show()
    }

    override fun onGlobalLayout() {
        binding.rlImage.viewTreeObserver.removeOnGlobalLayoutListener(this)
        val width: Int = binding.viewContent.measuredWidth
        val height: Int = binding.viewContent.measuredHeight
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
            binding.rlImage.layoutParams = params
        }
        if (dataUri == null) return
        Picasso.get()
            .load(dataUri)
            .resize(maxWidth, maxHeight)
            .centerCrop()
            .into(binding.imageView)
    }

    private fun checkForPermission() {
        when {
            ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED -> {
                // You can use the API that requires the permission.
                writeToExternal()
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

    private fun writeToExternal() {
        viewModel.writeImageToFile(
            (binding.imageView.drawable as BitmapDrawable).bitmap,
            is3x3
        )
    }

    companion object {
        private const val GRID = "grid"
        private const val DATA = "data"
    }
}