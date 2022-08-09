package com.v2dawn.tdytombstone.model;

import android.content.pm.ApplicationInfo;
import android.graphics.drawable.Drawable;
import java.io.Serializable


data class AppItemData(
    var name: String,
    var label: String,
    var packageName: String,
    var icon: Drawable,

    var enable: Boolean,
    var priority: Integer,
    var isXposedModule: Boolean,
    var isSystem: Boolean,
    var isImportantSystemApp: Boolean,
    var applicationInfo: ApplicationInfo,
) : Serializable {
    override fun toString(): String {
        return "AppItemData(name='$name'," +
                " label='$label', " +
                "packageName='$packageName', " +
                "icon=$icon, enable=$enable, " +
                "priority=$priority, " +
                "isXposedModule=$isXposedModule, " +
                "isSystem=$isSystem, " +
                "isImportantSystemApp=$isImportantSystemApp, " +
                "applicationInfo=$applicationInfo)"
    }
}