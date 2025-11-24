package com.example.apptest.core.network

import android.content.Context
import com.example.apptest.core.storage.SessionManager
import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {
    @Volatile private var retrofit: Retrofit? = null
    @Volatile private var retrofitAuth: Retrofit? = null

    fun getRetrofit(context: Context): Retrofit {
        return retrofit ?: synchronized(this) {
            retrofit ?: buildRetrofit(context).also { retrofit = it }
        }
    }

    fun getRetrofitAuth(context: Context): Retrofit {
        return retrofitAuth ?: synchronized(this) {
            retrofitAuth ?: buildRetrofitWithBase(context, com.example.apptest.BuildConfig.XANO_AUTH_BASE_URL).also { retrofitAuth = it }
        }
    }

    private fun buildRetrofit(context: Context): Retrofit {
        return buildRetrofitWithBase(context, com.example.apptest.BuildConfig.XANO_BASE_URL)
    }

    private fun buildRetrofitWithBase(context: Context, baseUrl: String): Retrofit {
        val sessionManager = SessionManager.getInstance(context.applicationContext)
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        val client = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(sessionManager))
            .addInterceptor(logging)
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.SECONDS)
            .writeTimeout(5, TimeUnit.SECONDS)
            .build()
        val gson = GsonBuilder().setLenient().create()
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }
}
