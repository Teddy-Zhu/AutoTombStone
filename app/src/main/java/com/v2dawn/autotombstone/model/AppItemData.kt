package com.v2dawn.autotombstone.model;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo
import android.graphics.drawable.Drawable;
import java.io.Serializable


data class AppItemData(
    var name: String,
    var label: String,
    var packageName: String,
    var icon: Drawable,

    var inFreeze: Boolean = false,

    var priority: Int = Integer.valueOf("20"),
    var isXposedModule: Boolean,
    var isSystem: Boolean,
    var isImportantSystemApp: Boolean,
    var isBlackApp: Boolean,
    var isWhiteApp: Boolean,
    var processes: HashSet<String> = HashSet(),

    ) : Serializable {

    val enable: Boolean
        get() = if (isSystem && !isBlackApp) true else isWhiteApp

    fun updatePriority() {
        var newPriority = 20
        if (isSystem) {
            if (isImportantSystemApp) {
                newPriority += 5
            }
            if (isBlackApp) {
                newPriority -= 2
            } else {
                newPriority += 5
            }

        } else {
            if (isWhiteApp) {
                newPriority -= 5
            }
        }
        if (inFreeze) {
            newPriority -= 4
        }
        priority = newPriority
    }

    override fun toString(): String {
        return "AppItemData(name='$name', label='$label', packageName='$packageName', icon=$icon, inFreeze=$inFreeze, priority=$priority, isXposedModule=$isXposedModule, isSystem=$isSystem, isImportantSystemApp=$isImportantSystemApp, isBlackApp=$isBlackApp, isWhiteApp=$isWhiteApp, processes=$processes, enable=$enable)"
    }


}