package com.marcelo.huba50

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Bundle
import android.os.Handler
import android.os.Looper
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
    private val handler = Handler(Looper.getMainLooper())
    @Volatile private var abriuOutroApp = false

    // Item 2: depois do Gemini, volta para a frente em 15 s
    private val voltarDepoisGemini = Runnable { trazerParaFrente() }

    // Item 2: em outro app, volta em 10 min (espera se um vídeo estiver tocando no próprio A50)
    private val voltarPorInatividade = object : Runnable {
        override fun run() {
            if (tocandoVideoLocal()) handler.postDelayed(this, 60_000) else trazerParaFrente()
        }
    }

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
                if (r.isForMainFrame) v.postDelayed({ v.reload() }, 30000)
            }
        }
        web.loadUrl(ENDERECO)
        esconderBarras()
    }

    override fun onStart() {
        super.onStart()
        abriuOutroApp = false
        handler.removeCallbacks(voltarPorInatividade)
    }

    override fun onStop() {
        super.onStop()
        handler.removeCallbacks(voltarDepoisGemini)
        handler.removeCallbacks(voltarPorInatividade)
        handler.postDelayed(voltarPorInatividade, 600_000)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            esconderBarras()
            handler.removeCallbacks(voltarDepoisGemini)
        } else if (!abriuOutroApp) {
            // Perdeu o foco sem abrir app pela tela: é o Gemini por cima
            web.evaluateJavascript("acordar()", null)
            handler.removeCallbacks(voltarDepoisGemini)
            handler.postDelayed(voltarDepoisGemini, 15_000)
        }
    }

    private fun trazerParaFrente() {
        try {
            startActivity(Intent(this, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT))
        } catch (e: Exception) { }
    }

    private fun tocandoVideoLocal(): Boolean = try {
        val msm = getSystemService(MEDIA_SESSION_SERVICE) as MediaSessionManager
        msm.getActiveSessions(ComponentName(this, TimerService::class.java)).any {
            it.packageName != "com.spotify.music" &&
                it.playbackState?.state == PlaybackState.STATE_PLAYING &&
                it.playbackInfo?.playbackType == MediaController.PlaybackInfo.PLAYBACK_TYPE_LOCAL
        }
    } catch (e: Exception) { false }

    @Suppress("DEPRECATION")
    private fun esconderBarras() {
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            or View.SYSTEM_UI_FLAG_FULLSCREEN
            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)
    }

    inner class Ponte {
        @JavascriptInterface
        fun abrirApp(pacote: String) {
            abriuOutroApp = true
            try {
                val i = packageManager.getLaunchIntentForPackage(pacote)
                if (i != null) startActivity(i) else abriuOutroApp = false
            } catch (e: Exception) { abriuOutroApp = false }
        }

        @JavascriptInterface
        fun abrirAssistente() {
            abriuOutroApp = false
            try {
                startActivity(Intent(Intent.ACTION_VOICE_COMMAND).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            } catch (e: Exception) {
                abrirApp("com.google.android.apps.bard")
            }
        }

        // Item 5: brilho da tela (-1 = normal do sistema)
        @JavascriptInterface
        fun brilho(valor: Double) {
            runOnUiThread {
                val lp = window.attributes
                lp.screenBrightness = if (valor < 0) WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE else valor.toFloat()
                window.attributes = lp
            }
        }

        // Item 1: lista de timers ativos
        @JavascriptInterface
        fun timers(): String = TimerService.atual
    }

    companion object {
        const val ENDERECO = "https://marcelomartins78.github.io/hub-a50/"
    }
}
