package com.v2dawn.autotombstone.hook.tombstone.hook;

import android.content.Intent
import android.os.Build
import android.service.notification.StatusBarNotification
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.log.loggerD
import com.highcapable.yukihookapi.hook.log.loggerI
import com.v2dawn.autotombstone.hook.tombstone.server.ActivityManagerService
import com.v2dawn.autotombstone.hook.tombstone.support.ClassEnum
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam

object Android : YukiBaseHooker() {

    override fun hook11(packageParam: LoadPackageParam) {
        // 类加载器
        val classLoader = packageParam.classLoader




        // Hook 广播分发
        XposedHelpers.findAndHookMethod(
            ClassEnum.BroadcastQueue, classLoader, MethodEnum.deliverToRegisteredReceiverLocked,
            ClassEnum.BroadcastRecord,
            ClassEnum.BroadcastFilter,
            Boolean::class.javaPrimitiveType,
            Int::class.javaPrimitiveType, BroadcastDeliverHook(memData)
        )

        // Hook oom_adj
//        if (!FreezerConfig.isConfigOn(FreezerConfig.disableOOM)) {
//            boolean colorOs = FreezerConfig.isColorOs();
//            if (!colorOs && (Build.MANUFACTURER.equals("OPPO") || Build.MANUFACTURER.equals("OnePlus"))) {
//                Log.w("If you are using ColorOS");
//                Log.w("You can create file color.os");
//            }
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
//                if (colorOs) {
//                    loggerI(msg="Hello ColorOS");
//                    XposedHelpers.findAndHookMethod(ClassEnum.OomAdjuster, classLoader, MethodEnum.computeOomAdjLSP, ClassEnum.ProcessRecord, int.class, ClassEnum.ProcessRecord, boolean.class, long.class, boolean.class, boolean.class, new OomAdjHook(classLoader, memData, OomAdjHook.Color));
//                } else {
//                    XposedHelpers.findAndHookMethod(ClassEnum.ProcessStateRecord, classLoader, MethodEnum.setCurAdj, int.class, new OomAdjHook(classLoader, memData, OomAdjHook.Android_S));
//                }
//                loggerI(msg="Auto lmk");
//            } else if (Build.VERSION.SDK_INT == Build.VERSION_CODES.R || Build.VERSION.SDK_INT == Build.VERSION_CODES.Q) {
//                XposedHelpers.findAndHookMethod(ClassEnum.OomAdjuster, classLoader, MethodEnum.applyOomAdjLocked, ClassEnum.ProcessRecord, boolean.class, long.class, long.class, new OomAdjHook(classLoader, memData, OomAdjHook.Android_Q_R));
//                loggerI(msg="Auto lmk");
//            }
//        }


        loggerI(msg = "Load success")
    }


    private fun hookAppSwitch(
        classLoader: ClassLoader,
        appStateChangeExecutor: AppStateChangeExecutor
    ) {

//        XposedHelpers.findAndHookMethod(ClassEnum.ActivityManagerService, classLoader,
//                "setHasOverlayUi",
//                int.class, boolean.class,
//                new XC_MethodHook() {
//                    @Override
//                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
//                    }
//
//                    @Override
//                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
//                        loggerI(msg="setHasOverlayUi");
//
//                    }
//                });
    }

    override fun onHook() {
        loggerI(msg = Build.MANUFACTURER + " device")
        loadSystem {
            loadHooker(ANRHook)

            loadHooker(CacheFreezerHook)

            loadHooker(UsageContextHook)
        }

    }

}
