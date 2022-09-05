package com.v2dawn.autotombstone.hook.tombstone.hook

import android.content.ComponentName
import android.content.Context
import android.os.IAtsConfigService
import android.os.IBinder
import com.android.server.AtsConfigService
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.factory.classOf
import com.highcapable.yukihookapi.hook.factory.field
import com.highcapable.yukihookapi.hook.factory.method
import com.highcapable.yukihookapi.hook.type.android.ViewClass
import com.highcapable.yukihookapi.hook.type.java.ObjectsClass
import com.highcapable.yukihookapi.hook.type.java.StringType
import com.v2dawn.autotombstone.hook.tombstone.hook.support.AppStateChangeExecutor
import com.v2dawn.autotombstone.hook.tombstone.support.ClassEnum
import com.v2dawn.autotombstone.hook.tombstone.support.atsLogD
import com.v2dawn.autotombstone.hook.tombstone.support.atsLogDApp
import com.v2dawn.autotombstone.hook.tombstone.support.atsLogE
import com.v2dawn.autotombstone.ui.activity.MainActivity

class TileHook : YukiBaseHooker() {

    companion object {
        var atsConfigService: IAtsConfigService? = null
    }

    fun getAtsService(context: Context): IAtsConfigService? {
        if (atsConfigService == null) {
            try {
                val binder: IBinder = ClassEnum.ServiceManagerClass.clazz
                    .method {
                        name = "getService"
                        param(StringType)
                    }.get().invoke<IBinder>(AtsConfigService.serviceName)!!

                atsConfigService =
                    IAtsConfigService.Stub.asInterface(binder)
            } catch (e: Exception) {
                atsLogE("ats config service get error", e = e)
            }

        }

        return atsConfigService
    }

    fun getFreezeApps(context: Context): MutableList<String> {
        return getAtsService(context)!!.queryBackgroundApps() ?: mutableListOf()
    }

    override fun onHook() {
        ClassEnum.QSTileImplClass.hook {
            injectMember {
                method {
                    name = "handleRefreshState"
                    param(Object::class.java)
                }
                beforeHook {
                    if (ClassEnum.CustomTileClass == instanceClass.name) {
                        val componentName = instanceClass.field {
                            name = "mComponent"
                        }.get(instance).cast<ComponentName>()


                        val context = instanceClass.field {
                            name = "mUserContext"
                            superClass()
                        }.get(instance).cast<Context>()

                        if (componentName != null && context != null) {
                            val pkgName = componentName.packageName

                            if (getFreezeApps(context).contains(pkgName)) {
                                atsLogDApp(
                                    "[${pkgName}] in freeze ,tile refresh disable",
                                    getAtsService(context)!!
                                )
                                resultNull()
                            }
                        }
                    }
                }
            }
            injectMember {
                method {
                    name = "click"
                    param(ViewClass)
                }
                beforeHook {
                    if (ClassEnum.CustomTileClass == instanceClass.name) {
                        val componentName = instanceClass.field {
                            name = "mComponent"
                        }.get(instance).cast<ComponentName>()


                        val context = instanceClass.field {
                            name = "mUserContext"
                            superClass()
                        }.get(instance).cast<Context>()

                        if (componentName != null && context != null) {
                            val pkgName = componentName.packageName

                            if (getFreezeApps(context).contains(pkgName)) {
                                atsLogDApp(
                                    "[${pkgName}] click tile called unfreeze ",
                                    getAtsService(context)!!
                                )
                                getAtsService(context)?.unControl(pkgName)
                            } else {
                                atsLogDApp("[${pkgName}] click tile", getAtsService(context)!!)
                            }
                        }
                    }
//                    else {
//
//                        //ignore system
//                        val context = instance.javaClass.field {
//                            name = "mContext"
//                            superClass()
//                        }.get(instance).cast<Context>()
//                        atsLogI("QSTileImpl context: ${context?.packageName}")
//                    }

                }
            }
        }

    }
}