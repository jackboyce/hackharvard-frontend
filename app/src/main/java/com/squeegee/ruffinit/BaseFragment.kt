package com.squeegee.ruffinit

import activitystarter.ActivityStarter
import activitystarter.MakeActivityStarter
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewManager
import io.reactivex.disposables.CompositeDisposable
import org.jetbrains.anko.AnkoContext
import org.jetbrains.anko.support.v4.ctx


abstract class BaseFragment: Fragment() {
    private var existingView: View? = null
    protected var subscriptions = CompositeDisposable()

    abstract fun createView(parent: ViewGroup?, manager: ViewManager): View
    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View {
        ActivityStarter.fill(this)
        return if (existingView != null) {
            existingView!!
        } else createView(container, AnkoContext.create(ctx, this))
    }

    override fun onDestroy() {
        subscriptions.clear()
        super.onDestroy()
    }
}