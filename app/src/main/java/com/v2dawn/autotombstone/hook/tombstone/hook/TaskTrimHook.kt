package com.v2dawn.autotombstone.hook.tombstone.hook

import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.v2dawn.autotombstone.hook.tombstone.support.ClassEnum
import com.v2dawn.autotombstone.hook.tombstone.support.atsLogI
import com.v2dawn.autotombstone.hook.tombstone.support.doNothing

class TaskTrimHook:YukiBaseHooker() {


    override fun onHook() {


        ClassEnum.RecentTasksClass.hook {
            injectMember {
                method {
                    name="trimInactiveRecentTasks"
                    emptyParam()
                }
                replaceUnit {
                    //ignored
                }
            }
        }
        atsLogI("hooked task trim")
    }
}