package com.v2dawn.autotombstone.hook

import com.highcapable.yukihookapi.annotation.xposed.InjectYukiHookWithXposed
import com.highcapable.yukihookapi.hook.factory.configs
import com.highcapable.yukihookapi.hook.factory.encase
import com.highcapable.yukihookapi.hook.xposed.proxy.IYukiHookXposedInit
import com.v2dawn.autotombstone.hook.tombstone.hook.Android
import com.v2dawn.autotombstone.hook.tombstone.hook.PowerKeeper

@InjectYukiHookWithXposed(entryClassName = "AutoTombStone")
class HookEntry : IYukiHookXposedInit {

    override fun onInit() = configs {
        debugTag = "autotombstone"
        isDebug = true
        isAllowPrintingLogs = true
        isEnableModulePrefsCache = true
        isEnableModuleAppResourcesCache = false
        isEnableHookModuleStatus = true
        isEnableDataChannel = false
        isEnableMemberCache = true
    }

    override fun onHook() = encase {
        loadHooker(Android)

        loadHooker(PowerKeeper)

    }
}