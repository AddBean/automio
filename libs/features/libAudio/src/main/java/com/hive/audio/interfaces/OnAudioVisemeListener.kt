package com.hive.audio.interfaces

interface OnAudioVisemeListener {
    fun onVisemeChanged(visme: List<Pair<Float, Long>>)
    fun onAudioCompleted()
}