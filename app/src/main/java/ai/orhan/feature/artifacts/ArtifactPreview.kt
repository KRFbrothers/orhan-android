package ai.orhan.feature.artifacts

import ai.orhan.data.artifacts.Artifact
import ai.orhan.data.artifacts.previewHtml
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import java.io.ByteArrayInputStream

@Composable
internal fun ArtifactPreview(artifact: Artifact) {
    val colors = MaterialTheme.colorScheme
    fun Color.css() = "#%06X".format(toArgb() and 0xFFFFFF)
    val html = remember(artifact, colors.background, colors.onBackground, colors.primary) {
        artifact.previewHtml(colors.background.css(), colors.onBackground.css(), colors.primary.css())
    }
    val textZoom = (LocalDensity.current.fontScale * 100).toInt()
    AndroidView(
        modifier = Modifier.fillMaxWidth().height(440.dp),
        factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = false
                settings.allowFileAccess = false
                settings.allowContentAccess = false
                settings.blockNetworkLoads = true
                settings.blockNetworkImage = true
                settings.domStorageEnabled = false
                @Suppress("DEPRECATION")
                settings.databaseEnabled = false
                settings.setSupportMultipleWindows(false)
                CookieManager.getInstance().setAcceptThirdPartyCookies(this, false)
                isSaveEnabled = false
                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?) = true
                    @Deprecated("Older Android callback")
                    override fun shouldOverrideUrlLoading(view: WebView?, url: String?) = true
                    override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?) =
                        WebResourceResponse("text/plain", "UTF-8", ByteArrayInputStream(ByteArray(0)))
                }
            }
        },
        update = { view ->
            view.settings.textZoom = textZoom
            view.setBackgroundColor(colors.background.toArgb())
            if (view.tag != html) {
                view.tag = html
                view.loadDataWithBaseURL("https://orhan-artifact.invalid/", html, "text/html", "UTF-8", null)
            }
        },
        onRelease = { it.stopLoading(); it.destroy() },
    )
}
