@file:Suppress("SetTextI18n")

package com.v2dawn.autotombstone.ui.activity

import android.content.ComponentName
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.IAtsConfigService
import android.os.IBinder
import android.util.Log
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.widget.AppCompatSeekBar
import androidx.core.view.isVisible
import com.android.server.AtsConfigService
import com.highcapable.yukihookapi.YukiHookAPI
import com.highcapable.yukihookapi.hook.factory.classOf
import com.highcapable.yukihookapi.hook.factory.method
import com.highcapable.yukihookapi.hook.factory.modulePrefs
import com.highcapable.yukihookapi.hook.type.java.StringType
import com.highcapable.yukihookapi.hook.xposed.prefs.data.PrefsData
import com.v2dawn.autotombstone.BuildConfig
import com.v2dawn.autotombstone.R
import com.v2dawn.autotombstone.config.ConfigConst
import com.v2dawn.autotombstone.databinding.ActivityMainBinding
import com.v2dawn.autotombstone.hook.tombstone.support.ClassEnum
import com.v2dawn.autotombstone.hook.tombstone.support.atsLogE
import com.v2dawn.autotombstone.ui.activity.base.BaseActivity
import com.v2dawn.autotombstone.utils.factory.*


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

        val allFreezeTypes = hashMapOf<Int, String>().apply {
            put(0, "kill 19")
            put(1, "kill 20")
            put(2, "freeze api")
            put(3, "freeze v2")
            put(4, "freeze v1")
        }
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

        binding.appDebugBackground.isVisible = BuildConfig.DEBUG

        if (BuildConfig.DEBUG) {
            binding.appDebugBackground.setOnClickListener {
                showDialog {
                    title = "后台运行Apps"
                    msg = getAtsService().queryBackgroundApps().joinToString(separator = "\n")
                    confirmButton(text = "我知道了") { cancel() }
                    noCancelable()
                }
            }
        }
        binding.enableDebug.isChecked =
            modulePrefs(ConfigConst.COMMON_NAME).get(ConfigConst.ENABLE_MODULE_LOG)
        binding.enableDebug.setOnCheckedChangeListener { _, checked ->
            modulePrefs(ConfigConst.COMMON_NAME).put(ConfigConst.ENABLE_MODULE_LOG, checked)
        }
        binding.enableStopservice.isChecked =
            modulePrefs(ConfigConst.COMMON_NAME).get(ConfigConst.STOP_SERVICE)

        binding.enableStopservice.setOnCheckedChangeListener { _, checked ->
            modulePrefs(ConfigConst.COMMON_NAME).put(ConfigConst.STOP_SERVICE, checked)
        }
        binding.enableRefreshTask.isChecked =
            modulePrefs(ConfigConst.COMMON_NAME).get(ConfigConst.ENABLE_RECHECK_APP)

        binding.enableRefreshTask.setOnCheckedChangeListener { _, checked ->
            modulePrefs(ConfigConst.COMMON_NAME).put(ConfigConst.ENABLE_RECHECK_APP, checked)
        }
        val supported = getAtsService().supportFreezeType

        val items = arrayListOf<FreezeTypeItem>()
        allFreezeTypes.filter { supported.contains(it.key) }.forEach {
            items.add(FreezeTypeItem(it.key, it.value))
        }

        binding.freezeTypeStatus.text =
            "${getString(R.string.freeze_method)} [${
                allFreezeTypes[modulePrefs.name(ConfigConst.COMMON_NAME)
                    .get(ConfigConst.FREEZE_TYPE)]
            }]"
        binding.freezeTypeStatus.setOnClickListener {
            showCommonPopup(
                it,
                items,
                selected = modulePrefs.name(ConfigConst.COMMON_NAME).get(ConfigConst.FREEZE_TYPE)
            ) { _: Int, t: FreezeTypeItem ->
                modulePrefs.name(ConfigConst.COMMON_NAME).put(ConfigConst.FREEZE_TYPE, t.type)
                binding.freezeTypeStatus.text =
                    "${getString(R.string.freeze_method)} [${t.text}]"
            }
        }
        val stopServiceModeItems = arrayListOf<StopServiceModeItem>().apply {
            add(StopServiceModeItem(1, "DirectMode"))
            add(StopServiceModeItem(2, "ApiMode"))

        }
        binding.stopServiceMode.text =
            "${getString(R.string.stop_service_mode)} [${
                stopServiceModeItems.filter {
                    it.type == modulePrefs.name(ConfigConst.COMMON_NAME)
                        .get(ConfigConst.STOP_SERVICE_MODE)
                }[0].text
            }]"
        binding.stopServiceMode.setOnClickListener {
            showCommonPopup(
                it,
                stopServiceModeItems,
                selected = stopServiceModeItems.indexOfFirst {
                    it.type == modulePrefs.name(ConfigConst.COMMON_NAME)
                        .get(ConfigConst.STOP_SERVICE_MODE)
                }
            ) { _: Int, t: StopServiceModeItem ->
                modulePrefs.name(ConfigConst.COMMON_NAME).put(ConfigConst.STOP_SERVICE_MODE, t.type)
                binding.stopServiceMode.text =
                    "${getString(R.string.stop_service_mode)} [${t.text}]"
            }
        }


        initEvent(
            binding.delayFreezeTime,
            binding.delayFreezeTimeVal,
            ConfigConst.COMMON_NAME,
            ConfigConst.DELAY_FREEZE_TIME
        )
        initEvent(
            binding.delayPauseTime,
            binding.delayPauseTimeVal,
            ConfigConst.COMMON_NAME,
            ConfigConst.DELAY_PAUSE_TIME
        )
        initEvent(
            binding.refreezeTime,
            binding.delayRefreezeTimeVal,
            ConfigConst.COMMON_NAME,
            ConfigConst.ENABLE_RECHECK_APP_TIME
        )


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

    class FreezeTypeItem(val type: Int, text: String) : PopUpItem(text)
    class StopServiceModeItem(val type: Int, text: String) : PopUpItem(text)

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
        if (!YukiHookAPI.Status.isModuleActive) {
            toast("模块未激活,该功能无法使用")
            return
        }
        showDialog {
            title = "提示"
            msg = "确认重启吗？除模块升级或激活外,修改配置无需重启即可生效"
            confirmButton {
//                Shell.cmd("reboot").submit();
                getAtsService().restartSystem()
            }
            cancelButton()
        }
    }

    private fun initEvent(
        appCompatSeekBar: AppCompatSeekBar,
        valText: TextView,
        configName: String,
        key: PrefsData<Long>,
    ) {

        appCompatSeekBar.progress =
            modulePrefs.name(configName).get(key).toInt()
        valText.text = "${appCompatSeekBar.progress}"
        appCompatSeekBar.setOnSeekBarChangeListener(object :
            SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                valText.text = "$progress"
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                modulePrefs.name(configName)
                    .put(key, seekBar.progress.toLong())
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
        })

    }

    private fun prefChange(configName: String, key: String) {
        Log.d(TAG, "pref change $configName , $key")

        if (!YukiHookAPI.Status.isModuleActive) {
            toast("模块未激活")
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