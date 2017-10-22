package com.squeegee.ruffinit

import android.view.View
import android.view.ViewManager
import org.jetbrains.anko.verticalLayout


class ReportInfoActivity: BaseActivity() {
    override fun createView(manager: ViewManager): View {
        return manager.verticalLayout {
        }
    }
}