package com.example.vehicle_tracking_driver_app.network

import com.example.vehicle_tracking_driver_app.models.*
import retrofit2.Call
import retrofit2.http.*

interface ApiService {
    // Driver signup and login
    @POST("api/driver/signup")
    fun signup(@Body signupRequest: DriverSignupRequest): Call<GenericResponse>

    @POST("api/driver/login")
    fun login(@Body loginRequest: DriverLoginRequest): Call<LoginResponse>
    @POST("api/driver/request")
    fun requestDriver(@Header("Authorization") token: String, @Body request: DriverRequest): Call<GenericResponse>
    // Profile endpoints
    @GET("api/driver/profile")
    fun getProfile(@Header("Authorization") token: String): Call<DriverProfile>

    @PUT("api/driver/profile")
    fun updateProfile(@Header("Authorization") token: String, @Body updateRequest: UpdateProfileRequest): Call<DriverProfile>

    // Requests endpoints for driver
    @GET("api/driver/requests")
    fun getRequests(@Header("Authorization") token: String): Call<List<Request>>

    @PUT("api/driver/requests/{id}/accept")
    fun acceptRequest(@Header("Authorization") token: String, @Path("id") requestId: String): Call<GenericResponse>

    @PUT("api/driver/requests/{id}/reject")
    fun rejectRequest(@Header("Authorization") token: String, @Path("id") requestId: String): Call<GenericResponse>

    // Optionally board and reach endpoints:
    @PUT("api/driver/requests/{id}/board")
    fun boardRequest(@Header("Authorization") token: String, @Path("id") requestId: String): Call<GenericResponse>

    @PUT("api/driver/requests/{id}/reach")
    fun reachRequest(@Header("Authorization") token: String, @Path("id") requestId: String): Call<GenericResponse>

    // Update FCM token for driver
    @PUT("api/driver/updateToken")
    fun updateToken(@Header("Authorization") token: String, @Body tokenUpdate: TokenUpdateRequest): Call<GenericResponse>

    @GET("api/user/{userID}")
    fun getUserById(@Header("Authorization") token: String, @Path("userID") userId: String): Call<UserResponse>

    @GET("api/driver/acceptedRequests")
    fun getAcceptedRequests(@Header("Authorization") token: String): Call<List<Request>>
    @PUT("api/driver/request/board")
    fun boardUserRequest(@Header("Authorization") token: String, @Query("userId") userId: String): Call<GenericResponse>

}
