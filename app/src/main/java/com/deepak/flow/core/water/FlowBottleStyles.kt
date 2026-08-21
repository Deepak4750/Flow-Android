package com.deepak.flow.core.water

import androidx.annotation.DrawableRes
import com.deepak.flow.R
import com.deepak.flow.core.model.UserProfile

object FlowBottleStyles {
    val lastIndex: Int get() = UserProfile.BOTTLE_STYLE_COUNT - 1

    @DrawableRes
    fun drawableRes(index: Int): Int = when (index.coerceIn(0, lastIndex)) {
        0 -> R.drawable.bottle_01
        1 -> R.drawable.bottle_02
        else -> R.drawable.bottle_03
    }
}
