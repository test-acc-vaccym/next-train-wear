package me.eigenein.nexttrainwear

import java.util.*

operator fun Date.minus(other: Date): Long {
    return this.time - other.time
}

fun Long.toHmDurationString(): String {
    return String.format("%d:%02d", this / 3600000, (this % 3600000) / 60000)
}

fun Long.toHmsDurationString(): String {
    val hours = this / 3600000
    val minutes = this / 60000
    val seconds = (this % 60000) / 1000
    if (hours == 0L) {
        return String.format("%d:%02d", minutes, seconds)
    }
    return String.format("%d:%02d:%02d", hours, minutes, seconds)
}
