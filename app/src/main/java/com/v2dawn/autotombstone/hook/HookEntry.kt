package com.v2dawn.autotombstone.hook

import android.os.Build
import com.highcapable.yukihookapi.annotation.xposed.InjectYukiHookWithXposed
import com.highcapable.yukihookapi.hook.factory.configs
import com.highcapable.yukihookapi.hook.factory.encase
import com.highcapable.yukihookapi.hook.xposed.prefs.YukiHookModulePrefs
import com.highcapable.yukihookapi.hook.xposed.proxy.IYukiHookXposedInit
import com.v2dawn.autotombstone.BuildConfig
import com.v2dawn.autotombstone.config.ConfigConst
import com.v2dawn.autotombstone.hook.tombstone.hook.*
import com.v2dawn.autotombstone.hook.tombstone.support.atsLogD
import com.v2dawn.autotombstone.hook.tombstone.support.atsLogI

@InjectYukiHookWithXposed(isUsingResourcesHook = false)
class HookEntry : IYukiHookXposedInit {

    override fun onInit() = configs {
        debugTag = "autotombstone"
        isDebug = false
        isAllowPrintingLogs = true
        isEnableModulePrefsCache = true
        isEnableModuleAppResourcesCache = false
        isEnableHookModuleStatus = true
        isEnableDataChannel = false
        isEnableMemberCache = true
    }

    override fun onHook() = encase {

//        loadZygote {
//            loadHooker(ActivityThreadHook())
//        }
        loadSystem {
            atsLogI("${Build.MANUFACTURER} device")


            loadHooker(CacheFreezerHook())
            loadHooker(UsageContextHook())
            loadHooker(AppStateChangeHook())
            loadHooker(BroadcastDeliverHook())
//            loadHooker(OomAdjHook())
            loadHooker(ANRHook())
            loadHooker(TaskTrimHook())
            loadHooker(PowerManagerServiceHook())
            loadHooker(ActivityTaskHook())
            loadHooker(ActivityIdleHook())
            loadHooker(TestActivityHook())

        }

//        loadApp("com.android.systemui"){
//            atsLogI("system ui")
//
//            loadHooker(TestActivityHook())
//
//        }
//        loadApp(BuildConfig.APPLICATION_ID) {
//            loadHooker(ConfigReloadHook())
//            atsLogI("load config reload hook")
//        }
    }
}