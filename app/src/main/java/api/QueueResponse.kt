package com.example.olfuantipoloregistrarqueueingmanagementsystem.api

data class QueueResponse(
    val success: Boolean,
    val queue: List<RequestItem>?,
    val requests: List<RequestItem>? // <-- needed for displayRequests()
)
