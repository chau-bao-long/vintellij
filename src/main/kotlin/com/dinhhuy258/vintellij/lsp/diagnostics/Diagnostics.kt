package com.dinhhuy258.vintellij.lsp.diagnostics

import com.dinhhuy258.vintellij.comrade.buffer.SyncBuffer
import com.dinhhuy258.vintellij.lsp.utils.offsetToPosition
import com.intellij.codeInsight.daemon.impl.DaemonCodeAnalyzerEx
import com.intellij.codeInsight.daemon.impl.HighlightInfo
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.editor.Document
import org.eclipse.lsp4j.Diagnostic
import org.eclipse.lsp4j.DiagnosticSeverity
import org.eclipse.lsp4j.Range

fun getHighlights(buffer: SyncBuffer): List<HighlightInfo> {
    val highlights = mutableListOf<HighlightInfo>()
    DaemonCodeAnalyzerEx.processHighlights(
        buffer.document,
        buffer.project,
        HighlightSeverity.GENERIC_SERVER_ERROR_OR_WARNING,
        0,
        buffer.document.getLineEndOffset(buffer.document.lineCount - 1)
    ) { highlightInfo ->
        highlights.add(highlightInfo)
        true
    }
    highlights.filter {
        val document = buffer.document
        it.startOffset > 0 && it.startOffset <= document.textLength && it.endOffset > 0 && it.endOffset <= document.textLength
    }

    return highlights
}

fun HighlightInfo.toDiagnostic(document: Document): Diagnostic? {
    if (this.description == null) {
        return null
    }

    val description = this.description
    val start = offsetToPosition(document, this.getStartOffset())
    val end = offsetToPosition(document, this.getEndOffset())

    return Diagnostic(Range(start, end), description, this.diagnosticSeverity(), "vintellij")
}

private fun HighlightInfo.diagnosticSeverity() =
    when (this.severity) {
        HighlightSeverity.INFORMATION -> DiagnosticSeverity.Information
        HighlightSeverity.WARNING -> DiagnosticSeverity.Warning
        HighlightSeverity.ERROR -> DiagnosticSeverity.Error
        else -> DiagnosticSeverity.Hint
    }
