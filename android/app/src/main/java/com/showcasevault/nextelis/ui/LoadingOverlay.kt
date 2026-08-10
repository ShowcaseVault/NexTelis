package com.showcasevault.nextelis.ui

import android.view.View
import com.showcasevault.nextelis.R

/**
 * Wraps the full-screen logo-on-black loading view (view_loading_overlay.xml)
 * that's shown while any API call is in flight. Include the overlay's root
 * FrameLayout in an activity's layout, then wire it here — keeps the
 * show/hide logic out of each activity.
 */
class LoadingOverlay(rootView: View) {

    private val overlay: View = rootView.findViewById(R.id.loadingOverlay)

    fun show() {
        overlay.visibility = View.VISIBLE
    }

    fun hide() {
        overlay.visibility = View.GONE
    }
}
