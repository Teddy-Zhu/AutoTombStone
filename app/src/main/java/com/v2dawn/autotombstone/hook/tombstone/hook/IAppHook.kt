package com.v2dawn.autotombstone.hook.tombstone.hook;

import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam

interface IAppHook {
    abstract fun hook(packageParam: LoadPackageParam?)
}
