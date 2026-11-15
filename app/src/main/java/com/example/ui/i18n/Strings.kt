package com.example.ui.i18n

import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class CurrencyInfo(
    val code: String,
    val symbol: String,
    val labelBn: String,
    val labelEn: String
)

object CurrencyCatalog {
    val all = listOf(
        CurrencyInfo("BDT", "৳", "বাংলাদেশি টাকা (৳ BDT)", "Bangladeshi Taka (৳ BDT)"),
        CurrencyInfo("USD", "$", "ইউএস ডলার ($ USD)", "US Dollar ($ USD)"),
        CurrencyInfo("EUR", "€", "ইউরো (€ EUR)", "Euro (€ EUR)"),
        CurrencyInfo("INR", "₹", "ভারতীয় রুপি (₹ INR)", "Indian Rupee (₹ INR)"),
        CurrencyInfo("GBP", "£", "ব্রিটিশ পাউন্ড (£ GBP)", "British Pound (£ GBP)")
    )

    fun symbolFor(code: String): String = all.firstOrNull { it.code == code }?.symbol ?: "৳"
}

fun formatMoney(
    amount: Double,
    currencyCode: String = "BDT",
    hideBalances: Boolean = false
): String {
    if (hideBalances) return "${CurrencyCatalog.symbolFor(currencyCode)} •••••"
    val symbol = CurrencyCatalog.symbolFor(currencyCode)
    val nf = NumberFormat.getNumberInstance(Locale.US).apply {
        maximumFractionDigits = if (amount % 1.0 == 0.0) 0 else 2
        minimumFractionDigits = 0
    }
    val absFormatted = nf.format(kotlin.math.abs(amount))
    return if (amount < 0) "-$symbol $absFormatted" else "$symbol $absFormatted"
}

fun formatDateTime(millis: Long, language: String): String {
    val locale = if (language == "bn") Locale("bn", "BD") else Locale.ENGLISH
    val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", locale)
    return sdf.format(Date(millis))
}

fun formatShortDate(millis: Long, language: String): String {
    val locale = if (language == "bn") Locale("bn", "BD") else Locale.ENGLISH
    val sdf = SimpleDateFormat("dd MMM yyyy", locale)
    return sdf.format(Date(millis))
}

fun formatMonthYear(year: Int, monthZeroBased: Int, language: String): String {
    val bnMonths = listOf(
        "জানুয়ারি", "ফেব্রুয়ারি", "মার্চ", "এপ্রিল", "মে", "জুন",
        "জুলাই", "আগস্ট", "সেপ্টেম্বর", "অক্টোবর", "নভেম্বর", "ডিসেম্বর"
    )
    val enMonths = listOf(
        "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December"
    )
    val name = if (language == "bn") bnMonths.getOrElse(monthZeroBased) { "" }
    else enMonths.getOrElse(monthZeroBased) { "" }
    return "$name $year"
}

data class AppStrings(
    val isBn: Boolean
) {
    val appTitle = "MoneyTracker"
    val appSubtitle = if (isBn) "ব্যক্তিগত আয়-ব্যয় ও বাজেট ম্যানেজার" else "Personal Money Manager & Budget Planner"

    // Bottom Navigation
    val navHome = if (isBn) "হোম" else "Home"
    val navTransactions = if (isBn) "লেনদেন" else "Transactions"
    val navBudget = if (isBn) "বাজেট" else "Budget"
    val navStats = if (isBn) "পরিসংখ্যান" else "Statistics"
    val navSettings = if (isBn) "সেটিংস" else "Settings"

    // Secondary Modules
    val savings = if (isBn) "সঞ্চয়" else "Savings"
    val lendBorrow = if (isBn) "ধার-দেনা" else "Lend & Borrow"
    val categories = if (isBn) "ক্যাটাগরি" else "Categories"
    val calendar = if (isBn) "ক্যালেন্ডার" else "Calendar"
    val smartInsights = if (isBn) "স্মার্ট বিশ্লেষণ" else "Smart Insights"

    // Auth
    val signInTitle = if (isBn) "আপনার টাকার প্রতিটি হিসাব রাখুন সহজে" else "Track Every Taka With Clarity"
    val signInDesc = if (isBn) {
        "নিরাপদ ক্লাউড ব্যাকআপ, দৈনিক আয়-ব্যয়, স্মার্ট বাজেট, সঞ্চয় ও ধার-দেনার হিসাব এক জায়গায়।"
    } else {
        "Secure cloud sync for daily income & expenses, smart monthly budgets, savings goals, and lend/borrow tracking."
    }
    val signInWithGoogle = if (isBn) "Gmail দিয়ে অ্যাকাউন্ট / সাইন ইন" else "Sign in / Create Account with Gmail"
    val signOut = if (isBn) "সাইন আউট" else "Sign Out"
    val localAccountTitle = if (isBn) "ফোনের লোকাল স্টোরেজ (অফলাইন)" else "Phone Local Storage (Offline)"
    val localAccountDesc = if (isBn) {
        "অফলাইনে ব্যবহারের জন্য সব হিসাব আপনার ফোনের লোকাল স্টোরেজে সেভ হচ্ছে।"
    } else {
        "All records are saved offline in your phone's local storage."
    }
    val cloudSyncActive = if (isBn) "Gmail ক্লাউডে সেভ হচ্ছে" else "Saving to Gmail Account"
    val storageModeTitle = if (isBn) "ডাটা সেভিং মাধ্যম (ক্লাউড / অফলাইন)" else "Data Storage Mode (Cloud / Offline)"
    val storageGmailMode = if (isBn) "Gmail অ্যাকাউন্ট" else "Gmail Account"
    val storageOfflineMode = if (isBn) "ফোন স্টোরেজ (অফলাইন)" else "Phone Storage (Offline)"
    val saveToPhoneStorageBtn = if (isBn) "ফোনের লোকাল স্টোরেজে সেভ করুন" else "Save Copy to Phone Storage"
    val uploadLocalToGmailBtn = if (isBn) "লোকাল ডাটা Gmail-এ সেভ করুন" else "Save Local Data to Gmail"
    val savedToPhoneSuccess = if (isBn) "সব ডাটা ফোনের লোকাল স্টোরেজে সেভ করা হয়েছে!" else "All data saved to phone local storage!"
    val syncedToGmailSuccess = if (isBn) "লোকাল ডাটা Gmail অ্যাকাউন্টে সেভ করা হয়েছে!" else "Local data saved to Gmail account!"
    val switchedToOfflineMsg = if (isBn) "অফলাইন ফোন স্টোরেজ চালু হয়েছে" else "Switched to Offline Phone Storage"
    val switchedToGmailMsg = if (isBn) "Gmail ক্লাউড অ্যাকাউন্ট চালু হয়েছে" else "Switched to Gmail Cloud Account"

    // Home Dashboard
    val currentBalance = if (isBn) "বর্তমান ব্যালেন্স" else "Current Balance"
    val todayIncome = if (isBn) "আজকের আয়" else "Today's Income"
    val todayExpense = if (isBn) "আজকের খরচ" else "Today's Expense"
    val monthlyIncome = if (isBn) "মাসিক আয়" else "Monthly Income"
    val monthlyExpense = if (isBn) "মাসিক খরচ" else "Monthly Expense"
    val monthlySavings = if (isBn) "মাসিক সঞ্চয়" else "Monthly Savings"
    val remainingBudget = if (isBn) "বাকি বাজেট" else "Remaining Budget"
    val recentTransactions = if (isBn) "সাম্প্রতিক লেনদেন" else "Recent Transactions"
    val dailySpendingSummary = if (isBn) "দৈনিক খরচের সারসংক্ষেপ" else "Daily Spending Summary"
    val incomeVsExpense = if (isBn) "আয় বনাম খরচ" else "Income vs Expense"
    val quickTools = if (isBn) "দ্রুত অ্যাক্সেস" else "Quick Access"
    val seeAll = if (isBn) "সব দেখুন" else "See All"
    val overviewSuffix = if (isBn) "সারসংক্ষেপ" else "overview"
    val balanceLabel = if (isBn) "ব্যালেন্স" else "Balance"
    val loansLabel = if (isBn) "ধার-দেনা" else "Loans"
    val todayWord = if (isBn) "আজ" else "Today"
    val spentInPrefix = if (isBn) "মোট খরচ" else "Spent in"
    val incomeInPrefix = if (isBn) "মোট আয়" else "Income in"
    val demoBannerTitle = if (isBn) "ডেমো ডাটা চালু আছে" else "Demo Data Active"
    val demoBannerSubtitle = if (isBn) "প্রাথমিক প্রিভিউর জন্য নমুনা হিসাব যোগ করা হয়েছে।" else "Sample records are loaded for preview."
    val clearDemo = if (isBn) "ডেমো মুছুন" else "Clear Demo"
    val demoBadge = if (isBn) "ডেমো" else "Demo"

    // Transactions
    val addTransaction = if (isBn) "লেনদেন যোগ করুন" else "Add Transaction"
    val editTransaction = if (isBn) "লেনদেন সম্পাদনা" else "Edit Transaction"
    val income = if (isBn) "আয়" else "Income"
    val expense = if (isBn) "খরচ" else "Expense"
    val all = if (isBn) "সব" else "All"
    val amount = if (isBn) "পরিমাণ" else "Amount"
    val category = if (isBn) "ক্যাটাগরি" else "Category"
    val dateAndTime = if (isBn) "তারিখ ও সময়" else "Date & Time"
    val note = if (isBn) "বিবরণ / নোট" else "Description / Note"
    val paymentMethod = if (isBn) "পেমেন্ট মাধ্যম" else "Payment Method"
    val cash = if (isBn) "নগদ (Cash)" else "Cash"
    val bank = if (isBn) "ব্যাংক (Bank)" else "Bank"
    val mobileBanking = if (isBn) "মোবাইল ব্যাংকিং" else "Mobile Banking"
    val otherMethod = if (isBn) "অন্যান্য (Others)" else "Others"
    val viewExpenses = if (isBn) "খরচ দেখুন" else "View Expenses"
    val totalFilteredExpense = if (isBn) "মোট খরচ" else "Total Expenses"
    val totalFilteredIncome = if (isBn) "মোট আয়" else "Total Income"
    val recordsCountSuffix = if (isBn) "টি হিসাব" else "records"
    val attachPhoto = if (isBn) "রসিদ / ছবি যুক্ত করুন (ঐচ্ছিক)" else "Attach Photo Receipt (Optional)"
    val changePhoto = if (isBn) "ছবি পরিবর্তন" else "Change Photo"
    val removePhoto = if (isBn) "ছবি সরান" else "Remove"
    val save = if (isBn) "সংরক্ষণ করুন" else "Save"
    val update = if (isBn) "আপডেট করুন" else "Update"
    val delete = if (isBn) "মুছে ফেলুন" else "Delete"
    val cancel = if (isBn) "বাতিল" else "Cancel"
    val searchHint = if (isBn) "নোট বা ক্যাটাগরি দিয়ে খুঁজুন..." else "Search by note or category..."
    val noTransactions = if (isBn) "কোনো লেনদেন পাওয়া যায়নি" else "No transactions found"

    // Budget
    val monthlyBudgetTitle = if (isBn) "মাসিক বাজেট পরিকল্পনা" else "Monthly Budget Planner"
    val setBudget = if (isBn) "বাজেট সেট করুন" else "Set Budget"
    val spent = if (isBn) "খরচ হয়েছে" else "Spent"
    val remaining = if (isBn) "বাকি আছে" else "Remaining"
    val totalBudget = if (isBn) "মোট বাজেট" else "Total Budget"
    val budgetWarn80 = if (isBn) "সতর্কতা: ৮০% এর বেশি বাজেট ব্যবহার হয়েছে!" else "Warning: Over 80% of budget used!"
    val budgetExceeded = if (isBn) "বাজেট অতিক্রম করেছে!" else "Budget Exceeded!"
    val budgetLimit = if (isBn) "মাসিক সীমা" else "Monthly Limit"

    // Savings
    val totalSavings = if (isBn) "মোট সঞ্চয়" else "Total Savings"
    val monthlySavingsTarget = if (isBn) "মাসিক সঞ্চয় লক্ষ্য" else "Monthly Savings Goal"
    val addSavingsGoal = if (isBn) "নতুন সঞ্চয় লক্ষ্য" else "New Savings Goal"
    val addMoney = if (isBn) "টাকা জমা করুন" else "Add Money"
    val savingsHistory = if (isBn) "সঞ্চয়ের ইতিহাস" else "Savings History"
    val goalTitle = if (isBn) "লক্ষ্যের নাম" else "Goal Title"
    val targetAmount = if (isBn) "লক্ষ্য পরিমাণ" else "Target Amount"

    // Lend & Borrow
    val toReceive = if (isBn) "পাবো (পাওনা)" else "To Receive (Lent)"
    val toRepay = if (isBn) "দিবো (দেনা)" else "To Repay (Borrowed)"
    val addLendBorrow = if (isBn) "ধার-দেনা যোগ করুন" else "Add Lend / Borrow"
    val personName = if (isBn) "ব্যক্তির নাম" else "Person Name"
    val phoneOptional = if (isBn) "ফোন নম্বর (ঐচ্ছিক)" else "Phone Number (Optional)"
    val dueDate = if (isBn) "পরিশোধের তারিখ" else "Due Date"
    val paid = if (isBn) "পরিশোধিত" else "Paid"
    val unpaid = if (isBn) "বাকি" else "Unpaid"
    val partial = if (isBn) "আংশিক পরিশোধিত" else "Partial"
    val recordPayment = if (isBn) "কিস্তি / পরিশোধ যোগ" else "Record Payment"
    val paymentHistory = if (isBn) "পরিশোধের ইতিহাস" else "Payment History"

    // Statistics
    val weekly = if (isBn) "সাপ্তাহিক" else "Weekly"
    val monthly = if (isBn) "মাসিক" else "Monthly"
    val annual = if (isBn) "বার্ষিক" else "Annual"
    val customRange = if (isBn) "কাস্টম" else "Custom"
    val categoryBreakdown = if (isBn) "ক্যাটাগরি অনুযায়ী খরচ" else "Category Expense Breakdown"
    val highestCategories = if (isBn) "সর্বোচ্চ খরচের খাতসমূহ" else "Highest Spending Categories"
    val exportPdf = if (isBn) "PDF রিপোর্ট" else "Export PDF"
    val exportCsv = if (isBn) "CSV রিপোর্ট" else "Export CSV"

    // Categories
    val addCategory = if (isBn) "নতুন ক্যাটাগরি" else "New Category"
    val editCategory = if (isBn) "ক্যাটাগরি সম্পাদনা" else "Edit Category"
    val addExpenseCategory = if (isBn) "নতুন খরচ যোগ করুন" else "Add Expense Category"
    val editExpenseCategory = if (isBn) "খরচ ও লোগো এডিট" else "Edit Expense & Logo"
    val editShort = if (isBn) "এডিট" else "Edit"
    val newShort = if (isBn) "+ নতুন" else "+ New"
    val suggestedLogos = if (isBn) "প্রস্তাবিত লোগো (Suggested)" else "Suggested Logos"
    val allLogos = if (isBn) "সব লোগো" else "All Logos"
    val quickSuggestions = if (isBn) "দ্রুত পরামর্শ" else "Quick Presets"
    val nameBnLabel = if (isBn) "বাংলা নাম" else "Bengali Name"
    val nameEnLabel = if (isBn) "ইংরেজি নাম" else "English Name"
    val chooseIcon = if (isBn) "লোগো / আইকন বাছাই করুন" else "Choose Logo / Icon"
    val chooseColor = if (isBn) "রং বাছাই করুন" else "Choose Color"

    // Calendar
    val dailyBalance = if (isBn) "দৈনিক ব্যালেন্স" else "Daily Balance"
    val dayTransactions = if (isBn) "দিনের লেনদেনসমূহ" else "Day Transactions"

    // Settings
    val languageLabel = if (isBn) "ভাষা / Language" else "Language / ভাষা"
    val themeLabel = if (isBn) "থিম" else "Theme"
    val light = if (isBn) "লাইট" else "Light"
    val dark = if (isBn) "ডार्क" else "Dark"
    val system = if (isBn) "সিস্টেম" else "System"
    val textSizeLabel = if (isBn) "লেখার আকার" else "Text Size"
    val small = if (isBn) "ছোট" else "Small"
    val medium = if (isBn) "মাঝারি" else "Medium"
    val large = if (isBn) "বড়" else "Large"
    val moreMenuDesc = if (isBn) "মেনু (হোম, লেনদেন, বাজেট, পরিসংখ্যান, সেটিংস)" else "More navigation menu"
    val alarmSectionTitle = if (isBn) "টেক্সট এলাম" else "Text Alarm"
    val dailyAlarmLabel = if (isBn) "দৈনিক হিসাব এলাম" else "Daily Expense Alarm"
    val alarmTimeLabel = if (isBn) "এলামের সময়" else "Alarm Time"
    val alarmTextLabel = if (isBn) "এলামের বার্তা (টেক্সট)" else "Alarm Message (Text)"
    val textAlarmTitle = if (isBn) "টেক্সট এলাম" else "Text Alarm"
    val textAlarmSoundOnlyNote = if (isBn) "শুধুমাত্র নোটিফিকেশন সাউন্ড হবে (আর কিছু নয়)" else "Notification sound only"
    val textAlarmHint = if (isBn) "যেকোনো রিমাইন্ডার টেক্সট লিখুন..." else "Write any custom alarm text..."
    val addTextAlarmBtn = if (isBn) "টেক্সট এলাম সেট করুন" else "Set Text Alarm"
    val repeatDailyLabel = if (isBn) "প্রতিদিন" else "Daily"
    val oneTimeLabel = if (isBn) "একবার" else "Once"
    val testAlarmBtn = if (isBn) "সাউন্ড টেস্ট" else "Test Sound"
    val savedTextAlarms = if (isBn) "সেট করা টেক্সট এলামসমূহ" else "Saved Text Alarms"
    val noTextAlarms = if (isBn) "এখনো কোনো টেক্সট এলাম যোগ করা হয়নি" else "No text alarms added yet"
    val alarmSavedMsg = if (isBn) "টেক্সট এলাম সংরক্ষণ করা হয়েছে!" else "Text alarm saved!"
    val alarmTestSentMsg = if (isBn) "টেস্ট নোটিফিকেশন পাঠানো হয়েছে!" else "Test notification sent!"
    val notifPermissionMsg = if (isBn) "টেক্সট এলাম দেখাতে নোটিফিকেশন অনুমতি প্রয়োজন" else "Notification permission needed for text alarms"
    val backupRestore = if (isBn) "ডাটা ব্যাকআপ ও রিস্টোর" else "Data Backup & Restore"
    val backupData = if (isBn) "ডাটা ব্যাকআপ (JSON)" else "Backup Data (JSON)"
    val restoreBackup = if (isBn) "ব্যাকআপ রিস্টোর" else "Restore Backup"
    val exportPersonalData = if (isBn) "ব্যক্তিগত ডাটা এক্সপোর্ট (CSV)" else "Export Personal Data (CSV)"
    val privacySecurity = if (isBn) "প্রাইভেসি ও নিরাপত্তা" else "Privacy & Security"
    val hideBalancesLabel = if (isBn) "ড্যাশবোর্ডে ব্যালেন্স লুকান" else "Hide Balances on Dashboard"
    val pinLockLabel = if (isBn) "৪-ডিজিট পিন লক" else "4-Digit PIN Lock"
    val biometricLockLabel = if (isBn) "বায়োমেট্রিক আনলক" else "Biometric Unlock"
    val setPinTitle = if (isBn) "৪-ডিজিট পিন সেট করুন" else "Set 4-Digit PIN"
    val enterPinTitle = if (isBn) "MoneyTracker আনলক করুন" else "Unlock MoneyTracker"
    val unlockWithBiometric = if (isBn) "ফিঙ্গারপ্রিন্ট / বায়োমেট্রিক ব্যবহার করুন" else "Use Biometric Unlock"
    val deleteExpensePasswordLabel = if (isBn) "খরচ মুছতে পাসওয়ার্ড" else "Password to Delete Expense"
    val deleteExpensePasswordDesc = if (isBn) "খরচ রিমুভ করার আগে পাসওয়ার্ড দিতে হবে" else "Require password before removing an expense"
    val setDeletePasswordTitle = if (isBn) "খরচ মোছার পাসওয়ার্ড সেট করুন" else "Set Expense Delete Password"
    val changeDeletePasswordBtn = if (isBn) "খরচ মোছার পাসওয়ার্ড পরিবর্তন" else "Change Delete Password"
    val verifyDeletePasswordTitle = if (isBn) "খরচ মুছতে পাসওয়ার্ড দিন" else "Enter Password to Delete Expense"
    val verifyDisableDeletePasswordTitle = if (isBn) "পাসওয়ার্ড সুরক্ষা বন্ধ করুন" else "Disable Delete Password"
    val currentPasswordLabel = if (isBn) "বর্তমান পাসওয়ার্ড" else "Current Password"
    val newPasswordLabel = if (isBn) "নতুন পাসওয়ার্ড (কমপক্ষে ৪ অক্ষর)" else "New Password (min 4 chars)"
    val confirmPasswordLabel = if (isBn) "পাসওয়ার্ড নিশ্চিত করুন" else "Confirm Password"
    val enterPasswordLabel = if (isBn) "পাসওয়ার্ড লিখুন" else "Enter Password"
    val wrongPasswordErr = if (isBn) "ভুল পাসওয়ার্ড! আবার চেষ্টা করুন" else "Incorrect password! Please try again"
    val passwordMismatchErr = if (isBn) "দুটি পাসওয়ার্ড মেলেনি" else "Passwords do not match"
    val passwordTooShortErr = if (isBn) "পাসওয়ার্ড কমপক্ষে ৪ অক্ষরের হতে হবে" else "Password must be at least 4 characters"
    val deletePasswordSavedMsg = if (isBn) "খরচ মোছার পাসওয়ার্ড সেট করা হয়েছে!" else "Delete expense password saved!"
    val deletePasswordDisabledMsg = if (isBn) "খরচ মোছার পাসওয়ার্ড বন্ধ করা হয়েছে" else "Delete expense password disabled"
    val expenseDeletedMsg = if (isBn) "খরচ মুছে ফেলা হয়েছে" else "Expense deleted"
    val invalidAmountErr = if (isBn) "সঠিক পরিমাণ লিখুন (০ এর বেশি)" else "Please enter a valid amount greater than 0"
    val invalidInputErr = if (isBn) "সব প্রয়োজনীয় ঘর সঠিকভাবে পূরণ করুন" else "Please fill in all required fields"

    // Monthly Archive
    val monthlyRecords = if (isBn) "মাসিক হিসাব" else "Monthly Records"
    val monthlyRecordsSubtitle = if (isBn) "মাস অনুযায়ী সংরক্ষিত আয়, খরচ ও সকল তথ্য" else "Saved income, expenses & full details by month"
    val savedMonthsCountSuffix = if (isBn) "টি মাসের হিসাব সংরক্ষিত" else "months saved"
    val currentMonthBadge = if (isBn) "চলতি মাস" else "Current Month"
    val savedBadge = if (isBn) "সংরক্ষিত" else "Saved"
    val openMonthDetails = if (isBn) "ট্যাপ করে সকল তথ্য দেখুন" else "Tap to view all details"
    val allYears = if (isBn) "সব বছর" else "All Years"
    val allMonths = if (isBn) "সব মাস" else "All Months"
    val avgDailyExpense = if (isBn) "দৈনিক গড় খরচ" else "Avg. Daily Expense"
    val paymentSummaryTitle = if (isBn) "পেমেন্ট মাধ্যম অনুযায়ী খরচ" else "Expense by Payment Method"
    val monthlyBudgetStatus = if (isBn) "এই মাসের বাজেট অবস্থা" else "Monthly Budget Status"
    val allMonthTransactions = if (isBn) "এই মাসের সকল লেনদেন" else "All Transactions This Month"
    val addExpenseThisMonth = if (isBn) "এই মাসে খরচ যোগ" else "Add Expense Here"
    val categoryDetailsTitle = if (isBn) "ক্যাটাগরি অনুযায়ী বিস্তারিত" else "Category Details"
    val noMonthData = if (isBn) "এই মাসে এখনো কোনো হিসাব সংরক্ষিত নেই" else "No records saved for this month yet"
    val viewByMonth = if (isBn) "মাস অনুযায়ী দেখুন" else "By Month"
}
