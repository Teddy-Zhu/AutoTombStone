package com.v2dawn.autotombstone.hook.tombstone.hook

import com.android.server.AtsConfigService
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.factory.classOf
import com.highcapable.yukihookapi.hook.factory.method
import com.highcapable.yukihookapi.hook.type.android.IBinderClass
import com.highcapable.yukihookapi.hook.type.java.StringType
import com.v2dawn.autotombstone.hook.tombstone.support.ClassEnum
import com.v2dawn.autotombstone.hook.tombstone.support.atsLogI

@Deprecated(message = "replace by app state change hook")
class ActivityThreadHook : YukiBaseHooker() {

    companion object {
        var serviceRegistered = false
    }

    override fun onHook() {
        if (serviceRegistered) return

        ClassEnum.ActivityThreadClass.hook {
            injectMember {
                allMethods("systemMain")
                afterHook {
//                    registerAtsConfigService()
                }
            }
        }


    }

}