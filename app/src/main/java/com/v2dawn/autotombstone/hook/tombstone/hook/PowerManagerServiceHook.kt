package com.v2dawn.autotombstone.hook.tombstone.hook

import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.v2dawn.autotombstone.hook.tombstone.server.PowerManagerService
import com.v2dawn.autotombstone.hook.tombstone.support.ClassEnum
import com.v2dawn.autotombstone.hook.tombstone.support.atsLogI

class PowerManagerServiceHook:YukiBaseHooker() {
    override fun onHook() {
        ClassEnum.PowerManagerServiceClass.hook {
            injectMember {
                method {
                    name = "onStart"
                    emptyParam()
                }
                afterHook {
                    atsLogI("hooked inject pms")
                    PowerManagerService(instance)
                }
            }
        }
        atsLogI("hooked pms")
    }
}