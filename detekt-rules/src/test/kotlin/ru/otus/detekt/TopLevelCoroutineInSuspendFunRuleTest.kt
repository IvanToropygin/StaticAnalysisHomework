package ru.otus.detekt

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.rules.KotlinCoreEnvironmentTest
import io.gitlab.arturbosch.detekt.test.compileAndLintWithContext
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

@KotlinCoreEnvironmentTest
internal class TopLevelCoroutineInSuspendFunRuleTest(private val env: KotlinCoreEnvironment) {
    private val rule = TopLevelCoroutineInSuspendFunRule(Config.empty)

    @Test
    fun `should detect launch on CoroutineScope inside suspend function`() {
        val code = """
            import kotlinx.coroutines.CoroutineScope
            import kotlinx.coroutines.Dispatchers
            import kotlinx.coroutines.launch

            val scope = CoroutineScope(Dispatchers.Default)

            suspend fun test() {
                scope.launch {
                    doWork()
                }
            }

            suspend fun doWork() {}
        """.trimIndent()

        val findings = rule.compileAndLintWithContext(env, code)
        assertEquals(1, findings.size)
        assertTrue(findings[0].message.contains("корутины"))
    }

    @Test
    fun `should detect async on CoroutineScope inside suspend function`() {
        val code = """
            import kotlinx.coroutines.CoroutineScope
            import kotlinx.coroutines.Dispatchers
            import kotlinx.coroutines.async

            val scope = CoroutineScope(Dispatchers.Default)

            suspend fun test() {
                val deferred = scope.async {
                    "Result"
                }
            }
        """.trimIndent()

        val findings = rule.compileAndLintWithContext(env, code)
        assertEquals(1, findings.size)
        assertTrue(findings[0].message.contains("корутины"))
    }

    @Test
    fun `should not report coroutineScope builder`() {
        val code = """
            import kotlinx.coroutines.coroutineScope
            import kotlinx.coroutines.launch

            suspend fun test() = coroutineScope {
                launch {
                    doWork()
                }
            }

            suspend fun doWork() {}
        """.trimIndent()

        val findings = rule.compileAndLintWithContext(env, code)
        assertEquals(0, findings.size)
    }

    @Test
    fun `should not report supervisorScope builder`() {
        val code = """
            import kotlinx.coroutines.supervisorScope
            import kotlinx.coroutines.launch

            suspend fun test() = supervisorScope {
                launch {
                    doWork()
                }
            }

            suspend fun doWork() {}
        """.trimIndent()

        val findings = rule.compileAndLintWithContext(env, code)
        assertEquals(0, findings.size)
    }

    @Test
    fun `should not report in non-suspend function`() {
        val code = """
            import kotlinx.coroutines.CoroutineScope
            import kotlinx.coroutines.Dispatchers
            import kotlinx.coroutines.launch

            val scope = CoroutineScope(Dispatchers.Default)

            fun test() {
                scope.launch {
                    doWork()
                }
            }

            fun doWork() {}
        """.trimIndent()

        val findings = rule.compileAndLintWithContext(env, code)
        assertEquals(0, findings.size)
    }

    @Test
    fun `should detect nested launches in suspend function`() {
        val code = """
            import kotlinx.coroutines.CoroutineScope
            import kotlinx.coroutines.Dispatchers
            import kotlinx.coroutines.launch

            val scope = CoroutineScope(Dispatchers.Default)

            suspend fun test() {
                scope.launch {
                    scope.launch {
                        doWork()
                    }
                }
            }

            suspend fun doWork() {}
        """.trimIndent()

        val findings = rule.compileAndLintWithContext(env, code)
        assertEquals(2, findings.size)
    }

    @Test
    fun `should detect GlobalScope inside suspend function`() {
        val code = """
            import kotlinx.coroutines.GlobalScope
            import kotlinx.coroutines.launch

            suspend fun test() {
                GlobalScope.launch {
                    doWork()
                }
            }

            suspend fun doWork() {}
        """.trimIndent()

        val findings = rule.compileAndLintWithContext(env, code)
        assertEquals(1, findings.size)
    }
}
