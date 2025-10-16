package com.example.terminal.data.repository

import android.os.Build
import com.example.terminal.data.local.UserPrefs
import com.example.terminal.data.network.ApiClient
import com.example.terminal.data.network.ClockInRequest
import com.example.terminal.data.network.ClockInResponse
import com.example.terminal.data.network.ClockOutRequest
import com.example.terminal.data.network.ClockOutResponse
import com.example.terminal.data.network.UserStatusResponse
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import com.google.gson.annotations.SerializedName

class WorkOrdersRepository(
    private val userPrefs: UserPrefs,
    private val gson: Gson = Gson()
) {
    suspend fun fetchUserStatus(userId: String): Result<UserStatus> = withContext(Dispatchers.IO) {
        val baseUrl = userPrefs.serverAddress.first()
        try {
            val apiService = ApiClient.getApiService(baseUrl)
            val response = apiService.getUserStatus(userId)
            if (response.isSuccessful) {
                val body = response.body()
                when (body) {
                    null -> Result.failure(IllegalStateException("Respuesta vacía del servidor"))
                    else -> Result.success(body.toDomain())
                }
            } else {
                val errorMessage = parseUserStatusError(response.code(), response.errorBody()?.string())
                Result.failure(IllegalStateException(errorMessage))
            }
        } catch (ex: Exception) {
            Result.failure(ex)
        }
    }

    suspend fun clockIn(
        workOrderAssemblyId: Int,
        userId: Int
    ): Result<ClockInResponse> = withContext(Dispatchers.IO) {
        val baseUrl = userPrefs.serverAddress.first()
        try {
            val apiService = ApiClient.getApiService(baseUrl)
            val response = apiService.clockIn(
                ClockInRequest(
                    workOrderAssemblyId = workOrderAssemblyId,
                    userId = userId.toString(),
                    divisionFK = DIVISION_FK,
                    deviceDate = currentIsoDateTime()
                )
            )

            if (response.isSuccessful) {
                val body = response.body()
                when {
                    body == null -> Result.failure(IllegalStateException("Respuesta vacía del servidor"))
                    !body.status.isSuccessStatus() -> {
                        val message = body.status?.takeIf { it.isNotBlank() }
                            ?: "Operación de Clock In rechazada por el servidor"
                        Result.failure(IllegalStateException(message))
                    }
                    else -> Result.success(body)
                }
            } else {
                val errorMessage = parseError(response.errorBody()?.string())
                Result.failure(IllegalStateException(errorMessage))
            }
        } catch (ex: Exception) {
            Result.failure(ex)
        }
    }

    suspend fun clockOut(
        workOrderCollectionId: Int,
        quantity: Int,
        complete: Boolean,
        quantityScrapped: Int = 0,
        scrapReasonPK: Int = 0,
        comment: String = ""
    ): Result<ClockOutResponse> = withContext(Dispatchers.IO) {
        val baseUrl = userPrefs.serverAddress.first()
        try {
            val apiService = ApiClient.getApiService(baseUrl)
            val response = apiService.clockOut(
                ClockOutRequest(
                    workOrderCollectionId = workOrderCollectionId,
                    quantity = quantity,
                    quantityScrapped = quantityScrapped,
                    scrapReasonPK = scrapReasonPK,
                    complete = complete,
                    comment = comment,
                    deviceTime = currentIsoDateTime(),
                    divisionFK = DIVISION_FK
                )
            )

            if (response.isSuccessful) {
                val body = response.body()
                when {
                    body == null -> Result.failure(IllegalStateException("Respuesta vacía del servidor"))
                    !body.status.isSuccessStatus() -> {
                        val message = body.status?.takeIf { it.isNotBlank() }
                            ?: "Operación de Clock Out rechazada por el servidor"
                        Result.failure(IllegalStateException(message))
                    }
                    else -> Result.success(body)
                }
            } else {
                val errorMessage = parseError(response.errorBody()?.string())
                Result.failure(IllegalStateException(errorMessage))
            }
        } catch (ex: Exception) {
            Result.failure(ex)
        }
    }

    private fun parseUserStatusError(code: Int, errorBody: String?): String {
        if (code == 404) {
            return "Wrong user"
        }
        return parseError(errorBody, defaultMessage = "Error al validar usuario")
    }

    private fun currentIsoDateTime(): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
        } else {
            val formatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US)
            formatter.timeZone = TimeZone.getDefault()
            formatter.format(Date())
        }
    }

    private fun parseError(errorBody: String?, defaultMessage: String = "Error desconocido"): String {
        if (errorBody.isNullOrBlank()) {
            return defaultMessage
        }

        val parsedDetail = parseErrorDetail(errorBody)
        return parsedDetail ?: errorBody
    }

    private fun parseErrorDetail(errorBody: String): String? {
        return try {
            val simpleDetail = gson.fromJson(errorBody, ErrorDetailResponse::class.java)
            when {
                simpleDetail?.detail.isNullOrBlank() -> {
                    val validationDetail = gson.fromJson(errorBody, ValidationErrorResponse::class.java)
                    validationDetail?.detail
                        ?.firstNotNullOfOrNull { it.message?.takeIf { msg -> msg.isNotBlank() } }
                }

                else -> simpleDetail?.detail
            }
        } catch (ex: Exception) {
            null
        }
    }

    private fun String?.isSuccessStatus(): Boolean {
        if (this.isNullOrBlank()) {
            return false
        }
        return equals("success", ignoreCase = true) || equals("ok", ignoreCase = true)
    }

    private companion object {
        const val DIVISION_FK = 1
    }
}

data class UserStatus(
    val userId: Int,
    val firstName: String,
    val lastName: String,
    val activeWorkOrder: ActiveWorkOrder?
)

data class ActiveWorkOrder(
    val workOrderCollectionId: Int?,
    val workOrderNumber: String?,
    val workOrderAssemblyNumber: String?,
    val clockInTime: String?,
    val partNumber: String?,
    val operationCode: String?,
    val operationName: String?
)

private fun UserStatusResponse.toDomain(): UserStatus {
    val hasActiveWorkOrder = workOrderCollectionId != null ||
        !workOrderNumber.isNullOrBlank() ||
        !workOrderAssemblyNumber.isNullOrBlank() ||
        !clockInTime.isNullOrBlank() ||
        !partNumber.isNullOrBlank() ||
        !operationCode.isNullOrBlank() ||
        !operationName.isNullOrBlank()

    val activeWorkOrder = if (hasActiveWorkOrder) {
        ActiveWorkOrder(
            workOrderCollectionId = workOrderCollectionId,
            workOrderNumber = workOrderNumber,
            workOrderAssemblyNumber = workOrderAssemblyNumber,
            clockInTime = clockInTime,
            partNumber = partNumber,
            operationCode = operationCode,
            operationName = operationName
        )
    } else {
        null
    }

    return UserStatus(
        userId = userId,
        firstName = firstName,
        lastName = lastName,
        activeWorkOrder = activeWorkOrder
    )
}

private data class ErrorDetailResponse(
    @SerializedName("detail") val detail: String?
)

private data class ValidationErrorDetail(
    @SerializedName("msg") val message: String?,
    @SerializedName("loc") val location: List<Any>?,
    @SerializedName("type") val type: String?
)

private data class ValidationErrorResponse(
    @SerializedName("detail") val detail: List<ValidationErrorDetail>?
)
