package com.example.olfuantipoloregistrarqueueingmanagementsystem.api

data class RequestItem(
    val id: Int,
    val first_name: String,
    val last_name: String,
    val student_number: String,
    val section: String? = null,
    val department: String? = null,
    val documents: String,
    val status: String,
    val decline_reason: String? = null,
    val queueing_num: String? = null,
    val serving_position: Int? = null,
    val served_by: String? = null, // <-- ADD THIS
    val created_at: String,
    var isUserQueue: Boolean = false
) {
    val fullName: String
        get() = "$first_name $last_name"

    // computed property for remarks/action
    val remarks: String
        get() = when (status) {
            "Completed" -> "Claimed"
            "Declined" -> decline_reason ?: ""
            else -> ""
        }
}
