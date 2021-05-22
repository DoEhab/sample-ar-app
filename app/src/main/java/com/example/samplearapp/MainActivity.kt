package com.example.samplearapp

import ApiHttpClient.apiClient
import PlaceNode
import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import com.example.samplearapp.augmentedReality.AugmentedRealityFragment
import com.example.samplearapp.model.Place
import com.example.samplearapp.model.getPositionVector
import com.example.samplearapp.service.PlacesService
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapFragment
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.ar.sceneform.AnchorNode
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers

class MainActivity : AppCompatActivity() {
    private lateinit var arFragment: AugmentedRealityFragment
    private lateinit var mapFragment: SupportMapFragment
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var placesService: PlacesService
    private var places: List<Place>? = null

    private val orientationAngles = FloatArray(3)

    private var anchorNode: AnchorNode? = null
    private var markers: MutableList<Marker> = emptyList<Marker>().toMutableList()
    private var currentLocation: Location? = null
    private var map: GoogleMap? = null

    private val _compositeDisposable = CompositeDisposable()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        arFragment =
            supportFragmentManager.findFragmentById(R.id.ar_fragment) as AugmentedRealityFragment
        mapFragment =
            supportFragmentManager.findFragmentById(R.id.map_fragment) as SupportMapFragment

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        placesService = apiClient.create(PlacesService::class.java)

        setUpMap()
        setUpAr()
    }


    private fun setUpMap() {
        mapFragment.getMapAsync { googleMap ->
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission.

                Toast.makeText(this, R.string.permission_not_granted,Toast.LENGTH_SHORT)
            } else {
                googleMap.isMyLocationEnabled = true
                fusedLocationClient.lastLocation
                    .addOnSuccessListener { location ->
                        currentLocation = location
                        val pos = CameraPosition.fromLatLngZoom(LatLng(location.latitude, location.longitude), 13f)
                        googleMap.moveCamera(CameraUpdateFactory.newCameraPosition(pos))
                        getNearbyPlaces(location)
                    }
            }

            googleMap.setOnMarkerClickListener { marker ->
                val tag = marker.tag
                if (tag !is Place) {
                    return@setOnMarkerClickListener false
                }
                showInfoWindow(tag)
                return@setOnMarkerClickListener true
            }
            map = googleMap
        }
    }


    private fun setUpAr() {
        arFragment.setOnTapArPlaneListener { hitResult, _, _ ->
            // Create anchor
            val anchor = hitResult.createAnchor()
            anchorNode = AnchorNode(anchor)
            anchorNode?.setParent(arFragment.arSceneView.scene)
            addPlaces(anchorNode!!)
        }
    }

    private fun addPlaces(anchorNode: AnchorNode) {
        val currentLocation = currentLocation
        if (currentLocation == null) {
            Log.w("", "Location has not been determined yet")
            return
        }

        val places = places
        if (places == null) {
            Log.w("", "No places to put")
            return
        }

        for (place in places) {
            // Add the place in AR
            val placeNode = PlaceNode(this, place)
            placeNode.setParent(anchorNode)
            placeNode.localPosition = place.getPositionVector(orientationAngles[0], LatLng(currentLocation.latitude,currentLocation.longitude))
            placeNode.setOnTapListener { _, _ ->
                showInfoWindow(place)
            }

            // Add the place in maps
            map?.let {
                val marker = it.addMarker(
                    MarkerOptions()
                        .position(place.geometry.location.latLng)
                        .title(place.name)
                )
                marker.tag = place
                markers.add(marker)
            }
        }
    }

    private fun showInfoWindow(place: Place) {
        // Show in AR
        val matchingPlaceNode = anchorNode?.children?.filter {
            it is PlaceNode
        }?.first {
            val otherPlace = (it as PlaceNode).place ?: return@first false
            return@first otherPlace == place
        } as? PlaceNode
        matchingPlaceNode?.showInfoWindow()

        // Show as marker
        val matchingMarker = markers.firstOrNull {
            val placeTag = (it.tag as? Place) ?: return@firstOrNull false
            return@firstOrNull placeTag == place
        }
        matchingMarker?.showInfoWindow()
    }

    private fun getNearbyPlaces(location: Location?) {
        _compositeDisposable.add(
            placesService.nearbyPlaces(
                apiKey = getString(R.string.google_maps_key),
                location = "${location?.latitude},${location?.longitude}",
                radiusInMeters = 2000,
                placeType = "park"
            ).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread()).subscribe({ listOfPlaces ->
                    places = listOfPlaces.results
                }, {
                    Log.e("Error", it.message.toString() )

                })
        )

    }

}

