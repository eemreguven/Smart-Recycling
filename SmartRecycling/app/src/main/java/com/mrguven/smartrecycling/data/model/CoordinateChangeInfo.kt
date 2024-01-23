package com.mrguven.smartrecycling.data.model

import android.graphics.RectF

data class CoordinateChangeInfo(
    var title: String,
    var firstSeenBox: RectF,
    var lastSeenBox: RectF,
    var seenCount:Int = 0,
    var notSeenCount: Int = 0,
    var isReadyThrowing: Boolean = false
)
