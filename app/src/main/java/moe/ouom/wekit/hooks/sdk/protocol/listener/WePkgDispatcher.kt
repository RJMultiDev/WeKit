package moe.ouom.wekit.hooks.sdk.protocol.listener

import de.robv.android.xposed.XposedHelpers
import moe.ouom.wekit.core.model.ApiHookItem
import moe.ouom.wekit.hooks.core.annotation.HookItem
import moe.ouom.wekit.hooks.sdk.protocol.WePkgManager
import moe.ouom.wekit.util.log.WeLogger

@HookItem(path = "protocol/wepkg_dispatcher", desc = "WePkg 请求/响应数据包拦截与篡改")
class WePkgDispatcher : ApiHookItem() {

    override fun entry(classLoader: ClassLoader) {
        val netSceneBaseClass = XposedHelpers.findClass("com.tencent.mm.modelbase.m1", classLoader)
        val callbackInterface = XposedHelpers.findClass("com.tencent.mm.network.l0", classLoader)

        val dispatchMethod = XposedHelpers.findMethodExact(
            netSceneBaseClass, "dispatch",
            "com.tencent.mm.network.s", "com.tencent.mm.network.v0", "com.tencent.mm.network.l0"
        )

        hookBefore(dispatchMethod) { param ->
            val v0Var = param.args[1] ?: return@hookBefore
            val originalCallback = param.args[2] ?: return@hookBefore

            val uri = XposedHelpers.callMethod(v0Var, "getUri") as String
            val cgiId = XposedHelpers.callMethod(v0Var, "getType") as Int
            try {
                val reqWrapper = XposedHelpers.callMethod(v0Var, "getReqObj")
                val reqPbObj = XposedHelpers.getObjectField(reqWrapper, "a") // m.a
                val reqBytes = XposedHelpers.callMethod(reqPbObj, "toByteArray") as ByteArray

                WePkgManager.handleRequestTamper(uri, cgiId, reqBytes)?.let { tampered ->
                    XposedHelpers.callMethod(reqPbObj, "parseFrom", tampered)
                    WeLogger.i("PkgDispatcher", "Request Tampered: $uri")
                }
            } catch (_: Throwable) {  }

            if (java.lang.reflect.Proxy.isProxyClass(originalCallback.javaClass)) return@hookBefore

            param.args[2] = java.lang.reflect.Proxy.newProxyInstance(
                classLoader,
                arrayOf(callbackInterface)
            ) { _, method, args ->
                when (method.name) {
                    "hashCode" -> return@newProxyInstance originalCallback.hashCode()
                    "toString" -> return@newProxyInstance originalCallback.toString()
                    "equals" -> return@newProxyInstance originalCallback.equals(args?.get(0))
                    "onGYNetEnd" -> {
                        try {
                            val respV0 = args!![4] ?: v0Var
                            val respWrapper = XposedHelpers.getObjectField(respV0, "b") // n
                            val respPbObj = XposedHelpers.getObjectField(respWrapper, "a") // PB 实例
                            val originalRespBytes = XposedHelpers.callMethod(respPbObj, "toByteArray") as ByteArray

                            WePkgManager.handleResponseTamper(uri, cgiId, originalRespBytes)?.let { tampered ->
                                XposedHelpers.callMethod(respPbObj, "parseFrom", tampered)
                                WeLogger.i("PkgDispatcher", "Response Tampered (Memory): $uri")
                            }
                        } catch (t: Throwable) {
                            WeLogger.e("PkgDispatcher", "Tamper inner logic fail", t)
                        }
                    }
                }

                return@newProxyInstance method.invoke(originalCallback, *(args ?: emptyArray()))
            }
        }
    }
}