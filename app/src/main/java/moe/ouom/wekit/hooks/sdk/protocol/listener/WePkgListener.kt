package moe.ouom.wekit.hooks.sdk.protocol.listener

import de.robv.android.xposed.XposedHelpers
import moe.ouom.wekit.core.model.ApiHookItem
import moe.ouom.wekit.hooks.core.annotation.HookItem
import moe.ouom.wekit.hooks.sdk.api.WeMessageApi
import moe.ouom.wekit.hooks.sdk.protocol.WePkgHelper
import moe.ouom.wekit.util.FunProtoData
import moe.ouom.wekit.util.common.SyncUtils
import moe.ouom.wekit.util.log.WeLogger
import java.lang.reflect.Modifier

@HookItem(path = "protocol/微信数据包监听", desc = "NetScene 监控与配置生成")
class WePkgListener : ApiHookItem() {

    companion object {
        private var DEBUG = true

    }
    override fun entry(classLoader: ClassLoader) {
        SyncUtils.postDelayed(3000) {
            if (DEBUG) {
                hookBuilder() // debug use only
            }
            hookDispatch()
        }
    }

    private fun hookBuilder() {
        val builderClass = WePkgHelper.INSTANCE?.dexClsConfigBuilder?.clazz

        try {
            if (builderClass == null) {
                WeLogger.e("WePkgListener-gen", "找不到 Builder 类")
                return
            }
            WeLogger.i("WePkgListener-gen", "start Hook ${builderClass.name}.a() 方法")
            hookAfter(builderClass, "a") { param ->
                val builder = param.thisObject

                val cgiId = try {
                    XposedHelpers.getIntField(builder, "d")
                } catch (_: Throwable) {
                    0
                }
                val funcId = try {
                    XposedHelpers.getIntField(builder, "e")
                } catch (_: Throwable) {
                    0
                }
                val routeId = try {
                    XposedHelpers.getIntField(builder, "f")
                } catch (_: Throwable) {
                    0
                }
                val uri = try {
                    XposedHelpers.getObjectField(builder, "c") as? String ?: ""
                } catch (_: Throwable) {
                    ""
                }

                // 获取 Request 对象的类名
                var reqClassName = "Unknown"
                try {
                    val reqObj = XposedHelpers.getObjectField(builder, "a")
                    if (reqObj != null) {
                        reqClassName = reqObj.javaClass.name
                    }
                } catch (_: Throwable) { }

                val configLog = "$cgiId to Triple(\"$reqClassName\", $funcId, $routeId), // $uri"
                WeLogger.w("WePkgListener-gen", configLog)
            }
        } catch (e: Throwable) {
            WeLogger.e("WePkgListener-gen", "Builder Hook 失败: ${e.message}")
        }
    }

    private fun hookDispatch() {
        val netSceneBaseClass = WeMessageApi.INSTANCE?.dexClassNetSceneBase?.clazz
        val pbBaseClass = WePkgHelper.INSTANCE?.dexClsProtoBase?.clazz

        if (netSceneBaseClass != null && pbBaseClass != null) {
            WeLogger.i("WePkgListener", "start Hook ${netSceneBaseClass.name}.dispatch")
            hookBefore(netSceneBaseClass, "dispatch") { param ->
                try {
                    val rrObj = param.args[1] ?: return@hookBefore
                    val uri = XposedHelpers.callMethod(rrObj, "getUri") as? String ?: "null"
                    val cgiId = XposedHelpers.callMethod(rrObj, "getType") as? Int ?: 0

                    if (isIgnoredCgi(uri, cgiId)) return@hookBefore

                    val realReqPb = findPbObjectSafe(rrObj, pbBaseClass.name, 0, 3)

                    if (realReqPb != null) {
                        if (DEBUG) WeLogger.i("WePkgListener", ">>> [捕获包体] $uri ($cgiId)")
                        try {
                            val pbBytes = XposedHelpers.callMethod(realReqPb, "toByteArray") as? ByteArray
                            if (pbBytes != null && pbBytes.isNotEmpty()) {
                                val data = FunProtoData()
                                data.fromBytes(pbBytes)
                                if (DEBUG) WeLogger.d("WePkgListener", "JSON: ${data.toJSON()}")
                            }
                        } catch (_: Exception) { }
                        if (DEBUG) WeLogger.printStackTrace()
                    }
                } catch (t: Throwable) {
                    if (DEBUG) WeLogger.e("WePkgListener", "Dispatch 扫描异常: ${t.message}")
                }
            }
        }
    }

    private fun findPbObjectSafe(instance: Any?, targetClassStr: String, currentDepth: Int, maxDepth: Int): Any? {
        if (instance == null || currentDepth > maxDepth) return null
        val clazz = instance.javaClass
        if (clazz.name.startsWith("java.") || clazz.name.startsWith("android.")) return null
        if (isInstanceOf(clazz, targetClassStr)) return instance

        try {
            var currentClass: Class<*>? = clazz
            while (currentClass != null && currentClass.name != "java.lang.Object") {
                for (field in currentClass.declaredFields) {
                    if (Modifier.isStatic(field.modifiers)) continue
                    field.isAccessible = true
                    val value = field.get(instance) ?: continue
                    val found = findPbObjectSafe(value, targetClassStr, currentDepth + 1, maxDepth)
                    if (found != null) return found
                }
                currentClass = currentClass.superclass
            }
        } catch (e: Exception) { }
        return null
    }

    private fun isInstanceOf(clazz: Class<*>, targetName: String): Boolean {
        var current: Class<*>? = clazz
        while (current != null) {
            if (current.name == targetName) return true
            current = current.superclass
        }
        return false
    }

    private fun isIgnoredCgi(uri: String, id: Int): Boolean {
        return false
//        return uri.contains("report") || uri.contains("log") || id == 381 || id == 988 || id == 2723
    }
}