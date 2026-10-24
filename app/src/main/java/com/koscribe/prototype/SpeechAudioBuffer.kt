package com.koscribe.prototype

import java.util.Arrays

/** One bounded, process-only mono 16 kHz PCM capture. No file or editor context. */
internal class SpeechAudioBuffer {
    companion object {
        const val SAMPLE_RATE = 16_000
        const val MAX_SECONDS = 60
        const val MAX_PCM_BYTES = SAMPLE_RATE * 2 * MAX_SECONDS
        private const val HEADER_BYTES = 44
    }

    private val pcm = ByteArray(MAX_PCM_BYTES)
    private var size = 0
    private var closed = false

    @Synchronized fun append(frame: ByteArray, count: Int): Boolean {
        if (closed) return false
        if (count < 0 || count > frame.size || count > MAX_PCM_BYTES - size) {
            clear()
            return false
        }
        System.arraycopy(frame, 0, pcm, size, count)
        size += count
        return true
    }

    @Synchronized fun finishWav(): ByteArray? {
        if (closed || size < 2 || size % 2 != 0) { clear(); return null }
        closed = true
        val output = ByteArray(HEADER_BYTES + size)
        fun ascii(offset: Int, value: String) = value.toByteArray(Charsets.US_ASCII).copyInto(output, offset)
        fun le16(offset: Int, value: Int) {
            output[offset] = value.toByte()
            output[offset + 1] = (value ushr 8).toByte()
        }
        fun le32(offset: Int, value: Int) {
            output[offset] = value.toByte()
            output[offset + 1] = (value ushr 8).toByte()
            output[offset + 2] = (value ushr 16).toByte()
            output[offset + 3] = (value ushr 24).toByte()
        }
        ascii(0, "RIFF"); le32(4, 36 + size); ascii(8, "WAVE")
        ascii(12, "fmt "); le32(16, 16); le16(20, 1); le16(22, 1)
        le32(24, SAMPLE_RATE); le32(28, SAMPLE_RATE * 2); le16(32, 2); le16(34, 16)
        ascii(36, "data"); le32(40, size)
        System.arraycopy(pcm, 0, output, HEADER_BYTES, size)
        Arrays.fill(pcm, 0)
        size = 0
        return output
    }

    @Synchronized fun clear() {
        closed = true
        Arrays.fill(pcm, 0)
        size = 0
    }
}
