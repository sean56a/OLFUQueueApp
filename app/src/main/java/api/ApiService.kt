package com.example.olfuantipoloregistrarqueueingmanagementsystem.api

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.Query

interface ApiService {

    @GET("get_requests.php")
    fun getRequests(): Call<QueueResponse>

    @GET("get_queue.php")
    fun getQueue(): Call<QueueResponse>

    @GET("get_queue.php")
    fun getQueueWithRequests(
        @Query("student_number") studentNumber: String
    ): Call<QueueResponse>

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

    @FormUrlEncoded
    @POST("update_queue.php")
    fun updateQueue(
        @Field("id") id: Int,
        @Field("status") status: String,
        @Field("served_by") served_by: String?
    ): Call<QueueResponse>
}
