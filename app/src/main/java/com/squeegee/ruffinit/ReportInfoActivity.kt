package com.squeegee.ruffinit

import android.view.View
import android.view.ViewManager
import org.jetbrains.anko.button
import org.jetbrains.anko.verticalLayout
import android.support.v4.app.NotificationCompat.getExtras
import android.os.Bundle

class ReportInfoActivity: BaseActivity() {

    lateinit var rep: Report

    override fun createView(manager: ViewManager): View {
        return manager.verticalLayout {
            val extras = intent.extras
            if (extras != null) {
                var data = extras!!.getString("id")
                rep = Report(data)
            }
            if(rep != null) {
                println(rep.state)
            }


        }
    }
}