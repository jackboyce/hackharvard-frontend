package com.squeegee.ruffinit

import android.view.ViewManager
import com.getbase.floatingactionbutton.FloatingActionsMenu
import org.jetbrains.anko.custom.ankoView


inline fun ViewManager.floatingActionsMenu(init: FloatingActionsMenu.() -> Unit): FloatingActionsMenu =
    ankoView({ FloatingActionsMenu(it) }, theme = 0, init = init)