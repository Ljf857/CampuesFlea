package com.example.campusflea.data.repository

import com.example.campusflea.CampusFleaApp
import com.example.campusflea.data.local.TokenManager
import com.example.campusflea.data.model.*
import com.example.campusflea.data.remote.RetrofitClient
import com.example.campusflea.data.remote.toFault

class AuthRepository {
    private val api = RetrofitClient.apiService
    private val tokenManager = TokenManager.getInstance(CampusFleaApp.instance)

    suspend fun login(phone: String, password: String): Result<LoginResponse> = try {
        val r = api.login(LoginRequest(phone, password))
        if (r.isSuccess() && r.data != null) {
            tokenManager.saveTokens(r.data.accessToken, r.data.refreshToken, r.data.expiresIn)
            Result.success(r.data)
        } else Result.failure(Exception(r.message))
    } catch (e: Exception) { Result.failure(e.toFault()) }

    suspend fun smsLogin(phone: String, smsCode: String): Result<LoginResponse> = try {
        val r = api.smsLogin(SmsLoginRequest(phone, smsCode))
        if (r.isSuccess() && r.data != null) {
            tokenManager.saveTokens(r.data.accessToken, r.data.refreshToken, r.data.expiresIn)
            Result.success(r.data)
        } else Result.failure(Exception(r.message))
    } catch (e: Exception) { Result.failure(e.toFault()) }

    suspend fun register(phone: String, password: String, smsCode: String, nickname: String): Result<Unit> = try {
        val r = api.register(RegisterRequest(phone, password, smsCode, nickname))
        if (r.isSuccess()) Result.success(Unit) else Result.failure(Exception(r.message))
    } catch (e: Exception) { Result.failure(e.toFault()) }

    suspend fun sendSms(phone: String, type: String): Result<SendSmsResponse> = try {
        val r = api.sendSms(SmsRequest(phone, type))
        if (r.isSuccess() && r.data != null) Result.success(r.data) else Result.failure(Exception(r.message))
    } catch (e: Exception) { Result.failure(e.toFault()) }

    suspend fun resetPassword(phone: String, smsCode: String, newPassword: String): Result<Unit> = try {
        val r = api.resetPassword(ResetPasswordRequest(phone, smsCode, newPassword))
        if (r.isSuccess()) Result.success(Unit) else Result.failure(Exception(r.message))
    } catch (e: Exception) { Result.failure(e.toFault()) }

    suspend fun logout() {
        try { api.logout() } catch (_: Exception) {}
        tokenManager.clearTokens()
    }

    suspend fun isLoggedIn(): Boolean = tokenManager.isLoggedIn()
}
