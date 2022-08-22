@file:Suppress("SetTextI18n")

package com.v2dawn.autotombstone.ui.activity

import android.content.ComponentName
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.view.isVisible
import com.highcapable.yukihookapi.YukiHookAPI
import com.highcapable.yukihookapi.hook.factory.modulePrefs
import com.v2dawn.autotombstone.BuildConfig
import com.v2dawn.autotombstone.R
import com.v2dawn.autotombstone.config.ConfigConst
import com.v2dawn.autotombstone.databinding.ActivityMainBinding
import com.v2dawn.autotombstone.ui.activity.base.BaseActivity
import com.v2dawn.autotombstone.utils.factory.navigate

class MainActivity : BaseActivity<ActivityMainBinding>() {
    companion object {
        const val TAG = "MainActivity"

        /** 窗口是否启动 */
        internal var isActivityLive = false

        /** 模块版本 */
        private const val moduleVersion = BuildConfig.VERSION_NAME

        /** 预发布的版本标识 */
        private const val pendingFlag = ""
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
            val dictChars = mutableListOf<Char>().apply {
                "123456789zxcvbnmasdfghjklqwertyuiop".forEach {
                    this.add(it)
                }
            }
            val randomStr =
                StringBuilder().apply { (1..((10..30).random())).onEach { append(dictChars.random()) } }
            Log.d(TAG, "randomStr:$randomStr")
            modulePrefs(ConfigConst.COMMON_NAME).put(ConfigConst.TEST_RANDOM, randomStr.toString())
            navigate<AppConfigureActivity>()
//            navigate<AppConfigureDetailActivity>()
        }
//

        val confs = ArrayList<String>().apply {
            add(ConfigConst.COMMON_NAME)
            add(ConfigConst.WHITE_APPS_NAME)
            add(ConfigConst.WHITE_APP_PROCESSES_NAME)
            add(ConfigConst.BLACK_SYSTEM_APPS_NAME)
            add(ConfigConst.KILL_APP_PROCESS_NAME)
        }
        for (conf in confs) {
            val l1: SharedPreferences.OnSharedPreferenceChangeListener =
                SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences: SharedPreferences, s: String ->
                    prefChange(conf, s)
                }

            prefsListeners[conf] = l1
            getPrefs(conf).registerOnSharedPreferenceChangeListener(l1)
        }

    }

    private fun prefChange(name: String, key: String) {
        Log.d(TAG, "pref change $name , $key")
    }

    private fun getPrefs(prefsName: String): SharedPreferences {
        try {
            return getSharedPreferences(prefsName, Context.MODE_WORLD_READABLE)
                ?: error("If you want to use module prefs, you must set the context instance first")
        } catch (_: Throwable) {
            return getSharedPreferences(prefsName, Context.MODE_PRIVATE)
                ?: error("If you want to use module prefs, you must set the context instance first")
        }
    }

    override fun onDestroy() {
        for (entry in prefsListeners.entries) {
            getPrefs(entry.key).unregisterOnSharedPreferenceChangeListener(entry.value)
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