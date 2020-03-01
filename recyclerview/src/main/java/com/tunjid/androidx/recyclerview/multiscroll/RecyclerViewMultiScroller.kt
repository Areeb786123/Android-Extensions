package com.tunjid.androidx.recyclerview.multiscroll

import android.view.MotionEvent
import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.doOnLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

@Experimental
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
annotation class ExperimentalRecyclerViewMultiScrolling

/**
 * A class that synchronizes scrolling multiple [RecyclerView]s. Useful for horizontally scrolling
 * layouts like stats.
 *
 * The synchronized RecyclerViews must have child items that are equal in size around the specified
 * orientation, have the same amount of items and also use a [LinearLayoutManager]
 */
@ExperimentalRecyclerViewMultiScrolling
class RecyclerViewMultiScroller(
        @RecyclerView.Orientation private val orientation: Int = RecyclerView.HORIZONTAL,
        private val cellSizer: CellSizer = DynamicCellSizer(orientation)
) {
    var displacement = 0
        private set
    private var childSize = 0
    private var active: RecyclerView? = null
    private val syncedScrollers = mutableSetOf<RecyclerView>()

    private val onScrollListener = object : RecyclerView.OnScrollListener() {
        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            if (orientation == RecyclerView.HORIZONTAL && dx == 0) return
            if (orientation == RecyclerView.VERTICAL && dy == 0) return

            if (active != null && recyclerView != active) return

            active = recyclerView
            syncedScrollers.forEach { if (it != recyclerView) it.scrollBy(dx, dy) }
            displacement += if (orientation == RecyclerView.HORIZONTAL) dx else dy
        }

        override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
            if (active != recyclerView || newState != RecyclerView.SCROLL_STATE_IDLE) return
            active = null
        }
    }

    private val onAttachStateChangeListener = object : View.OnAttachStateChangeListener {
        override fun onViewAttachedToWindow(v: View?) {
            if (v is RecyclerView) include(v)
        }

        override fun onViewDetachedFromWindow(v: View?) {
            if (v is RecyclerView) exclude(v)
        }
    }

    private val onItemTouchListener = object : RecyclerView.SimpleOnItemTouchListener() {
        override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
            // If the user flung the list, then touches any other synced list, stop scrolling
            if (e.actionMasked == MotionEvent.ACTION_DOWN && active != null) active?.stopScroll()
            return false // always return false, we aren't trying to override default scrolling
        }
    }

    fun clear() {
        active = null
        syncedScrollers.clear {
            exclude(it)
            it.removeOnAttachStateChangeListener(onAttachStateChangeListener)
        }
        cellSizer.clear()
    }

    fun add(recyclerView: RecyclerView) {
        if (syncedScrollers.contains(recyclerView)) return

        recyclerView.calculateChildSize()

        include(recyclerView)
        recyclerView.addOnAttachStateChangeListener(onAttachStateChangeListener)
    }

    private fun include(recyclerView: RecyclerView) {
        recyclerView.doOnLayout {
            if (syncedScrollers.contains(recyclerView)) return@doOnLayout
            recyclerView.sync()
            syncedScrollers.add(recyclerView)
            cellSizer.include(recyclerView)
            recyclerView.addOnScrollListener(onScrollListener)
            recyclerView.addOnItemTouchListener(onItemTouchListener)
        }
        if (!ViewCompat.isLaidOut(recyclerView) || recyclerView.isLayoutRequested) recyclerView.requestLayout()
    }

    private fun exclude(recyclerView: RecyclerView) {
        recyclerView.removeOnItemTouchListener(onItemTouchListener)
        recyclerView.removeOnScrollListener(onScrollListener)
        cellSizer.exclude(recyclerView)
        syncedScrollers.remove(recyclerView)
    }

    private fun RecyclerView.calculateChildSize() = doOnLayout {
        val child = getChildAt(0) ?: return@doOnLayout
        childSize = if (orientation == RecyclerView.HORIZONTAL) child.width else child.height
    }

    private fun RecyclerView.sync() {
        if (childSize == 0) return

        var offset = displacement
        var position = 0
        while (offset > 0) {
            offset -= cellSizer.sizeAt(position)
            position++
        }

        val linearLayoutManager = layoutManager as LinearLayoutManager
        linearLayoutManager.scrollToPositionWithOffset(position, -offset)
    }
}