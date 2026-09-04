package com.example.ui.util

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/**
 * Generador de efectos de sonido nativos estilo WhatsApp para el chat
 * (Pop al enviar mensaje y Chime al recibir mensaje).
 * Sintetizado directamente en PCM para máxima rapidez, fidelidad y cero dependencias de archivos.
 */
object SoundEffectHelper {

    private const val TAG = "SoundEffectHelper"
    private const val SAMPLE_RATE = 44100
    private val scope = CoroutineScope(Dispatchers.Default)

    private val sentSoundBuffer: ShortArray by lazy {
        generateSentPopBuffer()
    }

    private val receivedSoundBuffer: ShortArray by lazy {
        generateReceivedChimeBuffer()
    }

    /**
     * Reproduce el sonido de envío de mensaje (Pop/Tick suave y nítido).
     */
    fun playSentSound() {
        scope.launch {
            try {
                playSound(sentSoundBuffer)
            } catch (e: Exception) {
                Log.w(TAG, "Error reproduciendo sonido de envío: ${e.message}")
            }
        }
    }

    /**
     * Reproduce el sonido de recepción de mensaje (Chime/Campana suave tipo WhatsApp).
     */
    fun playReceivedSound() {
        scope.launch {
            try {
                playSound(receivedSoundBuffer)
            } catch (e: Exception) {
                Log.w(TAG, "Error reproduciendo sonido de recepción: ${e.message}")
            }
        }
    }

    private fun playSound(audioBuffer: ShortArray) {
        val minBufferSize = AudioTrack.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        val bufferSize = maxOf(audioBuffer.size * 2, minBufferSize)

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        val audioFormat = AudioFormat.Builder()
            .setSampleRate(SAMPLE_RATE)
            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .build()

        val track = AudioTrack.Builder()
            .setAudioAttributes(audioAttributes)
            .setAudioFormat(audioFormat)
            .setBufferSizeInBytes(bufferSize)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()

        track.write(audioBuffer, 0, audioBuffer.size)
        track.play()

        // Liberar el recurso después de finalizar la reproducción
        val durationMs = (audioBuffer.size * 1000L) / SAMPLE_RATE
        Thread.sleep(durationMs + 30)
        try {
            track.stop()
            track.release()
        } catch (e: Exception) {
            // ignore
        }
    }

    /**
     * Genera la onda de sonido del pop de envío (duración: ~48ms con modulación armónica suave).
     */
    private fun generateSentPopBuffer(): ShortArray {
        val durationSeconds = 0.048
        val numSamples = (SAMPLE_RATE * durationSeconds).toInt()
        val buffer = ShortArray(numSamples)

        val startFreq = 420.0
        val endFreq = 780.0

        for (i in 0 until numSamples) {
            val progress = i.toDouble() / numSamples
            val currentFreq = startFreq + (endFreq - startFreq) * progress
            val envelope = (1.0 - progress) * (1.0 - exp(-progress * 25.0))

            val sampleValue = sin(2.0 * PI * currentFreq * (i.toDouble() / SAMPLE_RATE)) * envelope
            buffer[i] = (sampleValue * 22000.0).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return buffer
    }

    /**
     * Genera la onda de sonido de recepción (duración: ~120ms, dos armónicos suaves 660Hz -> 880Hz).
     */
    private fun generateReceivedChimeBuffer(): ShortArray {
        val durationSeconds = 0.120
        val numSamples = (SAMPLE_RATE * durationSeconds).toInt()
        val buffer = ShortArray(numSamples)

        val splitPoint = (numSamples * 0.40).toInt()

        for (i in 0 until numSamples) {
            val t = i.toDouble() / SAMPLE_RATE
            val sampleValue: Double

            if (i < splitPoint) {
                // Tono 1: Mi5 (659 Hz)
                val noteProgress = i.toDouble() / splitPoint
                val env = (1.0 - noteProgress * 0.7) * (1.0 - exp(-noteProgress * 30.0))
                sampleValue = sin(2.0 * PI * 659.25 * t) * env
            } else {
                // Tono 2: La5 (880 Hz)
                val noteProgress = (i - splitPoint).toDouble() / (numSamples - splitPoint)
                val env = exp(-noteProgress * 3.5) * (1.0 - exp(-noteProgress * 20.0))
                sampleValue = (sin(2.0 * PI * 880.0 * t) + 0.3 * sin(2.0 * PI * 1320.0 * t)) * env
            }

            buffer[i] = (sampleValue * 24000.0).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return buffer
    }
}
