package com.squeegee.ruffinit

import android.view.View
import android.view.ViewManager
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import org.jetbrains.anko.verticalLayout


class MapActivity: BaseActivity() {
    override fun createView(manager: ViewManager): View {
        return manager.verticalLayout {
            id = R.id.contentPanel

            val mapFrag = SupportMapFragment.newInstance()
            supportFragmentManager.beginTransaction()
                .add(id, mapFrag)
                .commit()

            mapFrag.getMapAsync {
                println("fin")
                it.addMarker(MarkerOptions().position(LatLng(10.0, 10.0)).title("test"))
            }
        }
    }

}