@file:Suppress("SetTextI18n")

package com.v2dawn.autotombstone.ui.activity

import android.content.ComponentName
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.IAtsConfigService
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.view.isVisible
import com.highcapable.yukihookapi.YukiHookAPI
import com.highcapable.yukihookapi.hook.factory.method
import com.highcapable.yukihookapi.hook.factory.modulePrefs
import com.highcapable.yukihookapi.hook.type.java.StringType
import com.v2dawn.autotombstone.BuildConfig
import com.v2dawn.autotombstone.R
import com.v2dawn.autotombstone.config.ConfigConst
import com.v2dawn.autotombstone.databinding.ActivityMainBinding
import com.android.server.AtsConfigService
import com.highcapable.yukihookapi.hook.factory.classOf
import com.topjohnwu.superuser.Shell
import com.topjohnwu.superuser.ShellUtils
import com.v2dawn.autotombstone.hook.tombstone.support.ClassEnum
import com.v2dawn.autotombstone.hook.tombstone.support.atsLogE
import com.v2dawn.autotombstone.hook.tombstone.support.atsLogI
import com.v2dawn.autotombstone.hook.tombstone.support.atsLogW
import com.v2dawn.autotombstone.ui.activity.base.BaseActivity
import com.v2dawn.autotombstone.utils.factory.navigate
import com.v2dawn.autotombstone.utils.factory.showDialog


class MainActivity : BaseActivity<ActivityMainBinding>() {
    companion object {
        const val TAG = "MainActivity"

        /** 窗口是否启动 */
        internal var isActivityLive = false

        /** 模块版本 */
        private const val moduleVersion = BuildConfig.VERSION_NAME

        /** 预发布的版本标识 */
        private const val pendingFlag = ""

        var atsConfigService: IAtsConfigService? = null
    }

    private val prefsListeners =
        hashMapOf<String, SharedPreferences.OnSharedPreferenceChangeListener>()

    override fun onCreate() {
        isActivityLive = true

        refreshModuleStatus()
        binding.mainTextVersion.text = getString(R.string.module_version, BuildConfig.VERSION_NAME)
        binding.hideIconInLauncherSwitch.isChecked = isLauncherIconShowing.not()
        binding.hideIconInLauncherSwitch.setOnCheckedChangeListener { button, isChecked ->
            if (button.isPressed) hideOrShowLauncherIcon(isChecked)
        }
        // Your code here.
        binding.appConfigButton.setOnClickListener {
            navigate<AppConfigureActivity>()
        }

        binding.restartBtn.setOnClickListener {
            restartSystem()
        }
        Log.i(
            TAG,
            "check debug ${modulePrefs(ConfigConst.COMMON_NAME).get(ConfigConst.ENABLE_MODULE_LOG)}"
        )

        binding.enableDebug.isChecked =
            modulePrefs(ConfigConst.COMMON_NAME).get(ConfigConst.ENABLE_MODULE_LOG)
        binding.enableDebug.setOnCheckedChangeListener { _, checked ->
            modulePrefs(ConfigConst.COMMON_NAME).put(ConfigConst.ENABLE_MODULE_LOG, checked)
        }
        binding.kill19.isChecked =
            modulePrefs(ConfigConst.COMMON_NAME).get(ConfigConst.ENABLE_FORCE_KILL_19)

        binding.kill19.setOnCheckedChangeListener { _, checked ->
            modulePrefs(ConfigConst.COMMON_NAME).put(ConfigConst.ENABLE_FORCE_KILL_19, checked)
        }
        binding.kill20.isChecked =
            modulePrefs(ConfigConst.COMMON_NAME).get(ConfigConst.ENABLE_FORCE_KILL_20)

        binding.kill20.setOnCheckedChangeListener { _, checked ->
            modulePrefs(ConfigConst.COMMON_NAME).put(ConfigConst.ENABLE_FORCE_KILL_20, checked)
        }
        binding.freezerApi.isChecked =
            modulePrefs(ConfigConst.COMMON_NAME).get(ConfigConst.ENABLE_FREEEZER_API)
        binding.freezerApi.setOnCheckedChangeListener { _, checked ->
            modulePrefs(ConfigConst.COMMON_NAME).put(ConfigConst.ENABLE_FREEEZER_API, checked)
        }
        binding.freezerV2.isChecked =
            modulePrefs(ConfigConst.COMMON_NAME).get(ConfigConst.ENABLE_FREEEZER_V2)

        binding.freezerV2.setOnCheckedChangeListener { _, checked ->
            modulePrefs(ConfigConst.COMMON_NAME).put(ConfigConst.ENABLE_FREEEZER_V2, checked)
        }
        binding.freezerV1.isChecked =
            modulePrefs(ConfigConst.COMMON_NAME).get(ConfigConst.ENABLE_FREEEZER_V1)

        binding.freezerV1.setOnCheckedChangeListener { _, checked ->
            modulePrefs(ConfigConst.COMMON_NAME).put(ConfigConst.ENABLE_FREEEZER_V1, checked)
        }

        val confs = ArrayList<String>().apply {
            add(ConfigConst.COMMON_NAME)
            add(ConfigConst.WHITE_APPS_NAME)
            add(ConfigConst.WHITE_APP_PROCESSES_NAME)
            add(ConfigConst.BLACK_SYSTEM_APPS_NAME)
            add(ConfigConst.KILL_APP_PROCESS_NAME)
        }
        for (conf in confs) {
            val l1: SharedPreferences.OnSharedPreferenceChangeListener =
                SharedPreferences.OnSharedPreferenceChangeListener { _, s: String ->
                    prefChange(conf, s)
                }

            prefsListeners[conf] = l1
            modulePrefs(conf).registerChangeListener(l1)
        }

    }

    fun getAtsService(): IAtsConfigService {
        if (atsConfigService == null) {
            try {
                val binder: IBinder = classOf(
                    ClassEnum.ServiceManagerClass,
                    Thread.currentThread().contextClassLoader
                )
                    .method {
                        name = "getService"
                        param(StringType)
                    }.get().invoke<IBinder>(AtsConfigService.serviceName)!!

                atsConfigService =
                    IAtsConfigService.Stub.asInterface(binder)
            } catch (e: Exception) {
                atsLogE("ats config service get error", e = e)
            }

        }

        return atsConfigService!!
    }

    fun restartSystem() {

        showDialog {
            title = "提示"
            msg = "确认重启吗？除模块升级或激活外,修改配置无需重启即可生效"
            confirmButton {
                Shell.cmd("reboot").submit();
            }
            cancelButton()
        }
    }

    private fun prefChange(configName: String, key: String) {
        Log.d(TAG, "pref change $configName , $key")

        if (!YukiHookAPI.Status.isModuleActive) {
            return
        }
        getAtsService().configChange(configName, key)

    }

    override fun onDestroy() {
        for (entry in prefsListeners.entries) {
            modulePrefs(entry.key).unRegisterChangeListener(entry.value)
        }
        super.onDestroy()
    }

    /**
     * Hide or show launcher icons
     *
     * - You may need the latest version of LSPosed to enable the function of hiding launcher
     *   icons in higher version systems
     *
     * 隐藏或显示启动器图标
     *
     * - 你可能需要 LSPosed 的最新版本以开启高版本系统中隐藏 APP 桌面图标功能
     * @param isShow Whether to display / 是否显示
     */
    private fun hideOrShowLauncherIcon(isShow: Boolean) {
        packageManager?.setComponentEnabledSetting(
            ComponentName(packageName, "${BuildConfig.APPLICATION_ID}.Home"),
            if (isShow) PackageManager.COMPONENT_ENABLED_STATE_DISABLED else PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
            PackageManager.DONT_KILL_APP
        )
    }

    /**
     * Get launcher icon state
     *
     * 获取启动器图标状态
     * @return [Boolean] Whether to display / 是否显示
     */
    private val isLauncherIconShowing
        get() = packageManager?.getComponentEnabledSetting(
            ComponentName(packageName, "${BuildConfig.APPLICATION_ID}.Home")
        ) != PackageManager.COMPONENT_ENABLED_STATE_DISABLED

    /**
     * Refresh module status
     *
     * 刷新模块状态
     */
    private fun refreshModuleStatus() {
        binding.mainLinStatus.setBackgroundResource(
            when {
                YukiHookAPI.Status.isModuleActive -> R.drawable.bg_green_round
                else -> R.drawable.bg_dark_round
            }
        )
        binding.mainImgStatus.setImageResource(
            when {
                YukiHookAPI.Status.isModuleActive -> R.mipmap.ic_success
                else -> R.mipmap.ic_warn
            }
        )
        binding.mainTextStatus.text = getString(
            when {
                YukiHookAPI.Status.isModuleActive -> R.string.module_is_activated
                else -> R.string.module_not_activated
            }
        )
        binding.mainTextApiWay.isVisible = YukiHookAPI.Status.isModuleActive
        when {
            YukiHookAPI.Status.executorVersion > 0 ->
                binding.mainTextApiWay.text =
                    "Activated by ${YukiHookAPI.Status.executorName} API ${YukiHookAPI.Status.executorVersion}"
            YukiHookAPI.Status.isTaiChiModuleActive -> binding.mainTextApiWay.text =
                "Activated by TaiChi"
            else -> binding.mainTextApiWay.text = "Activated by anonymous"
        }
    }
}