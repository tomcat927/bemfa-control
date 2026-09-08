package com.tomcat927.bemfacontrol.data.network

import com.tomcat927.bemfacontrol.data.model.BemfaMessageRequest
import com.tomcat927.bemfacontrol.data.model.BemfaResponse
import com.tomcat927.bemfacontrol.data.model.BemfaTopic
import com.tomcat927.bemfacontrol.data.model.BemfaTopicList
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

interface BemfaApi {

    @GET("vb/api/v2/allTopic")
    suspend fun allTopics(
        @Query("openID") uid: String,
        @Query("type") type: Int,
    ): BemfaResponse<BemfaTopicList>

    @POST("va/postJsonMsg")
    suspend fun postMessage(
        @Body request: BemfaMessageRequest,
    ): BemfaResponse<Int>
}

object BemfaApiFactory {

    private const val BASE_URL = "https://apis.bemfa.com/"
    private const val TCP_DEVICE_TYPE = 3

    fun create(baseUrl: String = BASE_URL): BemfaApi {
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
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(BemfaApi::class.java)
    }

    fun tcpDeviceType(): Int = TCP_DEVICE_TYPE
}
