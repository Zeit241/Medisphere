package com.example.kursovaya.model.api

import com.example.kursovaya.BuildConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DoctorApiExtensionsTest {
    @Test
    fun toImageDataUri_returnsEmptyForNull() {
        assertEquals("", null.toImageDataUri())
    }

    @Test
    fun toImageDataUri_keepsHttpUrl() {
        val url = "https://example.com/photo.jpg"
        assertEquals(url, url.toImageDataUri())
    }

    @Test
    fun toImageDataUri_wrapsJpegBase64() {
        val base64 = "/9j/abc"
        assertTrue(base64.toImageDataUri().startsWith("data:image/jpeg;base64,"))
    }

    @Test
    fun toImageDataUri_keepsDataUri() {
        val data = "data:image/png;base64,abc"
        assertEquals(data, data.toImageDataUri())
    }

    @Test
    fun rewriteLoopbackForEmulatorIfNeeded_rewrites127Address() {
        val url = "http://127.0.0.1:8055/assets/x"
        val expected =
            if (BuildConfig.REWRITE_LOOPBACK_IN_IMAGE_URLS) {
                "http://${BuildConfig.LOOPBACK_REPLACEMENT_HOST}:8055/assets/x"
            } else {
                url
            }
        assertEquals(expected, url.rewriteLoopbackForEmulatorIfNeeded())
    }

    @Test
    fun appendDirectusAssetAccessTokenIfConfigured_skipsWhenTokenBlank() {
        val url = "http://example.com/photo.jpg"
        assertEquals(url, url.appendDirectusAssetAccessTokenIfConfigured())
    }

    @Test
    fun toImageDataUri_wrapsUnknownStringAsBase64() {
        val value = "random-data"
        assertTrue(value.toImageDataUri().startsWith("data:image/jpeg;base64,random-data"))
    }

    @Test
    fun rewriteLoopbackForEmulatorIfNeeded_respectsBuildConfig() {
        val url = "http://localhost:8055/assets/x"
        val expected =
            if (BuildConfig.REWRITE_LOOPBACK_IN_IMAGE_URLS) {
                "http://${BuildConfig.LOOPBACK_REPLACEMENT_HOST}:8055/assets/x"
            } else {
                url
            }
        assertEquals(expected, url.rewriteLoopbackForEmulatorIfNeeded())
    }
}
