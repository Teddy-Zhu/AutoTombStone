package com.v2dawn.autotombstone.hook.tombstone.hook;

import android.os.Build
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.log.loggerI
import com.v2dawn.autotombstone.hook.tombstone.support.ClassEnum
import com.v2dawn.autotombstone.hook.tombstone.support.MethodEnum
import com.v2dawn.autotombstone.hook.tombstone.support.atsLogI

class CacheFreezerHook : YukiBaseHooker() {


    override fun onHook() {
        // 禁用暂停执行已缓存
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.Q) {
            ClassEnum.CachedAppOptimizerClass.hook {
                injectMember {
                    method {
                        name = MethodEnum.useFreezer
                        emptyParam()
                    }
                    replaceToFalse()
                }
            }
            atsLogI( "Disable cache freezer")
        }
    }


}
