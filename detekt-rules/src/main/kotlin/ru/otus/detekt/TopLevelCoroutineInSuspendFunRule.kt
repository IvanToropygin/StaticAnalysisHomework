@file:OptIn(IDEAPluginsCompatibilityAPI::class)

package ru.otus.detekt

import io.gitlab.arturbosch.detekt.api.CodeSmell
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Entity
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Rule
import io.gitlab.arturbosch.detekt.api.Severity
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtLambdaArgument
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.typeUtil.supertypes
import org.jetbrains.kotlin.utils.IDEAPluginsCompatibilityAPI

class TopLevelCoroutineInSuspendFunRule(config: Config = Config.empty) : Rule(config) {

    override val issue: Issue = Issue(
        id = javaClass.simpleName,
        severity = Severity.CodeSmell,
        description = "Avoid running top level coroutines inside suspend functions",
        debt = Debt.FIVE_MINS
    )

    override fun visitCallExpression(expression: KtCallExpression) {
        super.visitCallExpression(expression)

        // Проверяем, что это вызов launch или async
        if (!isLaunchOrAsyncCall(expression)) return

        // Проверяем, что находимся внутри suspend функции
        if (!isInsideSuspendFunction(expression)) return

        // Проверяем, что это не scope-билдер
        if (isScopeBuilderCall(expression)) return

        // Проверяем тип получателя через type resolution
        if (isOnCoroutineScope(expression)) {
            report(
                CodeSmell(
                    issue,
                    Entity.from(expression),
                    message = "Запуск корутины внутри suspend функции. " +
                            "Используйте coroutineScope или supervisorScope."
                )
            )
        }
    }

    private fun isLaunchOrAsyncCall(expression: KtCallExpression): Boolean {
        val callee = expression.calleeExpression
        if (callee !is KtDotQualifiedExpression) return false

        val methodName = callee.selectorExpression?.text
        return methodName in coroutineFunctions
    }

    private fun isInsideSuspendFunction(element: KtElement): Boolean {
        var current: KtElement? = element
        while (current != null) {
            if (current is KtNamedFunction) {
                if (current.hasModifier(KtTokens.SUSPEND_KEYWORD)) {
                    return true
                }
            }
            current = current.parent as? KtElement
        }
        return false
    }

    private fun isScopeBuilderCall(expression: KtCallExpression): Boolean {
        // Проверяем, является ли вызов scope-билдером
        val callee = expression.calleeExpression
        if (callee is KtNameReferenceExpression) {
            return callee.text in scopeBuilders
        }

        // Проверяем, не находится ли вызов внутри scope-билдера
        var current: KtElement? = expression.parent as KtElement?
        while (current != null) {
            if (current is KtLambdaArgument) {
                val call = current.parent as? KtCallExpression
                if (call != null) {
                    val resolved = call.getResolvedCall(bindingContext)
                    val fqName = resolved?.resultingDescriptor?.fqNameSafe?.asString()
                    if (fqName == COROUTINE_SCOPE_BUILDER ||
                        fqName == SUPERVISOR_SCOPE_BUILDER) {
                        return true
                    }
                }
            }
            current = current.parent as? KtElement
        }
        return false
    }

    private fun isOnCoroutineScope(expression: KtCallExpression): Boolean {
        val bindingContext = bindingContext ?: return false

        val callee = expression.calleeExpression
        if (callee !is KtDotQualifiedExpression) return false

        val receiver = callee.receiverExpression
        val type = bindingContext.getType(receiver) ?: return false

        return isSubtypeOfCoroutineScope(type)
    }

    private fun isSubtypeOfCoroutineScope(type: KotlinType): Boolean {
        val typeName = type.constructor.declarationDescriptor?.fqNameSafe?.asString()
        if (typeName == COROUTINE_SCOPE_FQ_NAME) return true

        return type.supertypes().any {
            it.constructor.declarationDescriptor?.fqNameSafe?.asString() == COROUTINE_SCOPE_FQ_NAME
        }
    }

    companion object {
        private val scopeBuilders = setOf("coroutineScope", "supervisorScope")
        private val coroutineFunctions = setOf("launch", "async")
        private const val COROUTINE_SCOPE_FQ_NAME = "kotlinx.coroutines.CoroutineScope"
        private const val COROUTINE_SCOPE_BUILDER = "kotlinx.coroutines.coroutineScope"
        private const val SUPERVISOR_SCOPE_BUILDER = "kotlinx.coroutines.supervisorScope"
    }
}
