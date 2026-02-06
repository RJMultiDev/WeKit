package moe.ouom.wekit.hooks.sdk.protocol

import java.util.concurrent.CopyOnWriteArrayList

interface WePkgInterceptor {
    fun onRequest(uri: String, cgiId: Int, reqBytes: ByteArray): ByteArray? = null
    fun onResponse(uri: String, cgiId: Int, respBytes: ByteArray): ByteArray? = null
}

object WePkgManager {
    private val listeners = CopyOnWriteArrayList<WePkgInterceptor>()

    fun addInterceptor(interceptor: WePkgInterceptor) = listeners.addIfAbsent(interceptor)

    internal fun handleRequestTamper(uri: String, cgiId: Int, reqBytes: ByteArray): ByteArray? {
        for (listener in listeners) {
            val tampered = listener.onRequest(uri, cgiId, reqBytes)
            if (tampered != null) return tampered
        }
        return null
    }

    internal fun handleResponseTamper(uri: String, cgiId: Int, respBytes: ByteArray): ByteArray? {
        for (listener in listeners) {
            val tampered = listener.onResponse(uri, cgiId, respBytes)
            if (tampered != null) return tampered
        }
        return null
    }
}