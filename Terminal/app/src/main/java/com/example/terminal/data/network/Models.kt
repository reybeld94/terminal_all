package com.example.terminal.data.network

import com.google.gson.annotations.SerializedName

data class ClockInRequest(
    @SerializedName("workOrderAssemblyId") val workOrderAssemblyId: Int,
    @SerializedName("userId") val userId: String,
    @SerializedName("divisionFK") val divisionFK: Int,
    @SerializedName("deviceDate") val deviceDate: String
)

data class ClockInResponse(
    @SerializedName("status") val status: String?,
    @SerializedName("workOrderCollectionId") val workOrderCollectionId: Int?
)

data class ClockOutRequest(
    @SerializedName("workOrderCollectionId") val workOrderCollectionId: Int,
    @SerializedName("quantity") val quantity: Int,
    @SerializedName("quantityScrapped") val quantityScrapped: Int,
    @SerializedName("scrapReasonPK") val scrapReasonPK: Int,
    @SerializedName("complete") val complete: Boolean,
    @SerializedName("comment") val comment: String,
    @SerializedName("deviceTime") val deviceTime: String,
    @SerializedName("divisionFK") val divisionFK: Int
)

data class ClockOutResponse(
    @SerializedName("status") val status: String?
)

data class UserStatusResponse(
    @SerializedName("userId") val userId: Int,
    @SerializedName("firstName") val firstName: String,
    @SerializedName("lastName") val lastName: String,
    @SerializedName("workOrderCollectionId") val workOrderCollectionId: Int?,
    @SerializedName("workOrderNumber") val workOrderNumber: String?,
    @SerializedName("workOrderAssemblyNumber") val workOrderAssemblyNumber: String?,
    @SerializedName("clockInTime") val clockInTime: String?,
    @SerializedName("partNumber") val partNumber: String?,
    @SerializedName("operationCode") val operationCode: String?,
    @SerializedName("operationName") val operationName: String?
)

enum class ClockOutStatus(val isComplete: Boolean, val displayName: String) {
    COMPLETE(true, "Complete"),
    INCOMPLETE(false, "Incomplete");

    override fun toString(): String = displayName
}
