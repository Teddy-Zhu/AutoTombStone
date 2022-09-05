package com.v2dawn.autotombstone.hook.tombstone.server

import android.content.ComponentName
import android.net.Uri
import com.highcapable.yukihookapi.hook.factory.field

class Intent(raw: Any) : ForkOrigin(raw) {


    val mAction: String = getRawData().javaClass.field {
        name = "mAction"
    }.get(getRawData()).string()

    val mPackage: String = getRawData().javaClass.field {
        name = "mPackage"
    }.get(getRawData()).string()

    val mComponent: ComponentName? = getRawData().javaClass.field {
        name = "mComponent"
    }.get(getRawData()).cast<ComponentName>()
    val mData: Uri? = getRawData().javaClass.field {
        name = "mData"
    }.get(getRawData()).cast<Uri>()

    override fun toString(): String {
        return "Intent(mAction='$mAction', mPackage='$mPackage', mComponent=$mComponent, mData=$mData)"
    }

}