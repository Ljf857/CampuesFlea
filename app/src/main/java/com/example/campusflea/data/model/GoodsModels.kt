package com.example.campusflea.data.model

import androidx.compose.ui.graphics.Color
import com.google.gson.annotations.SerializedName

data class GoodsInfo(
    @SerializedName("id") val id: Long = 0,
    @SerializedName("title") val title: String = "",
    @SerializedName("price") val price: Double = 0.0,
    @SerializedName("originalPrice") val originalPrice: Double = 0.0,
    @SerializedName("coverImage") val coverImage: String = "",
    @SerializedName("images") val images: List<String> = emptyList(),
    @SerializedName("conditionLevel") val condition: Int = 0,
    @SerializedName("conditionDesc") val conditionDesc: String = "",
    @SerializedName("categoryId") val categoryId: Long = 0,
    @SerializedName("categoryName") val categoryName: String = "",
    @SerializedName("sellerId") val sellerId: Long = 0,
    @SerializedName("sellerName") val sellerName: String = "",
    @SerializedName("sellerAvatar") val sellerAvatar: String = "",
    @SerializedName("campus") val campus: String = "",
    @SerializedName("viewCount") val viewCount: Int = 0,
    @SerializedName("favoriteCount") val favoriteCount: Int = 0,
    @SerializedName("publishTime") val publishTime: String = ""
)

data class GoodsDetail(
    @SerializedName("id") val id: Long = 0,
    @SerializedName("title") val title: String = "",
    @SerializedName("price") val price: Double = 0.0,
    @SerializedName("originalPrice") val originalPrice: Double = 0.0,
    @SerializedName("description") val description: String = "",
    @SerializedName("images") val images: List<String> = emptyList(),
    @SerializedName("conditionLevel") val condition: Int = 0,
    @SerializedName("conditionDesc") val conditionDesc: String = "",
    @SerializedName("categoryId") val categoryId: Long = 0,
    @SerializedName("categoryName") val categoryName: String = "",
    @SerializedName("location") val location: String = "",
    @SerializedName("deliveryType") val deliveryType: Int = 0,
    @SerializedName("deliveryTypeDesc") val deliveryTypeDesc: String = "",
    @SerializedName("viewCount") val viewCount: Int = 0,
    @SerializedName("favoriteCount") val favoriteCount: Int = 0,
    @SerializedName("status") val status: Int = 0,
    @SerializedName("statusDesc") val statusDesc: String = "",
    @SerializedName("publishTime") val publishTime: String = "",
    @SerializedName("seller") val seller: SellerInfo? = null,
    @SerializedName("isFavorited") val isFavorited: Boolean = false,
    @SerializedName("relatedGoods") val relatedGoods: List<GoodsInfo> = emptyList()
)

data class SellerInfo(
    @SerializedName("id") val id: Long = 0,
    @SerializedName("nickname") val nickname: String = "",
    @SerializedName("avatar") val avatar: String = "",
    @SerializedName("verifyStatus") val verifyStatus: Int = 0,
    @SerializedName("campus") val campus: String = "",
    @SerializedName("soldCount") val soldCount: Int = 0,
    @SerializedName("avgRating") val avgRating: Double = 0.0
)

data class CategoryInfo(
    @SerializedName("id") val id: Long = 0,
    @SerializedName("parentId") val parentId: Long = 0,
    @SerializedName("name") val name: String = "",
    @SerializedName("icon") val icon: String = "",
    @SerializedName("color") val color: String = "",
    @SerializedName("colorDark") val colorDark: String = "",
    @SerializedName("sort") val sort: Int = 0,
    @SerializedName("status") val status: Int = 1,
    @SerializedName("children") val children: List<CategoryInfo> = emptyList()
) {
    val themeColor: Color get() {
        val hex = color.removePrefix("#")
        return try {
            Color(hex.toLong(16) or 0xFF000000.toLong())
        } catch (_: Exception) {
            Color(0xFF74777F)
        }
    }
}

data class PublishRequest(
    @SerializedName("title") val title: String,
    @SerializedName("price") val price: Double,
    @SerializedName("originalPrice") val originalPrice: Double? = null,
    @SerializedName("description") val description: String,
    @SerializedName("images") val images: List<String>,
    @SerializedName("conditionLevel") val condition: Int,
    @SerializedName("categoryId") val categoryId: Long,
    @SerializedName("location") val location: String,
    @SerializedName("deliveryType") val deliveryType: Int
)

data class ReportRequest(
    @SerializedName("reason") val reason: String,
    @SerializedName("description") val description: String,
    @SerializedName("evidenceImages") val evidenceImages: List<String> = emptyList()
)

data class HotKeywordsResponse(
    @SerializedName("keywords") val keywords: List<String>
)

data class BannerInfo(
    @SerializedName("id") val id: Long = 0,
    @SerializedName("title") val title: String = "",
    @SerializedName("image") val image: String = "",
    @SerializedName("linkType") val linkType: Int = 0,
    @SerializedName("linkUrl") val linkUrl: String = "",
    @SerializedName("sort") val sort: Int = 0,
    @SerializedName("status") val status: Int = 0
)
