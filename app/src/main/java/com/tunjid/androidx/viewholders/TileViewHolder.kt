package com.tunjid.androidx.viewholders

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.graphics.Color
import android.view.ViewGroup
import com.tunjid.androidx.databinding.ViewholderTileBinding
import com.tunjid.androidx.model.Tile
import com.tunjid.androidx.recyclerview.viewbinding.BindingViewHolder
import com.tunjid.androidx.recyclerview.viewbinding.viewHolderDelegate
import com.tunjid.androidx.recyclerview.viewbinding.viewHolderFrom

fun ViewGroup.tileViewHolder() = viewHolderFrom(ViewholderTileBinding::inflate).apply {
    animator = ValueAnimator.ofObject(ArgbEvaluator(), Color.RED).setDuration(COLOR_CHANGE_DURATION.toLong())
    listener = ValueAnimator.AnimatorUpdateListener { binding.tileText.setTextColor(it.animatedValue as Int) }
}

var BindingViewHolder<ViewholderTileBinding>.tile by viewHolderDelegate<Tile>()
private var BindingViewHolder<ViewholderTileBinding>.animator by viewHolderDelegate<ValueAnimator>()
private var BindingViewHolder<ViewholderTileBinding>.listener by viewHolderDelegate<ValueAnimator.AnimatorUpdateListener>()

fun BindingViewHolder<ViewholderTileBinding>.bind(tile: Tile) {
    this.tile = tile
    val tileText = binding.tileText
    tileText.text = tile.diffId

    animator.apply {
        setIntValues(tileText.currentTextColor, tile.color)
        addUpdateListener(listener)
        startDelay = START_DELAY.toLong() // Cheeky bit of code to keep scrolling smooth on fling
        start()
    }
}

fun BindingViewHolder<ViewholderTileBinding>.unbind() = animator.run {
    cancel()
    removeUpdateListener(listener)
}

private const val COLOR_CHANGE_DURATION = 1000
private const val START_DELAY = 300