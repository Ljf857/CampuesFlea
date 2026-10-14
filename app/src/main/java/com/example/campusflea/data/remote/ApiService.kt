package com.example.campusflea.data.remote

import com.example.campusflea.data.model.*
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.*

interface ApiService {

    @POST("/api/auth/login")
    suspend fun login(@Body request: LoginRequest): ApiResult<LoginResponse>

    @POST("/api/auth/login/sms")
    suspend fun smsLogin(@Body request: SmsLoginRequest): ApiResult<LoginResponse>

    @POST("/api/auth/register")
    suspend fun register(@Body request: RegisterRequest): ApiResult<Any>

    @POST("/api/auth/sms")
    suspend fun sendSms(@Body request: SmsRequest): ApiResult<SendSmsResponse>

    @POST("/api/auth/refresh")
    suspend fun refreshToken(@Header("Authorization") refreshToken: String): ApiResult<TokenResponse>

    @POST("/api/auth/logout")
    suspend fun logout(): ApiResult<Any>

    @POST("/api/auth/reset-password")
    suspend fun resetPassword(@Body request: ResetPasswordRequest): ApiResult<Any>

    @GET("/api/user/info")
    suspend fun getUserInfo(): ApiResult<UserInfo>

    @PUT("/api/user/profile")
    suspend fun updateProfile(@Body request: ProfileUpdateRequest): ApiResult<Any>

    @PUT("/api/user/password")
    suspend fun changePassword(@Body request: PasswordChangeRequest): ApiResult<Any>

    @PUT("/api/user/phone")
    suspend fun updatePhone(@Body request: UpdatePhoneRequest): ApiResult<Any>

    @GET("/api/user/verify/status")
    suspend fun getVerifyStatus(): ApiResult<VerifyStatusInfo>

    @Multipart
    @POST("/api/user/verify")
    suspend fun submitVerify(
        @Part("realName") realName: RequestBody,
        @Part("studentNo") studentNo: RequestBody,
        @Part idCardFront: MultipartBody.Part,
        @Part idCardBack: MultipartBody.Part
    ): ApiResult<Any>

    @GET("/api/user/address")
    suspend fun getAddressList(): ApiResult<List<AddressInfo>>

    @POST("/api/user/address")
    suspend fun addAddress(@Body request: AddressRequest): ApiResult<Any>

    @PUT("/api/user/address/{id}")
    suspend fun updateAddress(@Path("id") id: Long, @Body request: AddressRequest): ApiResult<Any>

    @DELETE("/api/user/address/{id}")
    suspend fun deleteAddress(@Path("id") id: Long): ApiResult<Any>

    @GET("/api/goods/recommend")
    suspend fun getRecommend(@Query("page") page: Int = 1, @Query("size") size: Int = 10): ApiResult<PageResult<GoodsInfo>>

    @GET("/api/goods/recommend/personal")
    suspend fun getPersonalRecommend(@Query("page") page: Int = 1, @Query("size") size: Int = 10): ApiResult<PageResult<GoodsInfo>>

    @GET("/api/goods/category/{categoryId}")
    suspend fun getCategoryGoods(
        @Path("categoryId") categoryId: Long,
        @Query("page") page: Int = 1,
        @Query("size") size: Int = 10,
        @Query("sort") sort: String? = null
    ): ApiResult<PageResult<GoodsInfo>>

    @GET("/api/goods/search")
    suspend fun search(
        @Query("keyword") keyword: String? = null,
        @Query("page") page: Int = 1,
        @Query("size") size: Int = 10,
        @Query("categoryId") categoryId: Long? = null,
        @Query("minPrice") minPrice: Double? = null,
        @Query("maxPrice") maxPrice: Double? = null,
        @Query("sort") sort: String? = null
    ): ApiResult<PageResult<GoodsInfo>>

    @GET("/api/goods/search/hot")
    suspend fun getHotKeywords(): ApiResult<List<String>>

    @GET("/api/goods/{id}")
    suspend fun getGoodsDetail(@Path("id") id: Long): ApiResult<GoodsDetail>

    @POST("/api/goods")
    suspend fun publishGoods(@Body request: PublishRequest): ApiResult<Any>

    @PUT("/api/goods/{id}")
    suspend fun updateGoods(@Path("id") id: Long, @Body request: PublishRequest): ApiResult<Any>

    @PUT("/api/goods/{id}/status")
    suspend fun updateGoodsStatus(@Path("id") id: Long, @Body body: Map<String, Int>): ApiResult<Any>

    @DELETE("/api/goods/{id}")
    suspend fun deleteGoods(@Path("id") id: Long): ApiResult<Any>

    @GET("/api/goods/mine")
    suspend fun getMyGoods(
        @Query("page") page: Int = 1,
        @Query("size") size: Int = 10,
        @Query("status") status: Int? = null
    ): ApiResult<PageResult<GoodsInfo>>

    @POST("/api/goods/{id}/favorite")
    suspend fun favorite(@Path("id") id: Long): ApiResult<Any>

    @DELETE("/api/goods/{id}/favorite")
    suspend fun unfavorite(@Path("id") id: Long): ApiResult<Any>

    @GET("/api/goods/favorites")
    suspend fun getFavorites(@Query("page") page: Int = 1, @Query("size") size: Int = 10): ApiResult<PageResult<GoodsInfo>>

    @GET("/api/goods/history")
    suspend fun getHistory(@Query("page") page: Int = 1, @Query("size") size: Int = 20): ApiResult<PageResult<GoodsInfo>>

    @DELETE("/api/goods/history/{id}")
    suspend fun deleteHistory(@Path("id") id: Long): ApiResult<Any>

    @DELETE("/api/goods/history")
    suspend fun clearHistory(): ApiResult<Any>

    @POST("/api/goods/{id}/report")
    suspend fun report(@Path("id") id: Long, @Body request: ReportRequest): ApiResult<Any>

    @GET("/api/goods/categories")
    suspend fun getCategories(): ApiResult<List<CategoryInfo>>

    @GET("/api/public/banners")
    suspend fun getBanners(): ApiResult<List<BannerInfo>>

    @POST("/api/orders")
    suspend fun createOrder(@Body request: CreateOrderRequest): ApiResult<Long>

    @GET("/api/orders")
    suspend fun getOrders(
        @Query("page") page: Int = 1,
        @Query("size") size: Int = 10,
        @Query("type") type: String? = null,
        @Query("status") status: Int? = null
    ): ApiResult<PageResult<OrderInfo>>

    @GET("/api/orders/{id}")
    suspend fun getOrderDetail(@Path("id") id: Long): ApiResult<OrderDetail>

    @PUT("/api/orders/{id}/confirm")
    suspend fun confirmOrder(@Path("id") id: Long): ApiResult<Any>

    @PUT("/api/orders/{id}/cancel")
    suspend fun cancelOrder(@Path("id") id: Long, @Body request: CancelRequest): ApiResult<Any>

    @PUT("/api/orders/{id}/receive")
    suspend fun receiveOrder(@Path("id") id: Long): ApiResult<Any>

    @POST("/api/orders/{id}/evaluate")
    suspend fun evaluateOrder(@Path("id") id: Long, @Body request: EvaluateRequest): ApiResult<Any>

    @GET("/api/messages/conversations")
    suspend fun getConversations(): ApiResult<List<ConversationInfo>>

    @GET("/api/messages/conversation/{userId}")
    suspend fun getMessages(
        @Path("userId") userId: Long,
        @Query("conversationId") conversationId: Long? = null,
        @Query("goodsId") goodsId: Long? = null,
        @Query("page") page: Int = 1,
        @Query("size") size: Int = 20
    ): ApiResult<PageResult<MessageInfo>>

    @POST("/api/messages")
    suspend fun sendMessage(@Body request: SendMessageRequest): ApiResult<MessageInfo>

    @GET("/api/messages/notifications")
    suspend fun getNotifications(@Query("page") page: Int = 1, @Query("size") size: Int = 20): ApiResult<PageResult<NotificationInfo>>

    @PUT("/api/messages/read/{conversationId}")
    suspend fun markRead(@Path("conversationId") conversationId: Long): ApiResult<Any>

    @DELETE("/api/messages/conversation/{userId}")
    suspend fun deleteConversation(
        @Path("userId") userId: Long,
        @Query("conversationId") conversationId: Long? = null
    ): ApiResult<Any>

    @Multipart
    @POST("/api/upload/image")
    suspend fun uploadImage(@Part file: MultipartBody.Part, @Part("type") type: String? = null): ApiResult<UploadResponse>

    @POST("/api/feedback")
    suspend fun submitFeedback(@Body request: Map<String, Any>): ApiResult<Any>
}
