package com.example.vehicle_tracking_driver_app.models

data class DriverSignupRequest(
    val name: String,
    val email: String,
    val password: String,
    val phone: String,
    val plateNumber: String,
    val photo: String? = null
)

data class DriverLoginRequest(
    val email: String,
    val password: String
)

data class LoginResponse(
    val token: String,
    val driverId: String
)

data class DriverProfile(
    val _id: String,
    val name: String,
    val email: String,
    val phone: String,
    val plateNumber: String,
    val photo: String?,
    val createdAt: String?,
    val updatedAt: String?
)

data class UpdateProfileRequest(
    val name: String?,
    val email: String?,
    val phone: String?,
    val plateNumber: String?,
    val photo: String?
)

data class Request(
    val _id: String,
    val user: User,
    val driverId: String,
    val status: String, // pending, accepted, rejected, boarded, reached
    val createdAt: String,
    val updatedAt: String
)

data class GenericResponse(
    val message: String
)

data class TokenUpdateRequest(
    val token: String
)

data class DriverRequest(
    val driverId: String
)

data class User(
    val _id: String,
    val name: String,
    val email: String,
    val phone: String,
)

data class UserResponse(
    val _id: String,
    val name: String,
    val email: String,
    val phone: String
)