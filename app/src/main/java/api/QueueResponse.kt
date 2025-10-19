package com.example.olfuantipoloregistrarqueueingmanagementsystem.api

data class QueueResponse(
    val success: Boolean,
    val message: String,                 // <-- add this
    val queue: List<RequestItem>? = null,
    val requests: List<RequestItem>? = null
)
