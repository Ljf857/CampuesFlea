package com.example.campusflea.data.model

import com.google.gson.annotations.SerializedName

data class LoginRequest(
    @SerializedName("phone") val phone: String,
    @SerializedName("password") val password: String
)

data class LoginResponse(
    @SerializedName("accessToken") val accessToken: String,
    @SerializedName("refreshToken") val refreshToken: String,
    @SerializedName("expiresIn") val expiresIn: Long,
    @SerializedName("userInfo") val userInfo: UserInfo
)

data class RegisterRequest(
    @SerializedName("phone") val phone: String,
    @SerializedName("password") val password: String,
    @SerializedName("smsCode") val smsCode: String,
    @SerializedName("nickname") val nickname: String
)

data class SmsLoginRequest(
    @SerializedName("phone") val phone: String,
    @SerializedName("smsCode") val smsCode: String
)

data class SmsRequest(
    @SerializedName("phone") val phone: String,
    @SerializedName("type") val type: String
)

data class ResetPasswordRequest(
    @SerializedName("phone") val phone: String,
    @SerializedName("smsCode") val smsCode: String,
    @SerializedName("newPassword") val newPassword: String
)

data class UserInfo(
    @SerializedName("id") val id: Long = 0,
    @SerializedName("phone") val phone: String = "",
    @SerializedName("nickname") val nickname: String = "",
    @SerializedName("avatar") val avatar: String = "",
    @SerializedName("gender") val gender: Int = 0,
    @SerializedName("genderDesc") val genderDesc: String = "",
    @SerializedName("signature") val signature: String = "",
    @SerializedName("campus") val campus: String = "",
    @SerializedName("verifyStatus") val verifyStatus: Int = 0,
    @SerializedName("verifyStatusDesc") val verifyStatusDesc: String = "",
    @SerializedName("status") val status: Int = 1,
    @SerializedName("registerTime") val registerTime: String = "",
    @SerializedName("stats") val stats: UserStats? = null
)

data class UserStats(
    @SerializedName("goodsCount") val goodsCount: Int = 0,
    @SerializedName("soldCount") val soldCount: Int = 0,
    @SerializedName("favoriteCount") val favoriteCount: Int = 0
)

data class ProfileUpdateRequest(
    @SerializedName("nickname") val nickname: String,
    @SerializedName("avatar") val avatar: String? = null,
    @SerializedName("gender") val gender: Int? = null,
    @SerializedName("signature") val signature: String? = null,
    @SerializedName("campus") val campus: String? = null
)

data class UpdatePhoneRequest(
    @SerializedName("newPhone") val newPhone: String,
    @SerializedName("smsCode") val smsCode: String
)

data class PasswordChangeRequest(
    @SerializedName("oldPassword") val oldPassword: String,
    @SerializedName("newPassword") val newPassword: String
)

data class VerifyStatusInfo(
    @SerializedName("verifyStatus") val verifyStatus: Int,
    @SerializedName("verifyStatusDesc") val verifyStatusDesc: String,
    @SerializedName("rejectReason") val rejectReason: String? = null
)

data class AddressInfo(
    @SerializedName("id") val id: Long = 0,
    @SerializedName("userId") val userId: Long = 0,
    @SerializedName("receiver") val receiver: String = "",
    @SerializedName("phone") val phone: String = "",
    @SerializedName("province") val province: String = "",
    @SerializedName("city") val city: String = "",
    @SerializedName("district") val district: String = "",
    @SerializedName("detail") val detail: String = "",
    @SerializedName("isDefault") val isDefault: Boolean = false
) {
    fun getFullAddress(): String = "$province$city$district$detail"
}

data class AddressRequest(
    @SerializedName("receiver") val receiver: String,
    @SerializedName("phone") val phone: String,
    @SerializedName("province") val province: String,
    @SerializedName("city") val city: String,
    @SerializedName("district") val district: String,
    @SerializedName("detail") val detail: String,
    @SerializedName("isDefault") val isDefault: Boolean = false
)

data class SendSmsResponse(
    @SerializedName("expireIn") val expireIn: Int = 0,
    /**
     * 验证码明文。**仅当服务端开启「模拟短信」时返回**，供本地联调自动回填；
     * 生产环境走阿里云真实短信通道，该字段不会下发，解析后为 null，
     * 此时需由用户从手机短信中读取并手动输入。
     */
    @SerializedName("code") val code: String? = null
)

data class TokenResponse(
    @SerializedName("accessToken") val accessToken: String,
    @SerializedName("refreshToken") val refreshToken: String,
    @SerializedName("expiresIn") val expiresIn: Long
)

data class UploadResponse(
    @SerializedName("url") val url: String,
    @SerializedName("fileName") val fileName: String,
    @SerializedName("size") val size: Long
)
