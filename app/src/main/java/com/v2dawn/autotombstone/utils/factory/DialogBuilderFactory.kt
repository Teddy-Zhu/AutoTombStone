@file:Suppress("unused", "OPT_IN_USAGE", "EXPERIMENTAL_API_USAGE")

package com.v2dawn.autotombstone.utils.factory

import android.app.Dialog
import android.app.TimePickerDialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.viewbinding.ViewBinding
import com.github.zawadz88.materialpopupmenu.popupMenu
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.highcapable.yukihookapi.annotation.CauseProblemsApi
import com.highcapable.yukihookapi.hook.factory.method
import com.highcapable.yukihookapi.hook.type.android.LayoutInflaterClass
import com.v2dawn.autotombstone.R

/**
 * 显示时间选择对话框
 * @param timeSet 当前时间 - 不写将使用当前时间格式：HH:mm
 * @param result 回调 - 小时与分钟 HH:mm
 */
fun Context.showTimePicker(timeSet: String = "", result: (String) -> Unit) =
    TimePickerDialog(
        this,
        { _, h, m -> result("${h.autoZero}:${m.autoZero}") },
        timeSet.hour,
        timeSet.minute,
        true
    ).show()

/**
 * 构造 [VB] 自定义 View 对话框
 * @param initiate 对话框方法体
 */
@JvmName(name = "showDialog-VB")
inline fun <reified VB : ViewBinding> Context.showDialog(initiate: DialogBuilder<VB>.() -> Unit) =
    DialogBuilder<VB>(context = this, VB::class.java).apply(initiate).show()

/**
 * 构造对话框
 * @param initiate 对话框方法体
 */
inline fun Context.showDialog(initiate: DialogBuilder<*>.() -> Unit) =
    DialogBuilder<ViewBinding>(context = this).apply(initiate).show()

/**
 * 对话框构造器
 * @param context 实例
 * @param bindingClass [ViewBinding] 的 [Class] 实例 or null
 */
class DialogBuilder<VB : ViewBinding>(
    val context: Context,
    private val bindingClass: Class<*>? = null
) {

    private var instanceAndroidX: androidx.appcompat.app.AlertDialog.Builder? = null // 实例对象
    private var instanceAndroid: android.app.AlertDialog.Builder? = null // 实例对象

    private var dialogInstance: Dialog? = null // 对话框实例
    private var customLayoutView: View? = null // 自定义布局

    /**
     * 获取 [DialogBuilder] 绑定布局对象
     * @return [VB]
     */
    val binding by lazy {
        bindingClass?.method {
            name = "inflate"
            param(LayoutInflaterClass)
        }?.get()?.invoke<VB>(LayoutInflater.from(context))?.apply {
            customLayoutView = root
        } ?: error("This dialog maybe not a custom view dialog")
    }

    /**
     * 是否需要使用 AndroidX 风格对话框
     * @return [Boolean]
     */
    private val isUsingAndroidX
        get() = runCatching { context is AppCompatActivity }.getOrNull() ?: false

    init {
        if (isUsingAndroidX)
            runInSafe { instanceAndroidX = MaterialAlertDialogBuilder(context) }
        else runInSafe {
            instanceAndroid = android.app.AlertDialog.Builder(
                context,
                android.R.style.Theme_Material_Light_Dialog
            )
        }
    }

    /** 设置对话框不可关闭 */
    fun noCancelable() {
        if (isUsingAndroidX)
            runInSafe { instanceAndroidX?.setCancelable(false) }
        else runInSafe { instanceAndroid?.setCancelable(false) }
    }

    /** 设置对话框标题 */
    var title
        get() = ""
        set(value) {
            if (isUsingAndroidX)
                runInSafe { instanceAndroidX?.setTitle(value) }
            else runInSafe { instanceAndroid?.setTitle(value) }
        }

    /** 设置对话框消息内容 */
    var msg
        get() = ""
        set(value) {
            if (isUsingAndroidX)
                runInSafe { instanceAndroidX?.setMessage(value) }
            else runInSafe { instanceAndroid?.setMessage(value) }
        }

    /** 设置进度条对话框消息内容 */
    var progressContent
        get() = ""
        set(value) {
            if (customLayoutView == null)
                customLayoutView = LinearLayout(context).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER or Gravity.START
                    addView(ProgressBar(context))
                    addView(View(context).apply {
                        layoutParams = ViewGroup.LayoutParams(20.dp(context), 5)
                    })
                    addView(TextView(context).apply {
                        tag = "progressContent"
                        text = value
                    })
                    setPadding(20.dp(context), 20.dp(context), 20.dp(context), 20.dp(context))
                }
            else customLayoutView?.findViewWithTag<TextView>("progressContent")?.text = value
        }

    /**
     * 设置对话框确定按钮
     * @param text 按钮文本内容
     * @param callback 点击事件
     */
    fun confirmButton(text: String = "确定", callback: () -> Unit = {}) {
        if (isUsingAndroidX)
            runInSafe { instanceAndroidX?.setPositiveButton(text) { _, _ -> callback() } }
        else runInSafe { instanceAndroid?.setPositiveButton(text) { _, _ -> callback() } }
    }

    /**
     * 设置对话框取消按钮
     * @param text 按钮文本内容
     * @param callback 点击事件
     */
    fun cancelButton(text: String = "取消", callback: () -> Unit = {}) {
        if (isUsingAndroidX)
            runInSafe { instanceAndroidX?.setNegativeButton(text) { _, _ -> callback() } }
        else runInSafe { instanceAndroid?.setNegativeButton(text) { _, _ -> callback() } }
    }

    /**
     * 设置对话框第三个按钮
     * @param text 按钮文本内容
     * @param callback 点击事件
     */
    fun neutralButton(text: String = "更多", callback: () -> Unit = {}) {
        if (isUsingAndroidX)
            runInSafe { instanceAndroidX?.setNeutralButton(text) { _, _ -> callback() } }
        else runInSafe { instanceAndroid?.setNeutralButton(text) { _, _ -> callback() } }
    }

    /** 取消对话框 */
    fun cancel() = dialogInstance?.cancel()

    /** 显示对话框 */
    @CauseProblemsApi
    fun show() {
        /** 若当前自定义 View 的对话框没有调用 [binding] 将会对其手动调用一次以确保显示布局 */
        if (bindingClass != null) binding
        if (isUsingAndroidX) runInSafe {
            instanceAndroidX?.create()?.apply {
                customLayoutView?.let { setView(it) }
                dialogInstance = this
            }?.show()
        } else runInSafe {
            instanceAndroid?.create()?.apply {
                customLayoutView?.let { setView(it) }
                window?.setBackgroundDrawable(
                    GradientDrawable(
                        GradientDrawable.Orientation.TOP_BOTTOM,
                        intArrayOf(Color.WHITE, Color.WHITE)
                    ).apply {
                        shape = GradientDrawable.RECTANGLE
                        gradientType = GradientDrawable.LINEAR_GRADIENT
                        cornerRadius = 15.dpFloat(this@DialogBuilder.context)
                    })
                dialogInstance = this
            }?.show()
        }
    }
}

fun Context.showOperatePopup(view: View, cb: (index: Int, resId: Int) -> Unit) {
    val operateList = listOf(
        R.string.control, R.string.uncontrol, R.string.stop_services,
        R.string.active, R.string.inactive, R.string.kill_app, R.string.freeze_app,
        R.string.unfreeze_app
    )
    val popupMenu = popupMenu {
        dropdownGravity = Gravity.END
        section {
            for ((index, value) in operateList.withIndex()) {
                item {
                    labelRes = value
                    callback = {
                        cb(index, value)
                    }
                }
            }
        }
    }

    popupMenu.show(this, view)
}

public open class PopUpItem(val text: String)

fun <T : PopUpItem> Context.showCommonPopup(
    view: View,
    items: List<T>,
    selected: Int = -1,
    cb: (index: Int, t: T) -> Unit
) {
    val popupMenu = popupMenu {
        dropdownGravity = Gravity.END
        section {
            items.forEachIndexed { index, t ->
                item {
                    label = t.text
                    icon = if (selected == index) R.drawable.ic_right else 0
                    callback = { //optional
                        cb(index, t)
                    }
                }

            }
        }
    }

    popupMenu.show(this, view)
}

fun Context.showPopup(view: View, currentIndex: Int, cb: (index: Int) -> Unit) {
    val popupMenu = popupMenu {
        dropdownGravity = Gravity.END
        section {
            item {
                labelRes = R.string.follow_process
                icon = if (currentIndex == 0) R.drawable.ic_right else 0
                callback = { //optional
                    cb(0)
                }
            }
            item {
                labelRes = R.string.force_process
                icon = if (currentIndex == 1) R.drawable.ic_right else 0
                callback = { //optional
                    cb(1)
                }
            }
            item {
                labelRes = R.string.ignore_process
                icon = if (currentIndex == 2) R.drawable.ic_right else 0
                callback = { //optional
                    cb(2)
                }
            }
        }
    }

    popupMenu.show(this, view)
}