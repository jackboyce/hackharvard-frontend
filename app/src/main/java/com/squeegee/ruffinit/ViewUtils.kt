package com.squeegee.ruffinit

import android.view.ViewManager
import com.getbase.floatingactionbutton.FloatingActionsMenu
import com.sothree.slidinguppanel.SlidingUpPanelLayout
import org.jetbrains.anko.custom.ankoView


inline fun ViewManager.floatingActionsMenu(init: FloatingActionsMenu.() -> Unit): FloatingActionsMenu =
    ankoView({ FloatingActionsMenu(it) }, theme = 0, init = init)

inline fun ViewManager.slidingUpPanelLayout(init: SlidingUpPanelLayout.() -> Unit): SlidingUpPanelLayout =
    ankoView({ SlidingUpPanelLayout(it) }, theme = 0, init = init)