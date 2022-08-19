package com.v2dawn.autotombstone.hook.tombstone.server;

import com.highcapable.yukihookapi.hook.factory.field
import com.v2dawn.autotombstone.hook.tombstone.support.FieldEnum
import de.robv.android.xposed.XposedHelpers;
import java.io.File


class ReceiverList(private val receiverList: Any) {
    public var processRecord: ProcessRecord? = null
    fun clear() {
        receiverList.javaClass
            .field { name = FieldEnum.appField }
            .get(processRecord).set(null)
    }

    fun restore(app: Any?) {
        receiverList.javaClass
            .field { name = FieldEnum.appField }
            .get(processRecord).set(app)
    }

    fun isNull(): Boolean {
        return processRecord == null
    }

    init {
        try {
            val raw = receiverList.javaClass.field {
                name = FieldEnum.appField
            }.get(receiverList).cast<Any>()
            raw?.let {
                processRecord =
                    ProcessRecord(raw)
            }
        } catch (ignored: Exception) {
        }
    }
}
