package com.example.otukai_watch.VoiceChat.model

import java.io.Serializable

//class Recording(var uri: String, var fileName: String, var isPlaying: Boolean) :Serializable {}
class Voice(val user: String, val side: Int, val id: Int, val time: Int)
class Recording(val voice: Voice, var uri: String, var fileName: String, var isPlaying: Boolean) :Serializable {}
