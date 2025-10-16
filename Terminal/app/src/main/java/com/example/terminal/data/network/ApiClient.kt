package com.example.terminal.data.network

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {
    const val DEFAULT_BASE_URL = "http://<IP-SERVER>:8080/"

    fun normalizeBaseUrl(baseUrl: String): String {
        var normalized = baseUrl.trim()
        if (normalized.isEmpty()) {
            return DEFAULT_BASE_URL
        }
        if (!normalized.startsWith("http://") && !normalized.startsWith("https://")) {
            normalized = "http://$normalized"
        }
        if (!normalized.endsWith('/')) {
            normalized += "/"
        }
        return normalized
    }

    @Volatile
    private var apiService: ApiService? = null
    @Volatile
    private var currentBaseUrl: String = DEFAULT_BASE_URL

    fun getApiService(baseUrl: String = currentBaseUrl): ApiService {
        val normalizedBaseUrl = normalizeBaseUrl(baseUrl)
        if (normalizedBaseUrl != currentBaseUrl) {
            synchronized(this) {
                if (normalizedBaseUrl != currentBaseUrl) {
                    currentBaseUrl = normalizedBaseUrl
                    apiService = null
                }
            }
        }
        return apiService ?: synchronized(this) {
            apiService ?: buildRetrofit(currentBaseUrl).also { apiService = it }
        }
    }

    fun updateBaseUrl(baseUrl: String) {
        val normalizedBaseUrl = normalizeBaseUrl(baseUrl)
        synchronized(this) {
            currentBaseUrl = normalizedBaseUrl
            apiService = null
        }
    }

    private fun buildRetrofit(baseUrl: String): ApiService {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        return retrofit.create(ApiService::class.java)
    }
}
