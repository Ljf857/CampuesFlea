package com.example.campusflea.ui.util

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * 分类图标名到 Material 图标的映射。
 *
 * ### 这张表是移动端的图标白名单
 *
 * 后台「分类管理」的图标选择器只提供这里存在的名字（见管理端 `constants/categoryIcons.ts`）。
 * 表里没有的名字会走 [resolve] 的兜底分支，静默显示成「通用」图标——
 * 历史上后台可选 27 个、这里只有 25 个，管理员选了 `Skirt` / `Shoes` 后
 * App 显示的是售卖图标，界面上没有任何提示。
 *
 * **新增图标时两边必须同步改。** 管理端新增一个场景图标 → 这里补一行 → 才能上线。
 *
 * 注意：即使某个名字已不再出现在管理端的可选目录里，也不要从这张表删除——
 * 数据库里仍有分类在用旧值，删掉会让它们突然退化成通用图标。
 */
object CategoryIcons {

    private val iconMap = mapOf(
        // ---------- 学习教材 ----------
        "MenuBook" to Icons.Default.MenuBook,
        "School" to Icons.Default.School,
        "Book" to Icons.Default.Book,
        "Calculate" to Icons.Default.Calculate,
        "Translate" to Icons.Default.Translate,
        "Functions" to Icons.Default.Functions, // 已不在管理端目录中，保留以兼容存量数据

        // ---------- 数码电子 ----------
        "Devices" to Icons.Default.Devices,
        "Laptop" to Icons.Default.Laptop,
        "Tablet" to Icons.Default.Tablet,
        "PhoneAndroid" to Icons.Default.PhoneAndroid,
        "Headphones" to Icons.Default.Headphones,
        "Speaker" to Icons.Default.Speaker,
        "Mouse" to Icons.Default.Mouse,
        "CameraAlt" to Icons.Default.CameraAlt,
        "Watch" to Icons.Default.Watch,
        "Tv" to Icons.Default.Tv,
        "Print" to Icons.Default.Print,
        "SportsEsports" to Icons.Default.SportsEsports,

        // ---------- 服饰美妆 ----------
        "Checkroom" to Icons.Default.Checkroom,
        "Face" to Icons.Default.Face,
        "Diamond" to Icons.Default.Diamond,
        // 该版本的 material-icons-extended 没有 Footprints / Refrigerator，
        // 用语义最接近的 DirectionsWalk / AcUnit 代替，存库值随之为这两个名字
        "DirectionsWalk" to Icons.Default.DirectionsWalk,
        "Backpack" to Icons.Default.Backpack,
        "ShoppingBag" to Icons.Default.ShoppingBag,

        // ---------- 运动户外 ----------
        "FitnessCenter" to Icons.Default.FitnessCenter,
        "SportsBasketball" to Icons.Default.SportsBasketball,
        "PedalBike" to Icons.Default.PedalBike,
        "Hiking" to Icons.Default.Hiking,
        "Skateboarding" to Icons.Default.Skateboarding,

        // ---------- 出行代步 ----------
        "ElectricRickshaw" to Icons.Default.ElectricRickshaw,
        "DirectionsBus" to Icons.Default.DirectionsBus,
        "Train" to Icons.Default.Train,
        "Flight" to Icons.Default.Flight,

        // ---------- 生活用品 ----------
        "Coffee" to Icons.Default.Coffee,
        "Kitchen" to Icons.Default.Kitchen,
        "LunchDining" to Icons.Default.LunchDining,
        "Microwave" to Icons.Default.Microwave,
        "AcUnit" to Icons.Default.AcUnit,
        "Bed" to Icons.Default.Bed,
        "Chair" to Icons.Default.Chair,
        "Weekend" to Icons.Default.Weekend,
        "Lightbulb" to Icons.Default.Lightbulb,
        "Light" to Icons.Default.Light, // 已不在管理端目录中，保留以兼容存量数据
        "Shower" to Icons.Default.Shower,

        // ---------- 兴趣娱乐 ----------
        "MusicNote" to Icons.Default.MusicNote,
        "Palette" to Icons.Default.Palette,
        "Science" to Icons.Default.Science,
        "Pets" to Icons.Default.Pets,
        "Extension" to Icons.Default.Extension,
        "ChildCare" to Icons.Default.ChildCare,
        "CardGiftcard" to Icons.Default.CardGiftcard,

        // ---------- 维修工具 ----------
        "Handyman" to Icons.Default.Handyman,
        "Construction" to Icons.Default.Construction,
        "AutoAwesome" to Icons.Default.AutoAwesome,

        // ---------- 其他 ----------
        "Inventory2" to Icons.Default.Inventory2,
        "Inventory" to Icons.Default.Inventory,
        "Storefront" to Icons.Default.Storefront,
        "Sell" to Icons.Default.Sell,
    )

    /** 未知图标名统一回退到这个，保证界面永远有图形可画 */
    val Fallback: ImageVector = Icons.Default.Sell

    fun resolve(iconName: String): ImageVector = iconMap[iconName] ?: Fallback
}
