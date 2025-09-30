package com.example.olfuantipoloregistrarqueueingmanagementsystem

data class RequestItem(
    val id: Int,
    val first_name: String,
    val last_name: String,
    val student_number: String,
    val section: String?,
    val department_id: Int?,
    val department: String?,
    val last_school_year: String?,
    val last_semester: String?,
    val documents: String?,
    val notes: String?,
    val attachment: String?,
    val created_at: String?,
    val status: String?,
    val processing_time: String?,
    val decline_reason: String?,
    val claim_date: String?,
    val approved_date: String?,
    val completed_date: String?,
    val scheduled_date: String?,
    val processing_deadline: String?,
    val processing_start: String?,
    val processing_end: String?,
    val updated_at: String?,
    val queueing_num: Int?,
    val serving_position: Int?,
    val served_by: Int?,
    val walk_in: Int?
) {
    // convenience: combine first & last name
    val fullName: String
        get() = "$first_name $last_name"
}
