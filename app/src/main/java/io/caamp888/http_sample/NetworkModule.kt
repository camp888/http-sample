package io.caamp888.http_sample

import android.content.Context
import android.net.ConnectivityManager
import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

class ApiClient {
    companion object {
        @Volatile
        var INSTANCE: ApiService? = null

        fun getApiClient(context: Context): ApiService {
            return INSTANCE ?: synchronized(this) {
                val instance = createRetrofitClient(context.applicationContext)
                INSTANCE = instance
                return instance
            }
        }
    }
}

inline fun<T> request(block: () -> Response<T>): Result<Response<T>> {
    return runCatching { block() }
}

private fun createRetrofitClient(context: Context): ApiService {
    return Retrofit.Builder()
        .baseUrl("todo")
        .client(createClient(context))
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(ApiService::class.java)
}

private fun createClient(context: Context): OkHttpClient {
    return OkHttpClient.Builder()
        .addInterceptor(HttpLoggingInterceptor {
            Log.d("HTTP", it)
        }.also { it.level = HttpLoggingInterceptor.Level.BODY })
        .addInterceptor { chain ->
            val connManager =
                context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val activityInfo = connManager.activeNetworkInfo
            if (activityInfo?.isConnected == false) {
                throw NoInternetConnectionException()
            } else {
                chain.proceed(chain.request())
            }

        }
        .build()
}

class NoInternetConnectionException : Exception()

interface ApiService {
    @POST("/endpoint-name")
    suspend fun post(@Body entity: SomeEntity): Response<Any>

    @GET("/endpoint-name")
    suspend fun get(): Response<SomeEntity>
}

