package com.example.campusflea.data.model

import com.google.gson.annotations.SerializedName

data class CreateOrderRequest(
    @SerializedName("goodsId") val goodsId: Long,
    @SerializedName("addressId") val addressId: Long? = null,
    @SerializedName("deliveryType") val deliveryType: Int,
    @SerializedName("remark") val remark: String? = null
)

data class OrderInfo(
    @SerializedName("id") val id: Long = 0,
    @SerializedName("orderNo") val orderNo: String = "",
    @SerializedName("goodsId") val goodsId: Long = 0,
    @SerializedName("goodsTitle") val goodsTitle: String = "",
    @SerializedName("goodsPrice") val goodsPrice: Double = 0.0,
    @SerializedName("goodsCover") val goodsCover: String = "",
    @SerializedName("deliveryType") val deliveryType: Int = 0,
    @SerializedName("deliveryTypeDesc") val deliveryTypeDesc: String = "",
    @SerializedName("status") val status: Int = 0,
    @SerializedName("statusDesc") val statusDesc: String = "",
    @SerializedName("createTime") val createTime: String = "",
    @SerializedName("buyerId") val buyerId: Long = 0,
    @SerializedName("sellerId") val sellerId: Long = 0,
    @SerializedName("buyer") val buyer: OrderUserBrief? = null,
    @SerializedName("seller") val seller: OrderUserBrief? = null
)

data class OrderDetail(
    @SerializedName("id") val id: Long = 0,
    @SerializedName("orderNo") val orderNo: String = "",
    @SerializedName("goods") val goods: OrderGoodsInfo? = null,
    @SerializedName("buyer") val buyer: OrderUserInfo? = null,
    @SerializedName("seller") val seller: OrderUserInfo? = null,
    @SerializedName("deliveryType") val deliveryType: Int = 0,
    @SerializedName("deliveryTypeDesc") val deliveryTypeDesc: String = "",
    @SerializedName("address") val address: AddressSnapshot? = null,
    @SerializedName("remark") val remark: String = "",
    @SerializedName("status") val status: Int = 0,
    @SerializedName("statusDesc") val statusDesc: String = "",
    @SerializedName("createTime") val createTime: String = "",
    @SerializedName("updateTime") val updateTime: String = "",
    @SerializedName("timeline") val timeline: List<TimelineItem> = emptyList()
)

data class OrderGoodsInfo(
    @SerializedName("id") val id: Long = 0,
    @SerializedName("title") val title: String = "",
    @SerializedName("price") val price: Double = 0.0,
    @SerializedName("coverImage") val coverImage: String = ""
)

data class OrderUserInfo(
    @SerializedName("id") val id: Long = 0,
    @SerializedName("nickname") val nickname: String = "",
    @SerializedName("avatar") val avatar: String = "",
    @SerializedName("phone") val phone: String = ""
)

data class OrderUserBrief(
    @SerializedName("id") val id: Long = 0,
    @SerializedName("nickname") val nickname: String = "",
    @SerializedName("avatar") val avatar: String = ""
)

data class AddressSnapshot(
    @SerializedName("receiver") val receiver: String = "",
    @SerializedName("phone") val phone: String = "",
    @SerializedName("fullAddress") val fullAddress: String = ""
)

data class TimelineItem(
    @SerializedName("time") val time: String = "",
    @SerializedName("desc") val desc: String = ""
)

data class EvaluateRequest(
    @SerializedName("rating") val rating: Int,
    @SerializedName("content") val content: String,
    @SerializedName("images") val images: List<String> = emptyList()
)

data class CancelRequest(
    @SerializedName("reason") val reason: String
)
