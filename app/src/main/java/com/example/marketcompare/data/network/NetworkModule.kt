package com.example.marketcompare.data.network

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object NetworkModule {
    // Replace with your backend base URL once available.
    private const val BASE_URL = "https://example.com/"

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val inventoryApi: InventoryApi by lazy {
        retrofit.create(InventoryApi::class.java)
    }

    val syncApi: SyncApi by lazy {
        retrofit.create(SyncApi::class.java)
    }
}
