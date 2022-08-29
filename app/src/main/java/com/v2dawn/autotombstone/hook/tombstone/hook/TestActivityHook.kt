package com.v2dawn.autotombstone.hook.tombstone.hook

import android.content.ComponentName
import android.content.Intent
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.type.android.ComponentNameClass
import com.highcapable.yukihookapi.hook.type.android.IntentClass
import com.highcapable.yukihookapi.hook.type.java.IntType
import com.highcapable.yukihookapi.hook.type.java.StringType
import com.v2dawn.autotombstone.hook.tombstone.hook.support.AppStateChangeExecutor
import com.v2dawn.autotombstone.hook.tombstone.support.atsLogD
import com.v2dawn.autotombstone.hook.tombstone.support.atsLogI

class TestActivityHook : YukiBaseHooker() {

    override fun onHook() {
        val a = arrayOf<Intent>()
        var b = arrayOf<String>()


    }
}