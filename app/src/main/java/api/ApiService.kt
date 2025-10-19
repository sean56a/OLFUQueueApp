package com.example.olfuantipoloregistrarqueueingmanagementsystem.api

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.Query

interface ApiService {

    // Get all requests (optionally filter by status)
    @GET("get_requests.php")
    fun getRequests(@Query("status") status: String? = null): Call<QueueResponse>

    // Get queue and user requests (optionally filter by student_number)
    @GET("get_queue.php")
    fun getQueueWithRequests(@Query("student_number") studentNumber: String? = null): Call<QueueResponse>

    // Submit a new request
    @FormUrlEncoded
    @POST("submit_request.php")
    fun submitRequest(
        @Field("first_name") first_name: String,
        @Field("last_name") last_name: String,
        @Field("student_number") student_number: String,
        @Field("section") section: String,
        @Field("department") department: String,
        @Field("document") document: String,
        @Field("walk_in") walk_in: Int
    ): Call<QueueResponse>

    // Update queue/request status
    @FormUrlEncoded
    @POST("update_queue.php")
    fun updateQueue(
        @Field("id") id: Int,
        @Field("status") status: String,
        @Field("served_by") served_by: String? = null
    ): Call<QueueResponse>
}
