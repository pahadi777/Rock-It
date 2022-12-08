package com.example.rockit

import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

const val BASE_URL = "https://jiosaavn-api-git-main-sumitkolhe.vercel.app/"

interface Request
{
    @GET("playlists?id=100294896")
    fun getsongs() : Call<Response>

    @GET("search/songs")
    fun searchsongs(@Query("query") query: String , @Query("limit") limit : Int) : Call<Response>
}

object SongService
{
    val SongInstance : Request
    init {
        val retrofit = Retrofit.Builder().baseUrl(BASE_URL).addConverterFactory(GsonConverterFactory.create()).build()
        SongInstance = retrofit.create(Request::class.java)
    }
}