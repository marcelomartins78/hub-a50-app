package com.marcelo.huba50

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.webkit.JavascriptInterface
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient

class MainActivity : Activity() {

    private lateinit var web: WebView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        web = WebView(this)
        setContentView(web)
        web.settings.javaScriptEnabled = true
        web.settings.domStorageEnabled = true
        web.settings.cacheMode = WebSettings.LOAD_DEFAULT
        web.addJavascriptInterface(Ponte(), "Android")
        web.webViewClient = object : WebViewClient() {
            override fun onReceivedError(v: WebView, r: WebResourceRequest, e: WebResourceError) {
                // Sem internet: tenta de novo em 30 segundos
                if (r.isForMainFrame) v.postDelayed({ v.reload() }, 30000)
            }
        }
        web.loadUrl(ENDERECO)
        esconderBarras()
    }

    @Suppress("DEPRECATION")
    private fun esconderBarras() {
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            or View.SYSTEM_UI_FLAG_FULLSCREEN
            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) esconderBarras()
    }

    inner class Ponte {
        @JavascriptInterface
        fun abrirApp(pacote: String) {
            try {
                val i = packageManager.getLaunchIntentForPackage(pacote)
                if (i != null) startActivity(i)
            } catch (e: Exception) { }
        }

        @JavascriptInterface
        fun abrirAssistente() {
            try {
                startActivity(Intent(Intent.ACTION_VOICE_COMMAND).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            } catch (e: Exception) {
                abrirApp("com.google.android.apps.bard")
            }
        }
    }

    companion object {
        const val ENDERECO = "https://marcelomartins78.github.io/hub-a50/"
    }
}
