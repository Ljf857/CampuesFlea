package com.example.campusflea.data.repository

import android.content.Context
import android.net.Uri
import com.example.campusflea.data.model.*
import com.example.campusflea.data.remote.RetrofitClient
import com.example.campusflea.data.remote.toFault
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

class UserRepository {
    private val api = RetrofitClient.apiService

    suspend fun getUserInfo(): Result<UserInfo> = try {
        val r = api.getUserInfo()
        if (r.isSuccess() && r.data != null) Result.success(r.data) else Result.failure(Exception(r.message))
    } catch (e: Exception) { Result.failure(e.toFault()) }

    suspend fun updateProfile(request: ProfileUpdateRequest): Result<Unit> = try {
        val r = api.updateProfile(request)
        if (r.isSuccess()) Result.success(Unit) else Result.failure(Exception(r.message))
    } catch (e: Exception) { Result.failure(e.toFault()) }

    suspend fun updatePhone(newPhone: String, smsCode: String): Result<Unit> = try {
        val r = api.updatePhone(UpdatePhoneRequest(newPhone, smsCode))
        if (r.isSuccess()) Result.success(Unit) else Result.failure(Exception(r.message))
    } catch (e: Exception) { Result.failure(e.toFault()) }

    suspend fun changePassword(oldPwd: String, newPwd: String): Result<Unit> = try {
        val r = api.changePassword(PasswordChangeRequest(oldPwd, newPwd))
        if (r.isSuccess()) Result.success(Unit) else Result.failure(Exception(r.message))
    } catch (e: Exception) { Result.failure(e.toFault()) }

    suspend fun getVerifyStatus(): Result<VerifyStatusInfo> = try {
        val r = api.getVerifyStatus()
        if (r.isSuccess() && r.data != null) Result.success(r.data) else Result.failure(Exception(r.message))
    } catch (e: Exception) { Result.failure(e.toFault()) }

    suspend fun getAddressList(): Result<List<AddressInfo>> = try {
        val r = api.getAddressList()
        if (r.isSuccess() && r.data != null) Result.success(r.data) else Result.failure(Exception(r.message))
    } catch (e: Exception) { Result.failure(e.toFault()) }

    suspend fun addAddress(request: AddressRequest): Result<Unit> = try {
        val r = api.addAddress(request)
        if (r.isSuccess()) Result.success(Unit) else Result.failure(Exception(r.message))
    } catch (e: Exception) { Result.failure(e.toFault()) }

    suspend fun updateAddress(id: Long, request: AddressRequest): Result<Unit> = try {
        val r = api.updateAddress(id, request)
        if (r.isSuccess()) Result.success(Unit) else Result.failure(Exception(r.message))
    } catch (e: Exception) { Result.failure(e.toFault()) }

    suspend fun deleteAddress(id: Long): Result<Unit> = try {
        val r = api.deleteAddress(id)
        if (r.isSuccess()) Result.success(Unit) else Result.failure(Exception(r.message))
    } catch (e: Exception) { Result.failure(e.toFault()) }

    suspend fun uploadAvatar(context: Context, uri: Uri): Result<String> = try {
        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: return Result.failure(Exception("无法读取图片"))
        val mimeType = context.contentResolver.getType(uri) ?: "image/jpeg"
        val requestBody = bytes.toRequestBody(mimeType.toMediaTypeOrNull())
        val part = MultipartBody.Part.createFormData("file", "avatar.jpg", requestBody)
        val r = api.uploadImage(part)
        if (r.isSuccess() && r.data != null) Result.success(r.data.url) else Result.failure(Exception(r.message))
    } catch (e: Exception) { Result.failure(e.toFault()) }

    suspend fun submitVerify(realName: String, studentNo: String, frontUri: Uri, backUri: Uri, context: Context): Result<Unit> = try {
        val nameBody = realName.toRequestBody("text/plain".toMediaTypeOrNull())
        val studentBody = studentNo.toRequestBody("text/plain".toMediaTypeOrNull())
        val frontBytes = context.contentResolver.openInputStream(frontUri)?.use { it.readBytes() }
            ?: return Result.failure(Exception("无法读取证件正面图片"))
        val backBytes = context.contentResolver.openInputStream(backUri)?.use { it.readBytes() }
            ?: return Result.failure(Exception("无法读取证件反面图片"))
        val frontMime = context.contentResolver.getType(frontUri) ?: "image/jpeg"
        val backMime = context.contentResolver.getType(backUri) ?: "image/jpeg"
        val frontPart = MultipartBody.Part.createFormData("idCardFront", "front.jpg", frontBytes.toRequestBody(frontMime.toMediaTypeOrNull()))
        val backPart = MultipartBody.Part.createFormData("idCardBack", "back.jpg", backBytes.toRequestBody(backMime.toMediaTypeOrNull()))
        val r = api.submitVerify(nameBody, studentBody, frontPart, backPart)
        if (r.isSuccess()) Result.success(Unit) else Result.failure(Exception(r.message))
    } catch (e: Exception) { Result.failure(e.toFault()) }
}
