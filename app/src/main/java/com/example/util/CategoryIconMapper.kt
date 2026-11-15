package com.example.util

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.AttachMoney
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.CardGiftcard
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.Celebration
import androidx.compose.material.icons.rounded.Checkroom
import androidx.compose.material.icons.rounded.ChildCare
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.DirectionsBus
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.FamilyRestroom
import androidx.compose.material.icons.rounded.Fastfood
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Flight
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LaptopMac
import androidx.compose.material.icons.rounded.LocalCafe
import androidx.compose.material.icons.rounded.LocalGasStation
import androidx.compose.material.icons.rounded.LocalHospital
import androidx.compose.material.icons.rounded.LocalTaxi
import androidx.compose.material.icons.rounded.MedicalServices
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Pets
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material.icons.rounded.ShoppingCart
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material.icons.rounded.Subscriptions
import androidx.compose.material.icons.rounded.TwoWheeler
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material.icons.rounded.Work
import androidx.compose.ui.graphics.vector.ImageVector

object CategoryIconMapper {
    val availableIcons: List<Pair<String, ImageVector>> = listOf(
        "restaurant" to Icons.Rounded.Restaurant,
        "fastfood" to Icons.Rounded.Fastfood,
        "cafe" to Icons.Rounded.LocalCafe,
        "shopping_cart" to Icons.Rounded.ShoppingCart,
        "shopping_bag" to Icons.Rounded.ShoppingBag,
        "checkroom" to Icons.Rounded.Checkroom,
        "home" to Icons.Rounded.Home,
        "directions_bus" to Icons.Rounded.DirectionsBus,
        "directions_car" to Icons.Rounded.DirectionsCar,
        "two_wheeler" to Icons.Rounded.TwoWheeler,
        "local_taxi" to Icons.Rounded.LocalTaxi,
        "local_gas_station" to Icons.Rounded.LocalGasStation,
        "bolt" to Icons.Rounded.Bolt,
        "water_drop" to Icons.Rounded.WaterDrop,
        "wifi" to Icons.Rounded.Wifi,
        "mobile" to Icons.Rounded.PhoneAndroid,
        "medical_services" to Icons.Rounded.MedicalServices,
        "local_hospital" to Icons.Rounded.LocalHospital,
        "school" to Icons.Rounded.School,
        "menu_book" to Icons.Rounded.MenuBook,
        "family_restroom" to Icons.Rounded.FamilyRestroom,
        "child_care" to Icons.Rounded.ChildCare,
        "movie" to Icons.Rounded.Movie,
        "subscriptions" to Icons.Rounded.Subscriptions,
        "flight" to Icons.Rounded.Flight,
        "fitness_center" to Icons.Rounded.FitnessCenter,
        "build" to Icons.Rounded.Build,
        "celebration" to Icons.Rounded.Celebration,
        "redeem" to Icons.Rounded.CardGiftcard,
        "pets" to Icons.Rounded.Pets,
        "person" to Icons.Rounded.Person,
        "credit_card" to Icons.Rounded.CreditCard,
        "payments" to Icons.Rounded.Payments,
        "storefront" to Icons.Rounded.Storefront,
        "laptop" to Icons.Rounded.LaptopMac,
        "work" to Icons.Rounded.Work,
        "account_balance_wallet" to Icons.Rounded.AccountBalanceWallet,
        "savings" to Icons.Rounded.Savings,
        "bank" to Icons.Rounded.AccountBalance,
        "favorite" to Icons.Rounded.Favorite,
        "star" to Icons.Rounded.Star,
        "money" to Icons.Rounded.AttachMoney,
        "category" to Icons.Rounded.Category
    )

    val availableColors: List<Long> = listOf(
        0xFFFF6B2CL,
        0xFF10B981L,
        0xFF3B82F6L,
        0xFFEF4444L,
        0xFFF59E0BL,
        0xFF8B5CF6L,
        0xFFEC4899L,
        0xFF06B6D4L,
        0xFF6366F1L,
        0xFF14B8A6L,
        0xFF0EA5E9L,
        0xFF64748BL
    )

    fun iconFor(key: String): ImageVector =
        availableIcons.firstOrNull { it.first == key }?.second ?: Icons.Rounded.Category

    fun suggestIcons(query: String, type: String = "expense"): List<Pair<String, ImageVector>> {
        val q = query.trim().lowercase()
        val matchedKeys = mutableListOf<String>()
        if (q.isNotEmpty()) {
            val rules = listOf(
                listOf("খাবার", "ভাত", "বিরিয়ানি", "হোটেল", "food", "meal", "dinner", "lunch") to listOf("restaurant", "fastfood", "cafe"),
                listOf("বাজার", "কাঁচাবাজার", "সবজি", "মুদি", "grocer", "bazar", "market") to listOf("shopping_cart", "shopping_bag", "restaurant"),
                listOf("চা", "কফি", "নাস্তা", "tea", "coffee", "breakfast", "snack") to listOf("cafe", "fastfood", "restaurant"),
                listOf("ভাড়া", "বাড়ি", "বাসা", "রুম", "rent", "house", "home", "flat") to listOf("home", "bolt", "water_drop"),
                listOf("যাতায়াত", "বাস", "রিকশা", "অটো", "উবার", "ট্যাক্সি", "বাইক", "গাড়ি", "transport", "bus", "car", "taxi", "ride", "bike") to listOf("directions_bus", "local_taxi", "directions_car", "two_wheeler"),
                listOf("তেল", "পেট্রোল", "ডিজেল", "অকটেন", "জ্বালানি", "fuel", "gas", "petrol", "diesel") to listOf("local_gas_station", "directions_car", "two_wheeler"),
                listOf("বিদ্যুৎ", "কারেন্ট", "বিল", "পানি", "গ্যাস", "bill", "electric", "water", "utility") to listOf("bolt", "water_drop", "credit_card"),
                listOf("মোবাইল", "রিচার্জ", "ইন্টারনেট", "ওয়াইফাই", "নেট", "mobile", "internet", "wifi", "data", "recharge") to listOf("wifi", "mobile", "subscriptions"),
                listOf("শপিং", "কেনাকাটা", "জামা", "কাপড়", "জুতা", "পোশাক", "shop", "cloth", "dress", "shoe", "fashion") to listOf("checkroom", "shopping_bag", "shopping_cart"),
                listOf("ওষুধ", "ডাক্তার", "চিকিৎসা", "হাসপাতাল", "মেডিকেল", "health", "medic", "doctor", "hospital", "pharmacy") to listOf("medical_services", "local_hospital", "favorite"),
                listOf("শিক্ষা", "বই", "স্কুল", "কলেজ", "টিউশন", "কোর্স", "edu", "school", "book", "study", "tuition", "course") to listOf("school", "menu_book", "laptop"),
                listOf("পরিবার", "বাচ্চা", "মা", "বাবা", "family", "baby", "kid", "child") to listOf("family_restroom", "child_care", "home"),
                listOf("বিনোদন", "সিনেমা", "মুভি", "গেম", "সাবস্ক্রিপশন", "entertain", "movie", "game", "sub", "netflix", "spotify") to listOf("movie", "subscriptions", "celebration"),
                listOf("ভ্রমণ", "ট্যুর", "ফ্লাইট", "হোটেল", "টিকেট", "travel", "tour", "flight", "trip", "ticket") to listOf("flight", "directions_bus", "celebration"),
                listOf("জিম", "ব্যায়াম", "ফিটনেস", "খেলা", "gym", "fit", "sport", "workout") to listOf("fitness_center", "favorite", "person"),
                listOf("মেরামত", "সার্ভিস", "মিস্ত্রি", "repair", "fix", "service", "maint") to listOf("build", "home", "directions_car"),
                listOf("উপহার", "দান", "চাঁদা", "যাকাত", "সদকা", "gift", "donat", "charity", "celebrat") to listOf("redeem", "celebration", "favorite"),
                listOf("পোষা", "বিড়াল", "কুকুর", "pet", "cat", "dog") to listOf("pets", "favorite"),
                listOf("বেতন", "বোনাস", "চাকরি", "salary", "bonus", "job", "work") to listOf("payments", "work", "bank"),
                listOf("ব্যবসা", "দোকান", "বিক্রি", "business", "shop", "store", "sale") to listOf("storefront", "payments", "money"),
                listOf("ফ্রিল্যান্স", "কম্পিউটার", "ল্যাপটপ", "freelance", "laptop", "online", "tech") to listOf("laptop", "work", "account_balance_wallet"),
                listOf("ব্যাংক", "কার্ড", "লোণ", "লোন", "কিস্তি", "bank", "card", "loan", "emi") to listOf("credit_card", "bank", "payments")
            )
            rules.forEach { (keywords, keys) ->
                if (keywords.any { kw -> q.contains(kw) }) {
                    keys.forEach { k -> if (k !in matchedKeys) matchedKeys.add(k) }
                }
            }
        }
        val fallbackKeys = if (type == "income") {
            listOf("payments", "storefront", "laptop", "redeem", "bank", "savings", "work", "account_balance_wallet")
        } else {
            listOf("restaurant", "shopping_cart", "cafe", "home", "directions_bus", "bolt", "medical_services", "checkroom")
        }
        fallbackKeys.forEach { k -> if (k !in matchedKeys) matchedKeys.add(k) }
        return matchedKeys.take(8).mapNotNull { key ->
            availableIcons.firstOrNull { it.first == key }
        }
    }
}
