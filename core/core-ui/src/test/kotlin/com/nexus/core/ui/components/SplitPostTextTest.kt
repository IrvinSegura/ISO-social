package com.nexus.core.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test

class SplitPostTextTest {

    @Test
    fun `the first line is the title when more text follows`() {
        assertEquals(
            PostText("Mis primeros pasos", "Así fue escribir mi primera publicación."),
            splitPostText("Mis primeros pasos\n\nAsí fue escribir mi primera publicación."),
        )
    }

    @Test
    fun `a short text is all title`() {
        assertEquals(PostText("Prueba privada del círculo", null), splitPostText("  Prueba privada del círculo  "))
    }

    @Test
    fun `a long paragraph gives its first sentence as the title`() {
        val text = "Hoy fue un gran día. " + "Caminamos por el centro y probamos comida nueva. ".repeat(3)
        val split = splitPostText(text)
        assertEquals("Hoy fue un gran día.", split.title)
        assertEquals(text.substringAfter("día. ").trim(), split.body)
    }

    @Test
    fun `a long text without a short first sentence has no title`() {
        val text = "palabra ".repeat(30).trim()
        assertEquals(PostText(null, text), splitPostText(text))
    }

    @Test
    fun `a long first line is not used as the title`() {
        val first = "x".repeat(120)
        assertEquals(PostText(null, "$first\nresto"), splitPostText("$first\nresto"))
    }
}
