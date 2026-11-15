package com.example.data.model

import java.util.Calendar

object DefaultCatalog {

    data class CategorySeed(
        val id: String,
        val nameBn: String,
        val nameEn: String,
        val type: String,
        val iconKey: String,
        val colorHex: Long
    )

    val incomeCategories = listOf(
        CategorySeed("inc_salary", "বেতন", "Salary", "income", "payments", 0xFF10B981L),
        CategorySeed("inc_business", "ব্যবসা", "Business", "income", "storefront", 0xFF059669L),
        CategorySeed("inc_freelance", "ফ্রিল্যান্স", "Freelance", "income", "laptop", 0xFF14B8A6L),
        CategorySeed("inc_rental", "ভাড়া আয়", "Rental Income", "income", "home", 0xFF0EA5E9L),
        CategorySeed("inc_investment", "বিনিয়োগ মুনাফা", "Investment", "income", "bank", 0xFF6366F1L),
        CategorySeed("inc_gift", "উপহার", "Gift", "income", "redeem", 0xFF8B5CF6L),
        CategorySeed("inc_other", "অন্যান্য (Others)", "Others", "income", "account_balance_wallet", 0xFF3B82F6L)
    )

    val expenseCategories = listOf(
        CategorySeed("exp_food", "খাবার", "Food", "expense", "restaurant", 0xFFFF6B2CL),
        CategorySeed("exp_groceries", "কাঁচাবাজার", "Groceries", "expense", "shopping_cart", 0xFF10B981L),
        CategorySeed("exp_breakfast", "নাস্তা ও চা", "Breakfast & Tea", "expense", "cafe", 0xFFF97316L),
        CategorySeed("exp_rent", "বাড়ি ভাড়া", "House Rent", "expense", "home", 0xFFEF4444L),
        CategorySeed("exp_transport", "যাতায়াত", "Transport", "expense", "directions_bus", 0xFF3B82F6L),
        CategorySeed("exp_fuel", "তেল ও জ্বালানি", "Fuel & Gas", "expense", "local_gas_station", 0xFF0EA5E9L),
        CategorySeed("exp_bills", "বিদ্যুৎ ও গ্যাস বিল", "Electricity & Gas Bills", "expense", "bolt", 0xFFF59E0BL),
        CategorySeed("exp_shopping", "শপিং", "Shopping", "expense", "shopping_bag", 0xFFEC4899L),
        CategorySeed("exp_clothes", "পোশাক ও জুতা", "Clothing & Shoes", "expense", "checkroom", 0xFFD946EFL),
        CategorySeed("exp_health", "চিকিৎসা", "Medical", "expense", "medical_services", 0xFF06B6D4L),
        CategorySeed("exp_mobile", "মোবাইল ও ইন্টারনেট", "Mobile & Internet", "expense", "wifi", 0xFF6366F1L),
        CategorySeed("exp_family", "পরিবার", "Family", "expense", "family_restroom", 0xFF8B5CF6L),
        CategorySeed("exp_education", "শিক্ষা", "Education", "expense", "school", 0xFF0EA5E9L),
        CategorySeed("exp_entertainment", "বিনোদন", "Entertainment", "expense", "movie", 0xFFD946EFL),
        CategorySeed("exp_subscriptions", "সাবস্ক্রিপশন", "Subscriptions", "expense", "subscriptions", 0xFF6366F1L),
        CategorySeed("exp_travel", "ভ্রমণ ও ট্যুর", "Travel & Tour", "expense", "flight", 0xFF14B8A6L),
        CategorySeed("exp_fitness", "জিম ও ফিটনেস", "Fitness & Gym", "expense", "fitness_center", 0xFF10B981L),
        CategorySeed("exp_repair", "মেরামত ও সার্ভিস", "Repair & Service", "expense", "build", 0xFFF59E0BL),
        CategorySeed("exp_charity", "উপহার ও দান", "Gifts & Charity", "expense", "redeem", 0xFFEC4899L),
        CategorySeed("exp_pets", "পোষা প্রাণী", "Pets", "expense", "pets", 0xFF8B5CF6L),
        CategorySeed("exp_personal", "ব্যক্তিগত খরচ", "Personal Expenses", "expense", "person", 0xFFF97316L),
        CategorySeed("exp_other", "অন্যান্য (Others)", "Others", "expense", "category", 0xFF64748BL)
    )

    val allDefaultCategories = incomeCategories + expenseCategories

    fun currentMonthKey(timeMillis: Long = System.currentTimeMillis()): String {
        val cal = Calendar.getInstance().apply { this.timeInMillis = timeMillis }
        val year = cal.get(Calendar.YEAR)
        val month = cal.get(Calendar.MONTH) + 1
        return String.format("%04d-%02d", year, month)
    }

    fun previousMonthKey(timeMillis: Long = System.currentTimeMillis()): String {
        val cal = Calendar.getInstance().apply {
            this.timeInMillis = timeMillis
            add(Calendar.MONTH, -1)
        }
        val year = cal.get(Calendar.YEAR)
        val month = cal.get(Calendar.MONTH) + 1
        return String.format("%04d-%02d", year, month)
    }

    fun monthKeyOf(timeMillis: Long): String = currentMonthKey(timeMillis)
}
