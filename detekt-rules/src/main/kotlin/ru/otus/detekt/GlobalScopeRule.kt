package ru.otus.detekt

import io.gitlab.arturbosch.detekt.api.CodeSmell
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Entity
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Rule
import io.gitlab.arturbosch.detekt.api.Severity
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression

class GlobalScopeRule(config: Config) : Rule(config) {

    override val issue: Issue = Issue(
        id = javaClass.simpleName,
        severity = Severity.CodeSmell,
        description = "Avoid using GlobalScope",
        debt = Debt.FIVE_MINS
    )

    override fun visitCallExpression(expression: KtCallExpression) {
        super.visitCallExpression(expression)

        // Получаем выражение вызова
        val calleeExpression = expression.calleeExpression
        if (calleeExpression !is KtDotQualifiedExpression) return

        // Проверяем имя метода
        val methodName = calleeExpression.selectorExpression?.text
        if (methodName !in listOf("launch", "async")) return

        // Проверяем получатель
        val receiver = calleeExpression.receiverExpression
        if (receiver.text == "GlobalScope") {
            report(
                CodeSmell(
                    issue,
                    Entity.from(expression),
                    message = "Использование GlobalScope.${methodName}() обнаружено. " +
                            "GlobalScope может привести к утечкам памяти."
                )
            )
        }
    }
}
