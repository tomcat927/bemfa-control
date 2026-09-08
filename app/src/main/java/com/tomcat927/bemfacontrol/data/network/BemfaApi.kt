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

    @GET("va/online")
    suspend fun checkOnline(
        @Query("uid") uid: String,
        @Query("topic") topic: String,
        @Query("type") type: Int,
    ): BemfaResponse<Boolean>

    @POST("va/modifyName")
    suspend fun modifyName(
        @Body request: BemfaNameRequest,
    ): BemfaResponse<Int>

    @GET("vb/api/v1/allRoom")
    suspend fun allRooms(
        @Query("openID") uid: String,
        @Query("type") type: Int,
    ): BemfaResponse<BemfaRoomList>

    @POST("vb/api/v1/changeTopicRoom")
    suspend fun changeTopicRoom(
        @Body request: BemfaChangeRoomRequest,
    ): BemfaResponse<Int>

    @GET("vb/delay/v1/timeList")
    suspend fun timerList(
        @Query("openID") uid: String,
        @Query("topicID") topic: String,
        @Query("type") type: Int,
    ): BemfaResponse<BemfaTimerList>

    @POST("vb/delay/v1/addTime")
    suspend fun addTimer(
        @Body request: BemfaTimerRequest,
    ): BemfaResponse<Int>

    @POST("vb/delay/v1/enableTime")
    suspend fun enableTimer(
        @Body request: BemfaTimerToggleRequest,
    ): BemfaResponse<Int>

    @POST("vb/delay/v1/disableTime")
    suspend fun disableTimer(
        @Body request: BemfaTimerToggleRequest,
    ): BemfaResponse<Int>

    @POST("vb/delay/v1/deleteTime")
    suspend fun deleteTimer(
        @Body request: BemfaTimerToggleRequest,
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
            encodeDefaults = true
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
