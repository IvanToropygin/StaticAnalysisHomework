package ru.otus.detekt

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.test.lint
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class GlobalScopeRuleTest {

    private val rule = GlobalScopeRule(Config.empty)

    @Test
    fun `should detect GlobalScope launch`() {
        val code = """
            import kotlinx.coroutines.GlobalScope
            import kotlinx.coroutines.launch

            fun test() {
                GlobalScope.launch {
                    println("Hello")
                }
            }
        """.trimIndent()

        val findings = rule.lint(code)
        assertEquals(1, findings.size)
        assertTrue(findings[0].message.contains("GlobalScope.launch"))
    }

    @Test
    fun `should detect GlobalScope async`() {
        val code = """
            import kotlinx.coroutines.GlobalScope
            import kotlinx.coroutines.async

            suspend fun test() {
                val deferred = GlobalScope.async {
                    "Result"
                }
            }
        """.trimIndent()

        val findings = rule.lint(code)
        assertEquals(1, findings.size)
        assertTrue(findings[0].message.contains("GlobalScope.async"))
    }

    @Test
    fun `should not detect custom scope launch`() {
        val code = """
            import kotlinx.coroutines.CoroutineScope
            import kotlinx.coroutines.Dispatchers
            import kotlinx.coroutines.launch

            val scope = CoroutineScope(Dispatchers.Default)

            fun test() {
                scope.launch {
                    println("Hello")
                }
            }
        """.trimIndent()

        val findings = rule.lint(code)
        assertEquals(0, findings.size)
    }

    @Test
    fun `should not detect non-coroutine launch`() {
        val code = """
            class MyClass {
                fun launch() {
                    println("Not coroutine")
                }

                fun test() {
                    launch()
                }
            }
        """.trimIndent()

        val findings = rule.lint(code)
        assertEquals(0, findings.size)
    }
}
