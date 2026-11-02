package ai.orhan.data.artifacts

import org.commonmark.ext.gfm.tables.TablesExtension
import org.commonmark.parser.Parser
import org.commonmark.renderer.html.HtmlRenderer
import org.commonmark.renderer.text.TextContentRenderer
import org.jsoup.Jsoup
import org.jsoup.safety.Cleaner
import org.jsoup.safety.Safelist

private val extensions = listOf(TablesExtension.create())
private val markdownParser = Parser.builder().extensions(extensions).maxOpenBlockParsers(64).build()
private val markdownRenderer = HtmlRenderer.builder().extensions(extensions)
    .escapeHtml(true).sanitizeUrls(true).build()
private val textRenderer = TextContentRenderer.builder().extensions(extensions).build()

fun markdownHtml(source: String): String = markdownRenderer.render(markdownParser.parse(source))

/** Local static markup only. WebView policy additionally blocks navigation, network and scripting. */
fun staticArtifactHtml(source: String): String {
    val parsed = Jsoup.parse(source)
    parsed.select("script,iframe,object,embed,form,input,button,textarea,select,link,meta,base").remove()
    // Keep document CSS for static layout; the preview CSP forbids all external CSS/resources.
    parsed.head().select("style").toList().forEach { parsed.body().prependChild(it) }
    val allowed = Safelist.relaxed()
        .addTags("style", "main", "section", "article", "header", "footer", "nav", "div", "span")
        .addAttributes(":all", "style", "class", "id")
        .removeAttributes("img", "src")
    return Cleaner(allowed).clean(parsed).body().html()
}

/** A labelled text excerpt for list cards; the saved source/type are unchanged. */
fun Artifact.previewText(): String = when (type) {
    ArtifactType.PLAIN_TEXT -> content.take(2_000)
    ArtifactType.MARKDOWN -> textRenderer.render(markdownParser.parse(content.take(4_000))).take(2_000)
    ArtifactType.HTML -> Jsoup.parse(staticArtifactHtml(content.take(4_000))).body().text().take(2_000)
}

fun Artifact.previewHtml(background: String, foreground: String, accent: String): String {
    val body = when (type) {
        ArtifactType.MARKDOWN -> markdownHtml(content)
        ArtifactType.HTML -> staticArtifactHtml(content)
        ArtifactType.PLAIN_TEXT -> error("Plain text uses the native detail view")
    }
    return """<!doctype html><html><head><meta charset="utf-8">
        <meta name="viewport" content="width=device-width,initial-scale=1">
        <meta http-equiv="Content-Security-Policy" content="default-src 'none'; style-src 'unsafe-inline'; img-src 'none'; form-action 'none'; base-uri 'none'">
        <style>html{background:$background;color:$foreground;color-scheme:light dark}
        body{font:1rem/1.6 sans-serif;margin:12px;overflow-wrap:anywhere}
        a{color:$accent} pre{white-space:pre-wrap} code{font-family:monospace}
        table{border-collapse:collapse;display:block;overflow-x:auto;max-width:100%}
        th,td{border:1px solid currentColor;padding:6px} img{max-width:100%}</style>
        </head><body>$body</body></html>""".trimIndent()
}
