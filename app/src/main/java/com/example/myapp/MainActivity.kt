package com.example.myapp

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.webkit.GeolocationPermissions
import android.webkit.PermissionRequest
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : Activity() {

    private lateinit var webView: WebView

    private var pendingCameraRequest: PermissionRequest? = null
    private var pendingLocationCallback: GeolocationPermissions.Callback? = null
    private var pendingLocationOrigin: String? = null

    companion object {
        private const val REQUEST_CAMERA = 1001
        private const val REQUEST_LOCATION = 1002
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        webView = WebView(this)

        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            setGeolocationEnabled(true)
            mediaPlaybackRequiresUserGesture = false
        }

        /*
         * WebView内のリンク処理
         */
        webView.webViewClient = object : WebViewClient() {

            override fun shouldOverrideUrlLoading(
                view: WebView,
                request: WebResourceRequest
            ): Boolean {

                val url = request.url.toString()

                if (
                    url.startsWith("http://") ||
                    url.startsWith("https://")
                ) {
                    showExternalLinkDialog(url)
                    return true
                }

                return false
            }

            @Suppress("DEPRECATION")
            override fun shouldOverrideUrlLoading(
                view: WebView,
                url: String
            ): Boolean {

                if (
                    url.startsWith("http://") ||
                    url.startsWith("https://")
                ) {
                    showExternalLinkDialog(url)
                    return true
                }

                return false
            }
        }

        /*
         * JavaScriptからの権限要求
         */
        webView.webChromeClient = object : WebChromeClient() {

            /*
             * カメラ
             */
            override fun onPermissionRequest(
                request: PermissionRequest
            ) {
                runOnUiThread {

                    if (
                        request.resources.contains(
                            PermissionRequest.RESOURCE_VIDEO_CAPTURE
                        )
                    ) {

                        pendingCameraRequest = request

                        showCameraPermissionDialog()

                    } else {

                        request.deny()
                    }
                }
            }

            /*
             * 位置情報
             */
            override fun onGeolocationPermissionsShowPrompt(
                origin: String,
                callback: GeolocationPermissions.Callback
            ) {
                runOnUiThread {

                    pendingLocationOrigin = origin
                    pendingLocationCallback = callback

                    showLocationPermissionDialog()
                }
            }
        }

        setContentView(webView)

        /*
         * assets/index.htmlを読み込む
         */
        webView.loadUrl(
            "file:///android_asset/index.html"
        )
    }

    /*
     * カメラ使用確認
     *
     * HTMLから要求されるたびに表示
     */
    private fun showCameraPermissionDialog() {

        AlertDialog.Builder(this)
            .setTitle("カメラの使用")
            .setMessage(
                "このページがカメラを使用しようとしています。\n\n" +
                "カメラの使用を許可しますか？"
            )
            .setPositiveButton("許可") { _, _ ->

                requestCameraPermission()
            }
            .setNegativeButton("許可しない") { _, _ ->

                denyCameraPermission()
            }
            .setOnCancelListener {

                denyCameraPermission()
            }
            .show()
    }

    /*
     * 位置情報使用確認
     *
     * HTMLから要求されるたびに表示
     */
    private fun showLocationPermissionDialog() {

        AlertDialog.Builder(this)
            .setTitle("位置情報の使用")
            .setMessage(
                "このページが位置情報を使用しようとしています。\n\n" +
                "位置情報の使用を許可しますか？"
            )
            .setPositiveButton("許可") { _, _ ->

                requestLocationPermission()
            }
            .setNegativeButton("許可しない") { _, _ ->

                denyLocationPermission()
            }
            .setOnCancelListener {

                denyLocationPermission()
            }
            .show()
    }

    /*
     * カメラのAndroid権限を確認
     */
    private fun requestCameraPermission() {

        if (
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        ) {

            grantCameraPermission()

        } else {

            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.CAMERA
                ),
                REQUEST_CAMERA
            )
        }
    }

    /*
     * 位置情報のAndroid権限を確認
     */
    private fun requestLocationPermission() {

        val fineGranted =
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

        val coarseGranted =
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

        if (fineGranted || coarseGranted) {

            grantLocationPermission()

        } else {

            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                REQUEST_LOCATION
            )
        }
    }

    /*
     * カメラをWebViewへ許可
     */
    private fun grantCameraPermission() {

        pendingCameraRequest?.grant(
            arrayOf(
                PermissionRequest.RESOURCE_VIDEO_CAPTURE
            )
        )

        pendingCameraRequest = null
    }

    /*
     * カメラを拒否
     */
    private fun denyCameraPermission() {

        pendingCameraRequest?.deny()

        pendingCameraRequest = null
    }

    /*
     * 位置情報をWebViewへ許可
     */
    private fun grantLocationPermission() {

        pendingLocationCallback?.invoke(
            pendingLocationOrigin,
            true,
            false
        )

        pendingLocationCallback = null
        pendingLocationOrigin = null
    }

    /*
     * 位置情報を拒否
     */
    private fun denyLocationPermission() {

        pendingLocationCallback?.invoke(
            pendingLocationOrigin,
            false,
            false
        )

        pendingLocationCallback = null
        pendingLocationOrigin = null
    }

    /*
     * Androidの権限結果
     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(
            requestCode,
            permissions,
            grantResults
        )

        when (requestCode) {

            REQUEST_CAMERA -> {

                if (
                    grantResults.isNotEmpty() &&
                    grantResults[0] ==
                    PackageManager.PERMISSION_GRANTED
                ) {

                    grantCameraPermission()

                } else {

                    denyCameraPermission()
                }
            }

            REQUEST_LOCATION -> {

                val granted =
                    grantResults.any {
                        it ==
                            PackageManager.PERMISSION_GRANTED
                    }

                if (granted) {

                    grantLocationPermission()

                } else {

                    denyLocationPermission()
                }
            }
        }
    }

    /*
     * http / httpsリンク
     *
     * モバイルデータ通信の確認を表示して
     * デフォルトブラウザへ移動
     */
    private fun showExternalLinkDialog(url: String) {

        AlertDialog.Builder(this)
            .setTitle("外部サイトを開きます")
            .setMessage(
                "このサイトを開くと、モバイルデータ通信を使用する可能性があります。\n\n" +
                "デフォルトブラウザで開きますか？"
            )
            .setPositiveButton("開く") { _, _ ->

                try {

                    val intent = Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse(url)
                    )

                    startActivity(intent)

                } catch (e: Exception) {

                    Toast.makeText(
                        this,
                        "ブラウザを開けませんでした。",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            .setNegativeButton(
                "キャンセル",
                null
            )
            .show()
    }

    /*
     * 戻るボタン
     */
    @Suppress("DEPRECATION")
    override fun onBackPressed() {

        if (webView.canGoBack()) {

            webView.goBack()

        } else {

            showExitDialog()
        }
    }

    /*
     * アプリ終了確認
     */
    private fun showExitDialog() {

        AlertDialog.Builder(this)
            .setTitle("アプリを終了しますか？")
            .setMessage(
                "アプリを終了すると、現在のページを閉じます。"
            )
            .setPositiveButton("終了") { _, _ ->

                finish()
            }
            .setNegativeButton(
                "キャンセル",
                null
            )
            .show()
    }

    /*
     * Activity終了時
     */
    override fun onDestroy() {

        webView.stopLoading()
        webView.destroy()

        super.onDestroy()
    }
}