package com.engineaudio.soundbank

/**
 * Representa os dados PCM decodificados de um arquivo WAV normalizados para float [-1.0f, 1.0f].
 */
data class WavAudioData(
    val samples: FloatArray,
    val numFrames: Int,
    val channels: Int,
    val sampleRate: Int
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as WavAudioData
        return numFrames == other.numFrames &&
               channels == other.channels &&
               sampleRate == other.sampleRate &&
               samples.contentEquals(other.samples)
    }

    override fun hashCode(): Int {
        var result = numFrames
        result = 31 * result + channels
        result = 31 * result + sampleRate
        return result
    }
}
