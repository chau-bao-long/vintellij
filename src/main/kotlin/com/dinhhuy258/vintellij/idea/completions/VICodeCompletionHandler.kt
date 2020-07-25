package com.dinhhuy258.vintellij.idea.completions

import com.intellij.codeInsight.completion.CodeCompletionHandlerBase
import com.intellij.codeInsight.completion.CompletionPhase.ItemsCalculated
import com.intellij.codeInsight.completion.CompletionProgressIndicator
import com.intellij.codeInsight.completion.CompletionType
import com.intellij.codeInsight.completion.impl.CompletionServiceImpl
import com.intellij.codeInsight.lookup.LookupElementPresentation
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiKeyword
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiVariable

class VICodeCompletionHandler(private val onSuggest: (word: String, kind: CompletionKind, menu: String) -> Unit) : CodeCompletionHandlerBase(CompletionType.BASIC) {
    @Suppress("UnstableApiUsage")
    override fun completionFinished(indicator: CompletionProgressIndicator?, hasModifiers: Boolean) {
        if (indicator == null) {
            return
        }
        CompletionServiceImpl.setCompletionPhase(ItemsCalculated(indicator))
        val lookup = indicator.lookup
        for (item in lookup.items) {
            val psiElement = item.psiElement ?: continue
            var kind = CompletionKind.UNKNOWN
            var menu = ""
            val presentation = LookupElementPresentation()
            item.renderElement(presentation)
            var word = item.lookupString.substring(lookup.getPrefixLength(item))
            when (psiElement) {
                is PsiMethod -> {
                    word += '('
                    if (psiElement.parameterList.parametersCount == 0) {
                        word += ")"
                    }
                    menu = presentation.typeText + " " + psiElement.name + presentation.tailText
                    kind = CompletionKind.FUNCTION
                }
                is PsiKeyword -> {
                    kind = CompletionKind.KEYWORD
                }
                is PsiClass -> {
                    menu = presentation.tailText ?: ""
                    kind = CompletionKind.TYPE
                }
                is PsiVariable -> {
                    menu = presentation.typeText ?: ""
                    kind = CompletionKind.VARIABLE
                }
                else -> {
                    menu = psiElement.javaClass.simpleName
                }
            }

            onSuggest(word, kind, menu)
        }
    }
}
