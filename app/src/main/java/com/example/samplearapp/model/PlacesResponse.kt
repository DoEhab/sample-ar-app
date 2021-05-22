package com.example.samplearapp.model

import com.squareup.moshi.Json


/**
 * Data class encapsulating a response from the nearby search call to the Places API.
 */
data class PlacesResponse(
   @Json(name = "results")
   val results: List<Place>
)

