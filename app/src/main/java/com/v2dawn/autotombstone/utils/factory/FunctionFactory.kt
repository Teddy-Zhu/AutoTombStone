@file:Suppress("DEPRECATION", "PrivateApi", "unused", "ObsoleteSdkInt")

package com.v2dawn.autotombstone.utils.factory

import android.app.Activity
import android.app.Notification
import android.app.Service
import android.app.WallpaperManager
import android.app.WallpaperManager.FLAG_SYSTEM
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.net.ConnectivityManager
import android.net.Uri
import android.os.*
import android.provider.Settings
import android.util.Base64
import android.view.View
import android.widget.Toast
import androidx.core.app.ActivityOptionsCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.getSystemService
import androidx.core.util.Pair
import com.android.server.AtsConfigService
import com.google.android.material.snackbar.Snackbar
import com.highcapable.yukihookapi.hook.factory.*
import com.highcapable.yukihookapi.hook.type.java.StringType
import com.highcapable.yukihookapi.hook.xposed.application.ModuleApplication.Companion.appContext
import com.highcapable.yukihookapi.hook.xposed.prefs.YukiHookModulePrefs
import com.topjohnwu.superuser.Shell
import com.v2dawn.autotombstone.config.ConfigConst
import com.v2dawn.autotombstone.hook.tombstone.support.ClassEnum
import com.v2dawn.autotombstone.hook.tombstone.support.atsLogE
import com.v2dawn.autotombstone.model.AppItemData
import com.v2dawn.autotombstone.utils.tool.SystemTool
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.*


/**
 * 系统深色模式是否开启
 * @return [Boolean] 是否开启
 */
val isSystemInDarkMode get() = appContext.isSystemInDarkMode

/**
 * 系统深色模式是否没开启
 * @return [Boolean] 是否开启
 */
inline val isNotSystemInDarkMode get() = !isSystemInDarkMode

/**
 * 系统深色模式是否开启
 * @return [Boolean] 是否开启
 */
val Context.isSystemInDarkMode get() = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

/**
 * 系统深色模式是否没开启
 * @return [Boolean] 是否开启
 */
inline val Context.isNotSystemInDarkMode get() = !isSystemInDarkMode

/**
 * 系统版本是否高于或等于 Android 12
 * @return [Boolean] 是否符合条件
 */
inline val isUpperOfAndroidS get() = Build.VERSION.SDK_INT > Build.VERSION_CODES.R

/**
 * 系统版本是否低于 Android 9
 * @return [Boolean] 是否符合条件
 */
inline val isLowerAndroidP get() = Build.VERSION.SDK_INT < Build.VERSION_CODES.P

/**
 * 当前设备是否是 ColorOS 定制 Android 系统
 * @return [Boolean] 是否符合条件
 */
val isColorOS by lazy { ("oppo.R").hasClass || ("com.color.os.ColorBuild").hasClass || ("oplus.R").hasClass }

/**
 * 当前设备是否不是 ColorOS 定制 Android 系统
 * @return [Boolean] 是否符合条件
 */
inline val isNotColorOS get() = !isColorOS

/**
 * 当前设备是否为 RealmeUI
 * @return [Boolean] 是否符合条件
 */
val isRealmeUI
    get() = safeOfFalse {
        val query = appContext.contentResolver.query(
            Uri.parse("content://com.oplus.customize.coreapp.configmanager.configprovider.AppFeatureProvider")
                .buildUpon()
                .appendPath("app_feature")
                .build(), null, "featurename=?", arrayOf("com.android.launcher.device_rm"), null
        )
        val isExist = query != null && query.count > 0
        query?.close()
        isExist
    }

/**
 * 获取 ColorOS 完整版本
 * @return [String]
 */
val colorOSFullVersion get() = "${if (isRealmeUI) "RealmeUI " else ""}$colorOSVersion"

/**
 * 获取 ColorOS 版本
 * @return [String]
 */
val colorOSVersion get() = "$colorOSNumberVersion ${Build.DISPLAY}"

/**
 * 获取 ColorOS 数字版本
 * @return [String]
 */
val colorOSNumberVersion
    get() = safeOf(default = "无法获取") {
        (classOf(name = "com.oplus.os.OplusBuild").let {
            it.field { name = "VERSIONS" }.ignoredError().get().array<String>()
                .takeIf { e -> e.isNotEmpty() }
                ?.get(it.method { name = "getOplusOSVERSION" }.ignoredError().get().int() - 1)
        } ?: findPropString(
            key = "ro.system.build.fingerprint", default = "无法获取"
        ).split("ssi:")[1].split("/")[0].trim())
    }

/**
 * 得到安装包信息
 * @return [PackageInfo]
 */
val Context.packageInfo get() = packageManager?.getPackageInfo(packageName, 0) ?: PackageInfo()

/**
 * 判断应用是否安装
 * @return [Boolean]
 */
val String.isInstall
    get() = safeOfFalse {
        appContext.packageManager.getPackageInfo(
            this, PackageManager.GET_UNINSTALLED_PACKAGES
        )
        true
    }

/**
 * 得到版本信息
 * @return [String]
 */
val Context.versionName get() = packageInfo.versionName ?: ""

/**
 * 得到版本号
 * @return [Int]
 */
val Context.versionCode get() = packageInfo.versionCode

/**
 * 得到 APP 名称
 * @param name APP 包名
 * @return [String]
 */
fun Context.findAppName(name: String) =
    safeOfNothing {
        packageManager?.getPackageInfo(name, 0)?.applicationInfo?.loadLabel(
            packageManager
        ).toString()
    }

/**
 * 得到 APP 图标
 * @param name APP 包名
 * @return [Drawable] or null
 */
fun Context.findAppIcon(name: String) =
    safeOfNull {
        packageManager?.getPackageInfo(
            name,
            0
        )?.applicationInfo?.loadIcon(packageManager)
    }

/**
 * 获取 APP 是否为 DEBUG 版本
 * @param name APP 包名
 * @return [Boolean]
 */
fun Context.isAppDebuggable(name: String) =
    safeOfFalse {
        (packageManager?.getPackageInfo(name, 0)?.applicationInfo?.flags?.and(
            ApplicationInfo.FLAG_DEBUGGABLE
        ) ?: 0) != 0
    }

/**
 * 对数值自动补零
 * @return [String]
 */
val Int.autoZero: String get() = if (this < 10) "0$this" else toString()

/**
 * 从字符串获取小时
 * @return [Int]
 */
val String.hour
    get() = safeOfNan {
        Calendar.getInstance().also {
            it.time = SimpleDateFormat("HH:mm", Locale.CHINA).parse(this) as Date
        }.get(Calendar.HOUR_OF_DAY)
    }

/**
 * 从字符串获取分钟
 * @return [Int]
 */
val String.minute
    get() = safeOfNan {
        Calendar.getInstance().also {
            it.time = SimpleDateFormat("HH:mm", Locale.CHINA).parse(this) as Date
        }.get(Calendar.MINUTE)
    }

/**
 * 是否关闭了通知权限
 * @return [Boolean]
 */
val isNotNoificationEnabled
    get() = !NotificationManagerCompat.from(appContext).areNotificationsEnabled()

/**
 * 网络连接是否正常
 * @return [Boolean] 网络是否连接
 */
val isNetWorkSuccess
    get() = safeOfFalse { appContext.getSystemService<ConnectivityManager>()?.activeNetworkInfo != null }

/**
 * dp 转换为 pxInt
 * @param context 使用的实例
 * @return [Int]
 */
fun Number.dp(context: Context) = dpFloat(context).toInt()

/**
 * dp 转换为 pxFloat
 * @param context 使用的实例
 * @return [Float]
 */
fun Number.dpFloat(context: Context) = toFloat() * context.resources.displayMetrics.density

/**
 * 获取系统壁纸颜色
 *
 * 由于 ColorOS 阉割了 [android.R.color.system_accent1_600] 这里取系统壁纸颜色做补偿
 * @return [Int] 无法获取时返回透明色
 */
val Context.wallpaperColor
    get() = safeOfNan {
        WallpaperManager.getInstance(this).getWallpaperColors(FLAG_SYSTEM)?.secondaryColor?.toArgb()
            ?: 0
    }

/**
 * 是否为白色
 * @return [Boolean]
 */
val Int.isWhite
    get() = safeOfTrue {
        val r = this and 0xff0000 shr 16
        val g = this and 0x00ff00 shr 8
        val b = this and 0x0000ff
        (0.2126 * r + 0.7152 * g + 0.0722 * b) >= 128
    }

/**
 * 调整颜色透明度
 * @param value 透明度
 * @return [Int] 调整后的颜色
 */
fun Int.colorAlphaOf(value: Float) =
    safeOfNan { (255.coerceAtMost(0.coerceAtLeast((value * 255).toInt())) shl 24) + (0x00ffffff and this) }

/**
 * Base64 加密
 * @return [String]
 */
val Bitmap.base64
    get() = safeOfNothing {
        val baos = ByteArrayOutputStream()
        compress(Bitmap.CompressFormat.PNG, 100, baos)
        Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT)
    }

/**
 * Base64 加密
 * @return [String]
 */
val String.base64: String get() = Base64.encodeToString(toByteArray(), Base64.DEFAULT)

/**
 * Base64 解密为字节流
 * @return [ByteArray]
 */
val String.unbase64 get() = Base64.decode(this, Base64.DEFAULT) ?: ByteArray(0)

/**
 * 字节流解析为位图
 * @return [Bitmap]
 */
val ByteArray.bitmap: Bitmap get() = BitmapFactory.decodeByteArray(this, 0, size)

/**
 * 字符串解析为位图
 * @return [Bitmap]
 */
val String.bitmap: Bitmap get() = unbase64.bitmap

/**
 * 获取系统 Prop 值
 * @param key Key
 * @param default 默认值
 * @return [String]
 */
fun findPropString(key: String, default: String = "") = safeOf(default) {
    (classOf(name = "android.os.SystemProperties").method {
        name = "get"
        param(StringType, StringType)
    }.get().invoke(key, default)) ?: default
}

/**
 * 是否有 Root 权限
 * @return [Boolean]
 */
val isRootAccess get() = safeOfFalse { Shell.rootAccess() }

/**
 * 执行命令
 * @param cmd 命令
 * @param isSu 是否使用 Root 权限执行 - 默认：是
 * @return [String] 执行结果
 */
fun execShell(cmd: String, isSu: Boolean = true) = safeOfNothing {
    (if (isSu) Shell.su(cmd) else Shell.sh(cmd)).exec().out.let {
        if (it.isNotEmpty()) it[0].trim() else ""
    }
}

/**
 * 弹出 [Toast]
 * @param msg 提示内容
 */
fun toast(msg: String) = Toast.makeText(appContext, msg, Toast.LENGTH_SHORT).show()

fun Context.updateBlackApps(packageName: String, isChecked: Boolean): Boolean {
    val blackApps = getBlackApps()
    if (isChecked) {
        //white
        val newBlackApps = blackApps.minus(packageName)
        if (newBlackApps.size != blackApps.size) {
            setBlackApps(newBlackApps)
            return true
        }
    } else {
        val newBlackApps = blackApps.plus(packageName)
        if (newBlackApps.size != blackApps.size) {
            setBlackApps(newBlackApps)
            return true
        }
    }

    return false
}

fun Context.updateWhiteApps(packageName: String, isChecked: Boolean): Boolean {
    val whiteApps = getWhiteApps()

    if (isChecked) {
        //white
        val newWhiteApps = whiteApps.plus(packageName)
        if (newWhiteApps.size != whiteApps.size) {
            setWhiteApps(newWhiteApps)
            return true
        }
    } else {
        val newWhiteApps = whiteApps.minus(packageName)
        if (newWhiteApps.size != whiteApps.size) {
            setWhiteApps(newWhiteApps)
            return true
        }
    }
    return false
}


fun Context.getBlackApps(): Set<String> {
    return modulePrefs(ConfigConst.BLACK_SYSTEM_APPS_NAME)
        .get(ConfigConst.BLACK_SYSTEM_APPS)
}

fun Context.getWhiteApps(): Set<String> {
    return modulePrefs(ConfigConst.WHITE_APPS_NAME)
        .get(ConfigConst.WHITE_APPS)
}

fun Context.getWhiteProcesses(): Set<String> {
    return modulePrefs(ConfigConst.WHITE_APP_PROCESSES_NAME)
        .get(ConfigConst.WHITE_APP_PROCESSES)
}

fun Context.getKillProcesses(): Set<String> {
    return modulePrefs(ConfigConst.KILL_APP_PROCESS_NAME)
        .get(ConfigConst.KILL_APP_PROCESS)
}

fun Context.setBlackApps(blackApps: Set<String>) {
    modulePrefs(ConfigConst.BLACK_SYSTEM_APPS_NAME)
        .put(ConfigConst.BLACK_SYSTEM_APPS, blackApps)
}

fun Context.setWhiteApps(whiteApps: Set<String>) {
    modulePrefs(ConfigConst.WHITE_APPS_NAME)
        .put(ConfigConst.WHITE_APPS, whiteApps)
}

fun Context.setWhiteProcesses(whiteProcesses: Set<String>) {
    modulePrefs(ConfigConst.WHITE_APP_PROCESSES_NAME)
        .put(ConfigConst.WHITE_APP_PROCESSES, whiteProcesses)
}

fun Context.setKillProcesses(killProcesses: Set<String>) {
    modulePrefs(ConfigConst.KILL_APP_PROCESS_NAME)
        .put(ConfigConst.KILL_APP_PROCESS, killProcesses)
}

inline fun <reified T : Activity> Activity.navigateWithTransition(
    position: Int,
    appItemData: AppItemData,
    vararg viewPairs: Pair<View, String>
) {
    val newIntent = Intent(this, T::class.java)
    val newBundle = Bundle();
    newBundle.putSerializable("pkgName", appItemData.packageName)
    newBundle.putInt("position", position)
    newIntent.putExtras(newBundle)

    val bundle =
        ActivityOptionsCompat.makeSceneTransitionAnimation(this, *viewPairs).toBundle()

    startActivity(newIntent, bundle);
}

/**
 * 跳转到指定页面
 *
 * [T] 为指定的 [Activity]
 */
inline fun <reified T : Activity> Context.navigate() = runInSafe {
    startActivity(Intent(if (this is Service) applicationContext else this, T::class.java).apply {
        if (this@navigate !is Activity) flags = Intent.FLAG_ACTIVITY_NEW_TASK
    })
}

fun PackageManager.originLabel(applicationInfo: ApplicationInfo): String {
    val label: String = this.getApplicationLabel(applicationInfo).toString()
    return if (label.endsWith("Application") || label.endsWith(".xml") || label.endsWith("false")) applicationInfo.packageName else label
}

/**
 * 弹出 [Snackbar]
 * @param msg 提示内容
 * @param actionText 按钮文本 - 不写默认取消按钮
 * @param callback 按钮事件回调
 */
fun Context.snake(msg: String, actionText: String = "", callback: () -> Unit = {}) =
    Snackbar.make((this as Activity).findViewById(android.R.id.content), msg, Snackbar.LENGTH_LONG)
        .apply {
            if (actionText.isBlank()) return@apply
            setActionTextColor(if (isSystemInDarkMode) Color.BLACK else Color.WHITE)
            setAction(actionText) { callback() }
        }.show()

/**
 * 启动系统浏览器
 * @param url 网址
 * @param packageName 指定包名 - 可不填
 */
fun Context.openBrowser(url: String, packageName: String = "") = runCatching {
    startActivity(Intent().apply {
        if (packageName.isNotBlank()) setPackage(packageName)
        action = Intent.ACTION_VIEW
        data = Uri.parse(url)
        /** 防止顶栈一样重叠在自己的 APP 中 */
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
    })
}.onFailure {
    if (packageName.isNotBlank()) snake(msg = "启动 $packageName 失败")
    else snake(msg = "启动系统浏览器失败")
}

/**
 * 跳转 APP 自身设置界面
 * @param packageName 包名
 */
fun Context.openSelfSetting(packageName: String = appContext.packageName) = runCatching {
    if (packageName.isInstall)
        startActivity(Intent().apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
            action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
            data = Uri.fromParts("package", packageName, null)
        })
    else toast(msg = "你没有安装此应用")
}.onFailure { toast(msg = "启动 $packageName 应用信息失败") }

/** 跳转通知设置界面 */
fun Context.openNotifySetting() = runCatching {
    Intent().also { intent ->
        intent.action = Settings.ACTION_APP_NOTIFICATION_SETTINGS
        intent.putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
        intent.putExtra(Notification.EXTRA_CHANNEL_ID, applicationInfo.uid)
        startActivity(intent)
    }
}.onFailure { snake(msg = "跳转通知设置失败") }

/**
 * 复制到剪贴板
 * @param content 要复制的文本
 */
fun Context.copyToClipboard(content: String) = runInSafe {
    (getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).apply {
        setPrimaryClip(ClipData.newPlainText(null, content))
        (primaryClip?.getItemAt(0)?.text ?: "").also {
            if (it != content) snake(msg = "复制失败") else snake(msg = "已复制")
        }
    }
}


fun Context.getAtsService(): IAtsConfigService? {
    try {
        val binder: IBinder = classOf(
            ClassEnum.ServiceManagerClass,
            Thread.currentThread().contextClassLoader
        )
            .method {
                name = "getService"
                param(StringType)
            }.get().invoke<IBinder>(AtsConfigService.serviceName)!!

        return IAtsConfigService.Stub.asInterface(binder)
    } catch (e: Exception) {
        atsLogE("ats config service get error", e = e)
    }
    return null
}

/**
 * 时间戳 -> 时间
 * @param format 格式化方法 - 默认：yyyy-MM-dd HH:mm:ss
 * @return [String] 目标字符串时间
 */
fun Long.stampToDate(format: String = "yyyy-MM-dd HH:mm:ss") =
    safeOfNothing { SimpleDateFormat(format, Locale.CHINA).format(Date(this)) ?: "" }

/**
 * 延迟执行
 * @param ms 毫秒 - 默认：150
 * @param it 方法体
 */
fun Any?.delayedRun(ms: Long = 150, it: () -> Unit) =
    runInSafe { Handler().postDelayed({ it() }, ms) }


fun buildAppItemData(
    pm: PackageManager,
    appInfo: ApplicationInfo,
    sysBlackApps: Set<String>,
    whiteApps: Set<String>,
    appItemData: AppItemData? = null,
    freezeApps: Set<String>
): AppItemData {
    val label: String = pm.originLabel(appInfo)
    val pkgName: String = appInfo.packageName
    val isSystem: Boolean = SystemTool.isSystem(appInfo)
    val isImportSystem: Boolean = SystemTool.isImportantSystemApp(appInfo)
    val isBlackApp: Boolean = sysBlackApps.contains(pkgName)
    val isWhiteApp: Boolean = whiteApps.contains(pkgName)
    val isFreeze: Boolean = freezeApps.contains(pkgName)

    if (appItemData == null) {
        val newAppData = AppItemData(
            name = label,
            label = label,
            applicationInfo = appInfo,
            isSystem = isSystem,
            isImportantSystemApp = isImportSystem,
            isXposedModule = SystemTool.isXposedModule(appInfo),
            icon = appInfo.loadIcon(pm),
            packageName = pkgName,
            inFreeze = isFreeze,
            isBlackApp = isBlackApp,
            isWhiteApp = isWhiteApp
        )
        newAppData.updatePriority()
        return newAppData;
    } else {
        appItemData.name = label
        appItemData.label = label
        appItemData.applicationInfo = appInfo
        appItemData.isSystem = isSystem
        appItemData.isImportantSystemApp = isImportSystem
        appItemData.isXposedModule = SystemTool.isXposedModule(appInfo)
        appItemData.icon = appInfo.loadIcon(pm)
        appItemData.packageName = pkgName
        appItemData.isBlackApp = isBlackApp
        appItemData.isWhiteApp = isWhiteApp
        appItemData.updatePriority()

        return appItemData
    }
}