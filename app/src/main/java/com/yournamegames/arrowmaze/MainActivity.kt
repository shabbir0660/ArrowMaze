package com.yournamegames.arrowmaze

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Bundle
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.widget.LinearLayout
import com.google.android.gms.ads.*
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

// TEST ad unit IDs from Google. Replace with your real IDs before publishing.
const val BANNER_ID = "ca-app-pub-3940256099942544/6300978111"
const val REWARDED_ID = "ca-app-pub-3940256099942544/5224354917"

class MainActivity : Activity() {
    private lateinit var web: WebView
    private var rewarded: RewardedAd? = null

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MobileAds.initialize(this) {}
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        web = WebView(this).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.mediaPlaybackRequiresUserGesture = false
            addJavascriptInterface(Bridge(), "Android")
            loadUrl("file:///android_asset/index.html")
        }
        root.addView(web, LinearLayout.LayoutParams(-1, 0, 1f))
        val banner = AdView(this).apply {
            setAdSize(AdSize.BANNER)
            adUnitId = BANNER_ID
            loadAd(AdRequest.Builder().build())
        }
        root.addView(banner, LinearLayout.LayoutParams(-2, -2).apply { gravity = android.view.Gravity.CENTER_HORIZONTAL })
        setContentView(root)
        loadRewarded()
    }

    private fun loadRewarded() {
        RewardedAd.load(this, REWARDED_ID, AdRequest.Builder().build(), object : RewardedAdLoadCallback() {
            override fun onAdLoaded(ad: RewardedAd) { rewarded = ad }
            override fun onAdFailedToLoad(e: LoadAdError) { rewarded = null }
        })
    }

    private fun js(code: String) = runOnUiThread { web.evaluateJavascript(code, null) }

    inner class Bridge {
        @JavascriptInterface
        fun showRewarded() {
            runOnUiThread {
                val ad = rewarded
                if (ad == null) { js("window.onAdFail&&window.onAdFail()"); loadRewarded(); return@runOnUiThread }
                var earned = false
                ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                    override fun onAdDismissedFullScreenContent() {
                        rewarded = null; loadRewarded()
                        if (earned) js("window.onAdReward&&window.onAdReward()")
                    }
                }
                ad.show(this@MainActivity) { earned = true }
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() { if (web.canGoBack()) web.goBack() else super.onBackPressed() }
}
