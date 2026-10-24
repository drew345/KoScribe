package com.koscribe.prototype

import org.junit.Assert.*
import org.junit.Test

class SpeechAudioBufferTest {
    @Test fun wavHasExactPcmAndLittleEndianHeader() {
        val source = byteArrayOf(1, 2, 3, 4)
        val buffer = SpeechAudioBuffer()
        assertTrue(buffer.append(source, source.size))
        source.fill(0) // capture must own its copy
        val wav = buffer.finishWav()!!
        assertEquals("RIFF", String(wav, 0, 4, Charsets.US_ASCII))
        assertEquals("WAVE", String(wav, 8, 4, Charsets.US_ASCII))
        assertEquals("data", String(wav, 36, 4, Charsets.US_ASCII))
        assertEquals(40, wav[4].toInt() and 0xff) // 36 + 4 PCM bytes
        assertEquals(4, wav[40].toInt() and 0xff)
        assertArrayEquals(byteArrayOf(1, 2, 3, 4), wav.copyOfRange(44, 48))
        assertFalse(buffer.append(byteArrayOf(5, 6), 2))
        assertNull(buffer.finishWav())
    }

    @Test fun sixtySecondCapacityIsAllowedButOverflowPoisonsTheCapture() {
        val buffer = SpeechAudioBuffer()
        val full = ByteArray(SpeechAudioBuffer.MAX_PCM_BYTES)
        assertTrue(buffer.append(full, full.size))
        assertEquals(44 + SpeechAudioBuffer.MAX_PCM_BYTES, buffer.finishWav()!!.size)

        val overflow = SpeechAudioBuffer()
        assertTrue(overflow.append(full, full.size))
        assertFalse(overflow.append(byteArrayOf(1, 2), 2))
        assertNull(overflow.finishWav())
        overflow.clear()
        assertFalse(overflow.append(byteArrayOf(1, 2), 2))
    }

    @Test fun discardedOrOddLengthAudioCannotUpload() {
        val discarded = SpeechAudioBuffer()
        assertTrue(discarded.append(byteArrayOf(1, 2), 2))
        discarded.clear()
        assertNull(discarded.finishWav())
        val odd = SpeechAudioBuffer()
        assertTrue(odd.append(byteArrayOf(1), 1))
        assertNull(odd.finishWav())
    }
}
