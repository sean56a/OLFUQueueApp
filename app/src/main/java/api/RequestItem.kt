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
    val queueing_num: Int? = null,       // <-- FIXED
    val serving_position: Int? = null,
    val served_by: String? = null,
    val created_at: String,
    var isUserQueue: Boolean = false
) {
    val fullName: String
        get() = "$first_name $last_name"

    val remarks: String
        get() = when (status) {
            "Completed" -> "Claimed"
            "Declined" -> decline_reason ?: ""
            else -> ""
        }

    val statusMessage: String
        get() = when (status) {
            "Serving" -> "\uD83C\uDF89 It's your turn! Please proceed to the counter."
            "In Queue Now" -> "You are in line."
            else -> status
        }
}


