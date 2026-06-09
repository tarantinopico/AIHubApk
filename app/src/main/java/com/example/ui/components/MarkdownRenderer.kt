package com.example.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

@Composable
fun MarkdownRenderer(
    text: String,
    modifier: Modifier = Modifier,
    textColor: Color = TextPrimary,
    isUser: Boolean = false
) {
    val blocks = remember(text) { parseMarkdownBlocks(text) }
    
    SelectionContainer(modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            blocks.forEach { block ->
                when (block) {
                    is MarkdownBlock.Paragraph -> {
                        Text(
                            text = parseInlineMarkdown(block.content, textColor, isUser),
                            style = AppTypography.bodyLarge,
                            color = textColor
                        )
                    }
                    is MarkdownBlock.Code -> {
                        CodeBlock(block = block, isUser = isUser)
                    }
                    is MarkdownBlock.Heading -> {
                        val style = when(block.level) {
                            1 -> AppTypography.headlineMedium
                            2 -> AppTypography.titleLarge
                            3 -> AppTypography.titleMedium
                            else -> AppTypography.titleSmall
                        }
                        Text(
                            text = parseInlineMarkdown(block.content, textColor, isUser),
                            style = style,
                            color = textColor,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    is MarkdownBlock.ListBlock -> {
                        ListRenderer(block = block, textColor = textColor, isUser = isUser)
                    }
                    is MarkdownBlock.Quote -> {
                        Box(
                            modifier = Modifier
                                .padding(vertical = 4.dp)
                                .drawBehind {
                                    drawLine(
                                        color = GradientMiddle,
                                        start = Offset(0f, 0f),
                                        end = Offset(0f, size.height),
                                        strokeWidth = 4.dp.toPx()
                                    )
                                }
                                .padding(start = 16.dp)
                        ) {
                            Text(
                                text = parseInlineMarkdown(block.content, textColor.copy(alpha = 0.8f), isUser),
                                style = AppTypography.bodyLarge,
                                fontStyle = FontStyle.Italic
                            )
                        }
                    }
                    is MarkdownBlock.Divider -> {
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 8.dp),
                            color = if (isUser) Color.White.copy(alpha = 0.3f) else SurfaceElevated
                        )
                    }
                    is MarkdownBlock.Table -> {
                        TableRenderer(block = block, textColor = textColor)
                    }
                }
            }
        }
    }
}

sealed class MarkdownBlock {
    data class Paragraph(val content: String) : MarkdownBlock()
    data class Code(val language: String, val code: String, val isComplete: Boolean) : MarkdownBlock()
    data class Heading(val level: Int, val content: String) : MarkdownBlock()
    data class ListBlock(val items: List<String>, val isOrdered: Boolean) : MarkdownBlock()
    data class Quote(val content: String) : MarkdownBlock()
    data object Divider : MarkdownBlock()
    data class Table(val headers: List<String>, val rows: List<List<String>>) : MarkdownBlock()
}

// Very robust block parser that handles streaming chunks correctly
fun parseMarkdownBlocks(text: String): List<MarkdownBlock> {
    val blocks = mutableListOf<MarkdownBlock>()
    val lines = text.split("\n")
    
    var inCodeBlock = false
    var codeLang = ""
    var codeContent = StringBuilder()
    
    var currentParagraph = StringBuilder()
    var currentList = mutableListOf<String>()
    var currentListIsOrdered = false
    
    fun flushParagraph() {
        if (currentParagraph.isNotEmpty()) {
            blocks.add(MarkdownBlock.Paragraph(currentParagraph.toString().trimEnd()))
            currentParagraph.clear()
        }
    }
    
    fun flushList() {
        if (currentList.isNotEmpty()) {
            blocks.add(MarkdownBlock.ListBlock(currentList.toList(), currentListIsOrdered))
            currentList.clear()
        }
    }
    
    var i = 0
    while (i < lines.size) {
        val line = lines[i]
        
        if (inCodeBlock) {
            if (line.trim().startsWith("```")) {
                inCodeBlock = false
                blocks.add(MarkdownBlock.Code(codeLang, codeContent.toString(), isComplete = true))
                codeContent.clear()
            } else {
                if (codeContent.isNotEmpty()) codeContent.append("\n")
                codeContent.append(line)
            }
            i++
            continue
        }
        
        if (line.trim().startsWith("```")) {
            flushParagraph()
            flushList()
            inCodeBlock = true
            codeLang = line.trim().removePrefix("```").trim()
            i++
            continue
        }
        
        if (line.matches(Regex("^#{1,6}\\s+.*"))) {
            flushParagraph()
            flushList()
            val level = line.takeWhile { it == '#' }.length
            blocks.add(MarkdownBlock.Heading(level, line.substring(level).trim()))
            i++
            continue
        }
        
        if (line.trim() == "---" || line.trim() == "***") {
            flushParagraph()
            flushList()
            blocks.add(MarkdownBlock.Divider)
            i++
            continue
        }
        
        if (line.trim().startsWith("> ")) {
            flushParagraph()
            flushList()
            blocks.add(MarkdownBlock.Quote(line.substringAfter("> ").trim()))
            i++
            continue
        }
        
        val listMatch = Regex("^(\\s*)([-*]|\\d+\\.)\\s+(.*)").find(line)
        if (listMatch != null) {
            flushParagraph()
            val isOrdered = listMatch.groupValues[2].matches(Regex("\\d+\\."))
            if (currentList.isNotEmpty() && currentListIsOrdered != isOrdered) {
                flushList()
            }
            currentListIsOrdered = isOrdered
            currentList.add(listMatch.groupValues[3])
            i++
            continue
        } else {
            flushList()
        }
        
        if (line.trim().startsWith("|") && line.trim().endsWith("|") && i + 1 < lines.size && lines[i+1].trim().matches(Regex("^\\|[-:| ]+\\|.*"))) {
            flushParagraph()
            val headers = line.split("|").map { it.trim() }.filter { it.isNotEmpty() }
            val rows = mutableListOf<List<String>>()
            i += 2 // Skip header and separator
            while (i < lines.size && lines[i].trim().startsWith("|") && lines[i].trim().endsWith("|")) {
                rows.add(lines[i].split("|").map { it.trim() }.filter { it.isNotEmpty() })
                i++
            }
            blocks.add(MarkdownBlock.Table(headers, rows))
            continue
        }
        
        if (line.trim().isNotEmpty()) {
            if (currentParagraph.isNotEmpty()) currentParagraph.append("\n")
            currentParagraph.append(line)
        } else {
            flushParagraph()
        }
        i++
    }
    
    flushParagraph()
    flushList()
    
    if (inCodeBlock) {
        blocks.add(MarkdownBlock.Code(codeLang, codeContent.toString(), isComplete = false))
    }
    
    return blocks
}

@Composable
fun parseInlineMarkdown(text: String, textColor: Color, isUser: Boolean = false): AnnotatedString {
    return buildAnnotatedString {
        var cursor = 0
        while (cursor < text.length) {
            when {
                // Bold: **text**
                text.startsWith("**", cursor) -> {
                    val endToken = text.indexOf("**", cursor + 2)
                    if (endToken != -1) {
                        pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                        append(text.substring(cursor + 2, endToken))
                        pop()
                        cursor = endToken + 2
                    } else {
                        append("**")
                        cursor += 2
                    }
                }
                // Italic: *text*
                text.startsWith("*", cursor) && (cursor + 1 >= text.length || text[cursor + 1] != '*') -> {
                    val endToken = text.indexOf("*", cursor + 1)
                    if (endToken != -1) {
                        pushStyle(SpanStyle(fontStyle = FontStyle.Italic))
                        append(text.substring(cursor + 1, endToken))
                        pop()
                        cursor = endToken + 1
                    } else {
                        append("*")
                        cursor += 1
                    }
                }
                // Strikethrough: ~~text~~
                text.startsWith("~~", cursor) -> {
                    val endToken = text.indexOf("~~", cursor + 2)
                    if (endToken != -1) {
                        pushStyle(SpanStyle(textDecoration = TextDecoration.LineThrough))
                        append(text.substring(cursor + 2, endToken))
                        pop()
                        cursor = endToken + 2
                    } else {
                        append("~~")
                        cursor += 2
                    }
                }
                // Inline Code: `text`
                text.startsWith("`", cursor) && (cursor + 1 >= text.length || text[cursor + 1] != '`') -> {
                    val endToken = text.indexOf("`", cursor + 1)
                    if (endToken != -1) {
                        pushStyle(
                            SpanStyle(
                                fontFamily = FontFamily.Monospace,
                                background = if (isUser) Color.White.copy(alpha = 0.2f) else SurfaceElevated,
                                fontSize = 14.sp
                            )
                        )
                        append(" ${text.substring(cursor + 1, endToken)} ")
                        pop()
                        cursor = endToken + 1
                    } else {
                        append("`")
                        cursor += 1
                    }
                }
                // Links: [Text](url)
                text.startsWith("[", cursor) -> {
                    val textEnd = text.indexOf("](", cursor)
                    val urlEnd = if (textEnd != -1) text.indexOf(")", textEnd) else -1
                    if (textEnd != -1 && urlEnd != -1) {
                        val linkText = text.substring(cursor + 1, textEnd)
                        val url = text.substring(textEnd + 2, urlEnd)
                        pushStringAnnotation(tag = "URL", annotation = url)
                        pushStyle(SpanStyle(color = GradientMiddle, textDecoration = TextDecoration.Underline, fontWeight = FontWeight.Bold))
                        append(linkText)
                        pop()
                        pop()
                        cursor = urlEnd + 1
                    } else {
                        append(text[cursor].toString())
                        cursor += 1
                    }
                }
                else -> {
                    append(text[cursor].toString())
                    cursor += 1
                }
            }
        }
    }
}

@Composable
fun CodeBlock(block: MarkdownBlock.Code, isUser: Boolean = false) {
    val clipboardManager = LocalClipboardManager.current
    val hapticFeedback = LocalHapticFeedback.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF1E1E1E))
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF333333))
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = block.language.ifEmpty { "text" },
                color = Color.LightGray,
                style = AppTypography.labelSmall,
                fontFamily = FontFamily.Monospace
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .clickable {
                        clipboardManager.setText(AnnotatedString(block.code))
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    }
                    .padding(4.dp)
            ) {
                Icon(
                    Icons.Default.ContentCopy,
                    contentDescription = "Zkopírovat",
                    tint = Color.LightGray,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text("Kopírovat", color = Color.LightGray, style = AppTypography.labelSmall)
            }
        }
        
        // Code content
        Box(modifier = Modifier.horizontalScroll(rememberScrollState())) {
            Text(
                text = highlightSyntax(block.code, block.language),
                modifier = Modifier.padding(16.dp),
                color = Color(0xFFD4D4D4),
                fontFamily = FontFamily.Monospace,
                fontSize = 14.sp,
                lineHeight = 20.sp
            )
        }
        
        if (!block.isComplete) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = GradientMiddle, strokeWidth = 2.dp)
            }
        }
    }
}

// Simple regex-based syntax highlighter for common keywords
fun highlightSyntax(code: String, lang: String): AnnotatedString {
    return buildAnnotatedString {
        append(code)
        
        val keywords = listOf("val", "var", "fun", "class", "interface", "import", "package", "if", "else", "for", "while", "return", "true", "false", "null", "const", "let")
        val keywordRegex = Regex("\\b(${keywords.joinToString("|")})\\b")
        val stringRegex = Regex("\".*?\"|'.*?'")
        val numberRegex = Regex("\\b\\d+\\b")
        val commentRegex = Regex("//.*|/\\*[\\s\\S]*?\\*/")
        
        // Very basic span application
        try {
            stringRegex.findAll(code).forEach { match ->
                addStyle(SpanStyle(color = Color(0xFFCE9178)), match.range.first, match.range.last + 1)
            }
            commentRegex.findAll(code).forEach { match ->
                addStyle(SpanStyle(color = Color(0xFF6A9955), fontStyle = FontStyle.Italic), match.range.first, match.range.last + 1)
            }
            numberRegex.findAll(code).forEach { match ->
                addStyle(SpanStyle(color = Color(0xFFB5CEA8)), match.range.first, match.range.last + 1)
            }
            keywordRegex.findAll(code).forEach { match ->
                addStyle(SpanStyle(color = Color(0xFF569CD6), fontWeight = FontWeight.Bold), match.range.first, match.range.last + 1)
            }
        } catch (e: Exception) {
            // Ignore highlighting if it gets too complex or breaks
        }
    }
}

@Composable
fun ListRenderer(block: MarkdownBlock.ListBlock, textColor: Color, isUser: Boolean = false) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        block.items.forEachIndexed { index, item ->
            Row(verticalAlignment = Alignment.Top) {
                Text(
                    text = if (block.isOrdered) "${index + 1}. " else "• ",
                    modifier = Modifier.padding(end = 8.dp),
                    style = AppTypography.bodyLarge,
                    color = textColor,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = parseInlineMarkdown(item, textColor, isUser),
                    style = AppTypography.bodyLarge,
                    color = textColor
                )
            }
        }
    }
}

@Composable
fun TableRenderer(block: MarkdownBlock.Table, textColor: Color) {
    Box(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
        Column(modifier = Modifier.background(SurfaceElevated, RoundedCornerShape(8.dp)).padding(1.dp)) {
            // Headers
            Row(modifier = Modifier.background(Color.Black.copy(alpha = 0.2f))) {
                block.headers.forEach { header ->
                    Text(
                        text = header,
                        modifier = Modifier.padding(8.dp).widthIn(min = 100.dp),
                        style = AppTypography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )
                }
            }
            HorizontalDivider(color = Surface)
            // Rows
            block.rows.forEach { row ->
                Row {
                    row.forEachIndexed { i, cell ->
                        Text(
                            text = cell,
                            modifier = Modifier.padding(8.dp).widthIn(min = 100.dp),
                            style = AppTypography.bodyMedium,
                            color = textColor
                        )
                    }
                }
                HorizontalDivider(color = SurfaceElevated.copy(alpha = 0.5f))
            }
        }
    }
}
