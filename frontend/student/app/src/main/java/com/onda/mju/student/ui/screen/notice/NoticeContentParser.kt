package com.onda.mju.student.ui.screen.notice

/**
 * 관리자 웹이 content 에 넣는 첨부 HTML / 마커를 본문과 파일 목록으로 분리한다.
 *
 * 예)
 * `본문<div class="onda-notice-attach"><a href="https://.../file.png">file.png</a></div>`
 */
object NoticeContentParser {

    private val attachBlockRegex = Regex(
        """<div\s+[^>]*class\s*=\s*["'][^"']*onda-notice-attach[^"']*["'][^>]*>\s*<a\s+[^>]*href\s*=\s*["']([^"']+)["'][^>]*>(.*?)</a>\s*</div>""",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
    )

    private val storageAnchorRegex = Regex(
        """<a\s+[^>]*href\s*=\s*["'](https?://[^"']*notice-attachments[^"']+)["'][^>]*>(.*?)</a>""",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
    )

    private val bracketAttachRegex = Regex("""\[첨부:\s*([^\]]+)]""")

    private val tagRegex = Regex("""<[^>]+>""")

    data class Parsed(
        val body: String,
        val attachments: List<NoticeAttachment>,
    )

    fun parse(raw: String): Parsed {
        if (raw.isBlank()) return Parsed(body = "", attachments = emptyList())

        val attachments = linkedMapOf<String, NoticeAttachment>()
        var working = raw

        attachBlockRegex.findAll(raw).forEach { match ->
            val url = decodeHtml(match.groupValues[1].trim())
            val name = cleanName(match.groupValues[2])
            if (url.isNotBlank()) {
                attachments[url] = NoticeAttachment(
                    name = name.ifBlank { fileNameFromUrl(url) },
                    url = url,
                    meta = extensionMeta(name.ifBlank { url }),
                )
            }
            working = working.replace(match.value, "\n")
        }

        storageAnchorRegex.findAll(working).forEach { match ->
            val url = decodeHtml(match.groupValues[1].trim())
            val name = cleanName(match.groupValues[2])
            if (url.isNotBlank() && !attachments.containsKey(url)) {
                attachments[url] = NoticeAttachment(
                    name = name.ifBlank { fileNameFromUrl(url) },
                    url = url,
                    meta = extensionMeta(name.ifBlank { url }),
                )
            }
            working = working.replace(match.value, "\n")
        }

        bracketAttachRegex.findAll(working).forEach { match ->
            val name = match.groupValues[1].trim()
            if (name.isNotBlank()) {
                val key = "name:$name"
                if (attachments.keys.none { it == key || attachments[it]?.name == name }) {
                    attachments[key] = NoticeAttachment(
                        name = name,
                        url = null,
                        meta = extensionMeta(name),
                    )
                }
            }
            working = working.replace(match.value, "\n")
        }

        val body = stripHtml(working)
            .replace(Regex("""[ \t]+\n"""), "\n")
            .replace(Regex("""\n{3,}"""), "\n\n")
            .trim()

        return Parsed(body = body, attachments = attachments.values.toList())
    }

    private fun cleanName(raw: String): String =
        stripHtml(raw).trim().ifBlank { "첨부파일" }

    private fun stripHtml(raw: String): String =
        decodeHtml(tagRegex.replace(raw, ""))

    private fun decodeHtml(raw: String): String =
        raw
            .replace("&nbsp;", " ", ignoreCase = true)
            .replace("&amp;", "&", ignoreCase = true)
            .replace("&lt;", "<", ignoreCase = true)
            .replace("&gt;", ">", ignoreCase = true)
            .replace("&quot;", "\"", ignoreCase = true)
            .replace("&#39;", "'")
            .replace("&apos;", "'", ignoreCase = true)

    private fun fileNameFromUrl(url: String): String {
        val last = url.substringAfterLast('/').substringBefore('?')
        return decodeHtml(last).ifBlank { "첨부파일" }
    }

    private fun extensionMeta(nameOrUrl: String): String {
        val ext = nameOrUrl
            .substringAfterLast('.', missingDelimiterValue = "")
            .substringBefore('?')
            .lowercase()
            .takeIf { it.length in 1..5 && it.all { ch -> ch.isLetterOrDigit() } }
        return when (ext) {
            null, "" -> "첨부파일"
            "pdf" -> "PDF 파일"
            "png", "jpg", "jpeg", "gif", "webp", "heic" -> "이미지 파일 · ${ext.uppercase()}"
            "doc", "docx" -> "문서 파일"
            "xls", "xlsx" -> "스프레드시트"
            "ppt", "pptx" -> "발표 자료"
            "zip", "rar", "7z" -> "압축 파일"
            else -> "${ext.uppercase()} 파일"
        }
    }
}
