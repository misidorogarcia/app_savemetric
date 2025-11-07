package com.misidoro.app_savemetric.data

object RetrofitClient {
    val api: ApiService by lazy {
        // Reusa el retrofit configurado en RetrofitProvider (okHttp + logging + timeouts)
        RetrofitProvider.create()
    }
}