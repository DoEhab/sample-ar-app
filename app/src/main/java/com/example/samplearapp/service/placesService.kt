package com.example.samplearapp.service

import com.example.samplearapp.model.PlacesResponse
import io.reactivex.Flowable
import retrofit2.http.GET
import retrofit2.http.Query

interface PlacesService {

    @GET("nearbysearch/json")
    fun nearbyPlaces(
        @Query("key") apiKey: String,
        @Query("location") location: String,
        @Query("radius") radiusInMeters: Int,
        @Query("type") placeType: String
    ): Flowable<PlacesResponse>

}