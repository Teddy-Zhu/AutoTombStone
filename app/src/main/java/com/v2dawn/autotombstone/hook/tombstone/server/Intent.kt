package com.v2dawn.autotombstone.hook.tombstone.server

import android.content.ComponentName
import android.net.Uri
import com.highcapable.yukihookapi.hook.factory.field

class Intent(raw: Any) : ForkOrigin(raw) {


    val mAction: String

    val mPackage: String

    val mComponent: ComponentName?
    val mData: Uri?

    init {

        mAction = getRawData().javaClass.field {
            name = "mAction"
        }.get(getRawData()).string()
        mPackage = getRawData().javaClass.field {
            name = "mPackage"
        }.get(getRawData()).string()
        mComponent = getRawData().javaClass.field {
            name = "mComponent"
        }.get(getRawData()).cast<ComponentName>()
        mData = getRawData().javaClass.field {
            name = "mData"
        }.get(getRawData()).cast<Uri>()

    }

    override fun toString(): String {
        return "Intent(mAction='$mAction', mPackage='$mPackage', mComponent=$mComponent, mData=$mData)"
    }

}