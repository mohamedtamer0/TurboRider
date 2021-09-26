package com.example.turborider.ui.home


import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.content.res.Resources
import android.os.Bundle
import android.os.Looper
import android.util.Log

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.core.app.ActivityCompat

import androidx.fragment.app.Fragment

import androidx.lifecycle.ViewModelProvider
import com.example.turborider.R
import com.example.turborider.databinding.FragmentHomeBinding

import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions

import com.google.firebase.database.*
import com.karumi.dexter.Dexter
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.single.PermissionListener


class HomeFragment : Fragment(), OnMapReadyCallback {


    private lateinit var mMap: GoogleMap
    private lateinit var homeViewModel: HomeViewModel
    private var _binding: FragmentHomeBinding? = null
    private lateinit var mapFragment: SupportMapFragment

    //Location
    lateinit var locationRequest: LocationRequest
    lateinit var locationCallback: LocationCallback
    lateinit var fusedLocationProviderClient: FusedLocationProviderClient


//    //Online System
//    private lateinit var onlineRef: DatabaseReference
//    private lateinit var currentUserRef: DatabaseReference
//    private lateinit var driversLocationRef: DatabaseReference
//    private lateinit var geoFire: GeoFire

//    private val onlineValueEventListener = object : ValueEventListener {
//        override fun onDataChange(snapshot: DataSnapshot) {
//            if (snapshot.exists()) {
//                currentUserRef.onDisconnect().removeValue()
//            }
//        }
//
//        override fun onCancelled(error: DatabaseError) {
//            Snackbar.make(mapFragment.requireView(), error.message, Snackbar.LENGTH_LONG).show()
//        }
//
//    }


//    override fun onResume() {
//        super.onResume()
//        registerOnlineSystem()
//    }
//
//    private fun registerOnlineSystem() {
//        onlineRef.addValueEventListener(onlineValueEventListener)
//    }


    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        homeViewModel =
            ViewModelProvider(this).get(HomeViewModel::class.java)

        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root

        init()


        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        return root
    }


    private fun init() {

        locationRequest = LocationRequest()
        locationRequest.priority = LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY
        locationRequest.setFastestInterval(3000)
        locationRequest.setSmallestDisplacement(10f)
        locationRequest.interval = 5000

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(p0: LocationResult?) {
                super.onLocationResult(p0)
                val newPos = LatLng(p0!!.lastLocation.latitude,p0!!.lastLocation.longitude)
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(newPos,18f))
            }
        }

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(requireContext())
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {

            return
        }
        fusedLocationProviderClient.requestLocationUpdates(locationRequest,locationCallback,Looper.myLooper())

    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }


    override fun onMapReady(googleMap: GoogleMap?) {
        mMap = googleMap!!

        Dexter.withContext(requireContext())
            .withPermission(Manifest.permission.ACCESS_FINE_LOCATION)
            .withListener(object : PermissionListener {
                override fun onPermissionGranted(p0: PermissionGrantedResponse?) {
                    //Enable btn first
                    if (ActivityCompat.checkSelfPermission(
                            requireContext(),
                            Manifest.permission.ACCESS_FINE_LOCATION
                        ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                            requireContext(),
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        return
                    }
                    mMap.isMyLocationEnabled = true
                    mMap.uiSettings.isMyLocationButtonEnabled = true
                    mMap.setOnMyLocationClickListener {
                        fusedLocationProviderClient!!.lastLocation
                            .addOnFailureListener {
                                Toast.makeText(context!!, it.message, Toast.LENGTH_LONG).show()

                            }.addOnSuccessListener {
                                val userLatLng = LatLng(it.latitude, it.longitude)
                                mMap.animateCamera(
                                    CameraUpdateFactory.newLatLngZoom(
                                        userLatLng,
                                        18f
                                    )
                                )
                            }
                        true

                    }


                    //LayOut
                    val locationButton = ( mapFragment.requireView()
                        .findViewById<View>("1".toInt())!!
                        .parent!! as View).findViewById<View>("2".toInt())
                    val params = locationButton.layoutParams as RelativeLayout.LayoutParams
                    params.addRule(RelativeLayout.ALIGN_PARENT_TOP,0)
                    params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE)
                    params.bottomMargin = 50


                }

                override fun onPermissionDenied(p0: PermissionDeniedResponse?) {
                    Toast.makeText(
                        context!!,
                        "Permission " + p0!!.permissionName + " was denied",
                        Toast.LENGTH_LONG
                    ).show()
                }

                override fun onPermissionRationaleShouldBeShown(
                    p0: PermissionRequest?,
                    p1: PermissionToken?
                ) {

                }

            }).check()

        try {
            val success = googleMap.setMapStyle(
                MapStyleOptions.loadRawResourceStyle(
                    context,
                    R.raw.uber_maps_style
                )
            )
            if (!success) {
                Log.e("Tamer_Error", "style error")
            }
        } catch (e: Resources.NotFoundException) {
            Log.e("Tamer_Error", e.message!!)
        }



    }


    override fun onDestroy() {
        fusedLocationProviderClient.removeLocationUpdates(locationCallback)
        super.onDestroy()
    }

}