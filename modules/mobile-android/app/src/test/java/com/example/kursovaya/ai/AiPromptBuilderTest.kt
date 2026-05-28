package com.example.kursovaya.ai

import com.example.kursovaya.model.api.AiCatalogResponse
import com.example.kursovaya.model.api.AiDoctorRef
import com.example.kursovaya.model.api.AiServiceRef
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [28])
class AiPromptBuilderTest {
    @Test
    fun buildSystemPrompt_replacesDoctorAndServicePlaceholders() {
        val context = RuntimeEnvironment.getApplication()
        val catalog =
            AiCatalogResponse(
                doctors = listOf(AiDoctorRef(1L, "Dr A", "Therapy")),
                services = listOf(AiServiceRef(2L, "Consultation")),
            )

        val prompt = AiPromptBuilder.buildSystemPrompt(context, catalog)

        assertTrue(prompt.contains("Dr A"))
        assertTrue(prompt.contains("Consultation"))
        assertFalse(prompt.contains("{{СПИСОК_ВРАЧЕЙ}}"))
        assertFalse(prompt.contains("{{СПИСОК_УСЛУГ}}"))
    }

    @Test
    fun buildSystemPrompt_handlesEmptyCatalog() {
        val context = RuntimeEnvironment.getApplication()
        val prompt = AiPromptBuilder.buildSystemPrompt(context, AiCatalogResponse())

        assertTrue(prompt.contains("[]"))
    }
}
