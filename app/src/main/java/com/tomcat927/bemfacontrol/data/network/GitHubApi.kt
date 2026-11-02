package com.tomcat927.bemfacontrol.data.network

import com.tomcat927.bemfacontrol.data.model.GitHubRelease
import com.tomcat927.bemfacontrol.data.model.GitHubGist
import com.tomcat927.bemfacontrol.data.model.GitHubGistRequest
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import java.util.concurrent.TimeUnit

interface GitHubApi {

    @GET("repos/tomcat927/bemfa-control/releases/latest")
    suspend fun latestRelease(): GitHubRelease

    @POST("gists")
    suspend fun createGist(
        @Header("Authorization") authorization: String,
        @Body request: GitHubGistRequest,
    ): GitHubGist

    @GET("gists/{id}")
    suspend fun getGist(
        @Header("Authorization") authorization: String,
        @Path("id") id: String,
    ): GitHubGist

    @PATCH("gists/{id}")
    suspend fun updateGist(
        @Header("Authorization") authorization: String,
        @Path("id") id: String,
        @Body request: GitHubGistRequest,
    ): GitHubGist
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
            .addInterceptor { chain ->
                chain.proceed(
                    chain.request().newBuilder()
                        .header("Accept", "application/vnd.github+json")
                        .header("X-GitHub-Api-Version", "2022-11-28")
                        .header("User-Agent", "bemfa-control")
                        .build(),
                )
            }
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
