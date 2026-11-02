package ai.orhan.data.artifacts

import org.junit.Assert.*
import org.junit.Test

class ArtifactRenderingTest {
    @Test fun markdownRendersFormattingTablesAndEscapesEmbeddedHtml() {
        val source = "# Heading\n\n**Bold** and `code`\n\n| A | B |\n| - | - |\n| x | y |\n\n<script>alert(1)</script>"
        val html = markdownHtml(source)
        assertTrue(html.contains("<h1>Heading</h1>"))
        assertTrue(html.contains("<strong>Bold</strong>"))
        assertTrue(html.contains("<code>code</code>"))
        assertTrue(html.contains("<table>"))
        assertFalse(html.contains("<script>"))
    }

    @Test fun htmlPreviewRetainsLayoutButRemovesActiveContentAndPrivateSchemes() {
        val html = staticArtifactHtml("""<html><head><style>h1 { color: red }</style>
            <meta http-equiv="refresh" content="0;url=https://example.com"></head>
            <body onload="alert(1)"><main><h1>Page</h1><script>steal()</script>
            <iframe src="file:///data/data/ai.orhan/private"></iframe>
            <a href="javascript:steal()">bad</a><img src="content://private/item">
            <form action="https://example.com"><input name="private"></form></main></body></html>""")
        assertTrue(html.contains("<h1>Page</h1>"))
        assertTrue(html.contains("color: red"))
        for (forbidden in listOf("<script", "<iframe", "onload=", "javascript:", "content://", "file:///", "http-equiv", "<form", "<input")) {
            assertFalse(forbidden, html.contains(forbidden))
        }
    }

    @Test fun previewTextIsDerivedFromTypeWithoutChangingTheSavedSource() {
        val markdown = Artifact("m", "MD", "# Heading\n\n**Bold**", 1, ArtifactType.MARKDOWN)
        assertFalse(markdown.previewText().contains("**"))
        val html = Artifact("h", "HTML", "<h1>Heading</h1><script>private()</script>", 1, ArtifactType.HTML)
        assertEquals("Heading", html.previewText())
        assertTrue(html.content.contains("<script>"))
        assertEquals("# Literal", Artifact("t", "TXT", "# Literal", 1).previewText())
    }
}
