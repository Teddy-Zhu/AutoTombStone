package com.v2dawn.autotombstone.hook.tombstone.hook

import android.os.Build
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.factory.method
import com.highcapable.yukihookapi.hook.type.android.IBinderClass
import com.highcapable.yukihookapi.hook.type.java.StringType
import com.v2dawn.autotombstone.hook.tombstone.hook.support.AppStateChangeExecutor
import com.v2dawn.autotombstone.hook.tombstone.support.AtsConfigService
import com.v2dawn.autotombstone.hook.tombstone.support.ClassEnum
import com.v2dawn.autotombstone.hook.tombstone.support.atsLogI
import de.robv.android.xposed.XposedHelpers

class ActivityThreadHook : YukiBaseHooker() {

    companion object {
        var serviceRegistered = false
    }

    override fun onHook() {
        if (serviceRegistered) return

    }


    private fun registerAtsConfigService() {
        atsLogI("register atsConfigService")
        if (serviceRegistered) return


        val atsConfigService = AtsConfigService()

        ClassEnum.ServiceManagerClass.clazz
            .method {
                name = "addService"
                param(StringType, IBinderClass, Boolean::class.javaPrimitiveType!!)
            }.get().call(AtsConfigService.serviceName, atsConfigService, true)

        serviceRegistered = true
    }
}