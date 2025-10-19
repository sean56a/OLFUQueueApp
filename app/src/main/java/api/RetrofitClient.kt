package com.example.olfuantipoloregistrarqueueingmanagementsystem.api

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    // Emulator accesses local XAMPP server via 10.0.2.2
    private const val BASE_URL = "http://10.0.2.2/queue/api/"

    val instance: ApiService by lazy {
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        retrofit.create(ApiService::class.java)
    }
}
