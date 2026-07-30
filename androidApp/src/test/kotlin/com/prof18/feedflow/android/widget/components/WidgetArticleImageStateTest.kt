package com.prof18.feedflow.android.widget.components

import com.prof18.feedflow.android.widget.WidgetImageRequestIdentity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WidgetArticleImageStateTest {

    @Test
    fun `budget key change removes value remembered for old full key`() {
        val oldKey = requestKey(payloadBudgetBytes = 4_096L)
        val currentKey = requestKey(payloadBudgetBytes = 2_048L)
        val oldState = WidgetArticleImageState(
            requestKey = oldKey,
            value = "old bitmap",
        )

        val currentState = oldState.forKey(currentKey)

        assertEquals(currentKey, currentState.requestKey)
        assertNull(currentState.valueFor(currentKey))
    }

    @Test
    fun `completion for stale full key is rejected`() {
        val staleKey = requestKey(payloadBudgetBytes = 4_096L)
        val currentKey = requestKey(payloadBudgetBytes = 2_048L)
        val currentState = WidgetArticleImageState<String>(requestKey = currentKey)

        val result = currentState.accept(
            completedKey = staleKey,
            currentKey = currentKey,
            value = "stale bitmap",
        )

        assertNull(result.valueFor(currentKey))
    }

    @Test
    fun `completion for current full key is accepted for rendering`() {
        val currentKey = requestKey(payloadBudgetBytes = 2_048L)
        val currentState = WidgetArticleImageState<String>(requestKey = currentKey)

        val result = currentState.accept(
            completedKey = currentKey,
            currentKey = currentKey,
            value = "current bitmap",
        )

        assertEquals("current bitmap", result.valueFor(currentKey))
    }

    private fun requestKey(payloadBudgetBytes: Long) = WidgetImageRequestIdentity(
        imageUrl = "https://example.com/article.png",
        edgePx = 50,
        exactSizeKey = "same-exact-sizes",
        payloadBudgetBytes = payloadBudgetBytes,
    )
}
