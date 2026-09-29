package com.engineaudio.soundbank

import android.content.Context
import android.util.Log
import com.engineaudio.EngineType
import com.engineaudio.V6AudioEngine
import java.io.File
import java.io.FileInputStream

/**
 * Carrega bancos de sons reais (WAV PCM) das pastas de assets ou diretórios externos (pendrive/SD).
 */
object SoundBankLoader {
    private const val TAG = "SoundBankLoader"

    const val TRACK_IDLE  = 0
    const val TRACK_LOW   = 1
    const val TRACK_MID   = 2
    const val TRACK_HIGH  = 3
    const val TRACK_DECEL = 4

    /**
     * Carrega banco de sons oficial dos assets do app para o tipo de motor especificado.
     */
    fun loadFromAssets(
        context: Context,
        engineType: EngineType,
        engine: V6AudioEngine
    ): Boolean {
        val assetDir = "engine_sounds/${engineType.folderName}"

        Log.i(TAG, "Carregando SoundBank oficial dos assets: $assetDir para ${engineType.displayName}")
        engine.clearSamples()

        val assetManager = context.assets
        val baseRpms = engineType.baseRpms

        val trackFiles = arrayOf(
            Pair(TRACK_IDLE, "idle.wav"),
            Pair(TRACK_LOW, "low_on.wav"),
            Pair(TRACK_MID, "mid_on.wav"),
            Pair(TRACK_HIGH, "high_on.wav"),
            Pair(TRACK_DECEL, "decel.wav")
        )

        var loadedCount = 0
        for ((trackId, fileName) in trackFiles) {
            try {
                val input = assetManager.open("$assetDir/$fileName")
                val wav = WavReader.read(input)
                if (wav != null) {
                    engine.loadSampleTrack(
                        trackId = trackId,
                        samples = wav.samples,
                        numFrames = wav.numFrames,
                        channels = wav.channels,
                        baseRPM = baseRpms[trackId]
                    )
                    loadedCount++
                }
            } catch (e: Exception) {
                Log.w(TAG, "Asset não encontrado: $assetDir/$fileName (${e.message})")
            }
        }

        // 2. Carrega Pops / Backfires de escape
        for (popName in arrayOf("pop1.wav", "pop2.wav", "pop3.wav")) {
            try {
                val input = assetManager.open("$assetDir/$popName")
                val wav = WavReader.read(input)
                if (wav != null) {
                    engine.addPopSample(wav.samples, wav.numFrames, wav.channels)
                }
            } catch (_: Exception) {}
        }

        // 3. Carrega BOV (Blow-Off Valve)
        try {
            val input = assetManager.open("$assetDir/bov.wav")
            val wav = WavReader.read(input)
            if (wav != null) {
                engine.setBovSample(wav.samples, wav.numFrames, wav.channels)
            }
        } catch (_: Exception) {}

        Log.i(TAG, "SoundBank carregado com sucesso: $loadedCount/5 tracks principais.")
        return loadedCount > 0
    }

    /**
     * Carrega amostras personalizadas de um diretório externo (ex: pendrive USB ou armazenamento interno).
     */
    fun loadFromDirectory(
        directory: File,
        engine: V6AudioEngine,
        idleRpm: Float = 800f,
        lowRpm: Float = 2200f,
        midRpm: Float = 4400f,
        highRpm: Float = 6800f,
        decelRpm: Float = 3200f
    ): Boolean {
        if (!directory.exists() || !directory.isDirectory) {
            Log.e(TAG, "Diretório inválido: ${directory.absolutePath}")
            return false
        }

        Log.i(TAG, "Carregando SoundBank personalizado de: ${directory.absolutePath}")
        engine.clearSamples()

        val trackFiles = arrayOf(
            Triple(TRACK_IDLE, File(directory, "idle.wav"), idleRpm),
            Triple(TRACK_LOW, File(directory, "low_on.wav"), lowRpm),
            Triple(TRACK_MID, File(directory, "mid_on.wav"), midRpm),
            Triple(TRACK_HIGH, File(directory, "high_on.wav"), highRpm),
            Triple(TRACK_DECEL, File(directory, "decel.wav"), decelRpm)
        )

        var loadedCount = 0
        for ((trackId, file, rpm) in trackFiles) {
            if (file.exists() && file.isFile) {
                val wav = WavReader.read(FileInputStream(file))
                if (wav != null) {
                    engine.loadSampleTrack(
                        trackId = trackId,
                        samples = wav.samples,
                        numFrames = wav.numFrames,
                        channels = wav.channels,
                        baseRPM = rpm
                    )
                    loadedCount++
                }
            }
        }

        for (popName in arrayOf("pop1.wav", "pop2.wav", "pop3.wav")) {
            val popFile = File(directory, popName)
            if (popFile.exists()) {
                val wav = WavReader.read(FileInputStream(popFile))
                if (wav != null) {
                    engine.addPopSample(wav.samples, wav.numFrames, wav.channels)
                }
            }
        }

        val bovFile = File(directory, "bov.wav")
        if (bovFile.exists()) {
            val wav = WavReader.read(FileInputStream(bovFile))
            if (wav != null) {
                engine.setBovSample(wav.samples, wav.numFrames, wav.channels)
            }
        }

        return loadedCount > 0
    }
}
