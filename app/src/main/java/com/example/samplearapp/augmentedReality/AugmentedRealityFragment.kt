package com.example.samplearapp.augmentedReality

import android.Manifest
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.samplearapp.R
import com.google.ar.sceneform.ux.ArFragment

class AugmentedRealityFragment : ArFragment() {
    override fun getAdditionalPermissions(): Array<String> =
        listOf(Manifest.permission.ACCESS_FINE_LOCATION).toTypedArray()
}