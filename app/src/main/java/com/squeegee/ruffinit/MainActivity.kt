package com.squeegee.ruffinit

import android.view.View
import android.view.ViewManager
import org.jetbrains.anko.verticalLayout
import android.support.v4.app.NotificationCompat.getExtras
import android.os.Bundle
import org.jetbrains.anko.button


class MainActivity: BaseActivity() {
    override fun createView(manager: ViewManager): View {
        return manager.verticalLayout {
            val extras = intent.extras
            var data = ""
            if (extras != null) {
                data = extras!!.getString("id")
            }
            button("Say Hello") {
            }
        }
    }
}