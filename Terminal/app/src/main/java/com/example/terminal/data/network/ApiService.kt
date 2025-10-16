package com.example.terminal.data.network

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface ApiService {
    @POST("clock-in")
    suspend fun clockIn(@Body request: ClockInRequest): Response<ApiResponse>

    @POST("clock-out")
    suspend fun clockOut(@Body request: ClockOutRequest): Response<ApiResponse>

    @GET("users/{userId}")
    suspend fun getUserStatus(@Path("userId") userId: String): Response<UserStatusResponse>
}
