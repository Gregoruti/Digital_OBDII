package com.engineaudio.soundbank

import android.util.Log
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Leitor ultra-rápido de arquivos WAV PCM 16-bit com zero dependências externas.
 * Compatível com canais Mono e Estéreo (44.1kHz / 48kHz).
 */
object WavReader {
    private const val TAG = "WavReader"

    fun read(inputStream: InputStream): WavAudioData? {
        return try {
            val bytes = inputStream.readBytes()
            val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)

            // 1. Verifica cabeçalho RIFF
            val riff = ByteArray(4)
            buffer.get(riff)
            if (String(riff) != "RIFF") {
                Log.e(TAG, "Formato inválido: não é RIFF")
                return null
            }

            buffer.int // fileSize - 8

            val wave = ByteArray(4)
            buffer.get(wave)
            if (String(wave) != "WAVE") {
                Log.e(TAG, "Formato inválido: não é WAVE")
                return null
            }

            var channels = 2
            var sampleRate = 44100
            var bitsPerSample = 16
            var dataOffset = -1
            var dataSize = 0

            // 2. Itera pelos Chunks até encontrar 'fmt ' e 'data'
            while (buffer.hasRemaining()) {
                val chunkId = ByteArray(4)
                if (buffer.remaining() < 8) break
                buffer.get(chunkId)
                val chunkSize = buffer.int
                val chunkName = String(chunkId)

                when (chunkName) {
                    "fmt " -> {
                        val audioFormat = buffer.short.toInt() // 1 = PCM
                        channels = buffer.short.toInt()
                        sampleRate = buffer.int
                        buffer.int // byteRate
                        buffer.short // blockAlign
                        bitsPerSample = buffer.short.toInt()

                        // Pula bytes extras do chunk fmt se houver
                        val remainingFmt = chunkSize - 16
                        if (remainingFmt > 0 && buffer.remaining() >= remainingFmt) {
                            buffer.position(buffer.position() + remainingFmt)
                        }
                    }
                    "data" -> {
                        dataOffset = buffer.position()
                        dataSize = chunkSize
                        break // Encontrou o bloco de áudio
                    }
                    else -> {
                        // Pula chunks desconhecidos (ex: LIST, JUNK, ID3)
                        if (buffer.remaining() >= chunkSize) {
                            buffer.position(buffer.position() + chunkSize)
                        } else {
                            break
                        }
                    }
                }
            }

            if (dataOffset < 0 || dataSize <= 0) {
                Log.e(TAG, "Chunk 'data' não encontrado no WAV")
                return null
            }

            if (bitsPerSample != 16) {
                Log.e(TAG, "Apenas WAV PCM 16-bit é suportado atualmente (detectado: $bitsPerSample-bit)")
                return null
            }

            val numSamples = dataSize / 2
            val numFrames = numSamples / channels
            val floatSamples = FloatArray(numSamples)

            buffer.position(dataOffset)
            val shortBuffer = buffer.asShortBuffer()
            val shortArray = ShortArray(numSamples)
            shortBuffer.get(shortArray)

            // Converte short 16-bit [-32768, 32767] para float [-1.0f, 1.0f]
            for (i in 0 until numSamples) {
                floatSamples[i] = shortArray[i] / 32768.0f
            }

            WavAudioData(
                samples = floatSamples,
                numFrames = numFrames,
                channels = channels,
                sampleRate = sampleRate
            )
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao decodificar WAV: ${e.message}", e)
            null
        } finally {
            try {
                inputStream.close()
            } catch (_: Exception) {}
        }
    }
}
