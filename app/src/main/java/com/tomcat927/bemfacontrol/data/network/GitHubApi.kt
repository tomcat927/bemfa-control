package com.tomcat927.bemfacontrol.data.network

import com.tomcat927.bemfacontrol.data.model.GitHubRelease
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.GET
import java.util.concurrent.TimeUnit

interface GitHubApi {

    @GET("repos/tomcat927/bemfa-control/releases/latest")
    suspend fun latestRelease(): GitHubRelease
}

object GitHubApiFactory {

    private const val BASE_URL = "https://api.github.com/"

    fun create(): GitHubApi {
        val json = Json {
            ignoreUnknownKeys = true
            explicitNulls = false
            coerceInputValues = true
        }
        val client = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .build()

        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(GitHubApi::class.java)
    }
}
