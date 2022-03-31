package com.bayraktar.photo_cropper.core

import android.app.Dialog
import android.graphics.Color
import android.os.Bundle
import android.widget.Toast
import androidx.annotation.LayoutRes
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.drawable.toDrawable
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.lifecycle.ViewModelProvider
import com.bayraktar.photo_cropper.R
import com.bayraktar.photo_cropper.BR
import com.bayraktar.photo_cropper.core.events.CommonViewEvent
import com.bayraktar.photo_cropper.core.markers.ViewEvent
import com.bayraktar.photo_cropper.ext.observe
import com.bayraktar.photo_cropper.ext.observeEvent
import com.bayraktar.photo_cropper.utils.findGenericSuperclass

/**
 * Created by emrebayraktar on 31,March,2022
 */
abstract class BaseActivity<B : ViewDataBinding, M : BaseViewModel>(
    @LayoutRes private val layoutId: Int
) : AppCompatActivity() {

    lateinit var binding: B
    lateinit var viewModel: M

    var progressDialog: Dialog? = null

    @Suppress("UNCHECKED_CAST")
    val viewModelClass: Class<M>
        get() = findGenericSuperclass<BaseActivity<B, M>>()
            ?.actualTypeArguments
            ?.getOrNull(1) as? Class<M>
            ?: throw IllegalStateException()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel = ViewModelProvider(this).get(viewModelClass)
        binding = DataBindingUtil.setContentView(this, layoutId)
        binding.setVariable(BR.viewModel, viewModel)
        binding.lifecycleOwner = this
        binding.executePendingBindings()

        observe(viewModel.loading, ::onLoadingStateChanged)
        observeEvent(viewModel.viewEvent, ::onViewEvent)
    }

    private fun createProgressDialog() {
        progressDialog = Dialog(this)
        progressDialog?.let {
            it.setContentView(
                R.layout.layout_progress_dialog
            )
            it.window?.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
            it.setCancelable(false)
            it.setCanceledOnTouchOutside(false)
        }
    }

    fun showLoading() {
        if (progressDialog == null) {
            createProgressDialog()
        }

        progressDialog?.show()
    }

    fun hideLoading() {
        progressDialog?.dismiss()
    }

    fun showSuccess(@StringRes message: Int) =
        showMessage(CommonViewEvent.MessageType.SUCCESS, message)

    fun showSuccess(message: String) = showMessage(CommonViewEvent.MessageType.SUCCESS, message)

    fun showError(@StringRes message: Int) = showMessage(CommonViewEvent.MessageType.ERROR, message)

    fun showError(message: String) = showMessage(CommonViewEvent.MessageType.ERROR, message)

    fun showMessage(type: CommonViewEvent.MessageType, @StringRes message: Int) {
        showMessage(type, getString(message))
    }

    fun showMessage(type: CommonViewEvent.MessageType, message: String) {
        // TODO: 31.03.2022 DefaultSnackBar.build(this, type, message).show()
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    fun showToast(icon: Int, text: String) {
        // TODO: 31.03.2022 DefaultToast.build(this, icon, text).show()
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show()
    }

    protected open fun onLoadingStateChanged(loading: Boolean) {
        if (loading) {
            showLoading()
        } else {
            hideLoading()
        }
    }

    protected open fun onViewEvent(viewEvent: ViewEvent) {
        when (viewEvent) {
            is CommonViewEvent.ShowMessage -> showMessage(viewEvent.type, viewEvent.message)
            is CommonViewEvent.ShowMessageRes -> showMessage(viewEvent.type, viewEvent.message)
        }
    }
}