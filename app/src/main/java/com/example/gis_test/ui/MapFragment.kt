package com.example.gis_test.ui

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.*
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.gis_test.data.AppDatabase
import com.example.gis_test.data.Business
import com.example.GotYourBack.databinding.MapviewBinding
import kotlinx.coroutines.launch

class MapFragment : Fragment() {
    private var _binding: MapviewBinding? = null
    private val binding get() = _binding!!
    private var focusBusinessId: Long? = null
    private var userId: Long? = null

    @JavascriptInterface
    fun onMapReady() {
        activity?.runOnUiThread {
            loadBusinesses()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        userId = arguments?.getLong("userId")
        focusBusinessId = arguments?.getLong("focusBusinessId")
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = MapviewBinding.inflate(inflater, container, false)
        setupWebView()
        return binding.root
    }

    private fun setupWebView() {
        binding.mapWebView.apply {
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                cacheMode = WebSettings.LOAD_NO_CACHE
                setSupportZoom(true)
                builtInZoomControls = true
                displayZoomControls = false
                useWideViewPort = true
                loadWithOverviewMode = true
            }

            setOnTouchListener { v, event ->
                v.parent.requestDisallowInterceptTouchEvent(true)
                v.onTouchEvent(event)
            }

            WebView.setWebContentsDebuggingEnabled(true)
            addJavascriptInterface(this@MapFragment, "Android")

            webViewClient = object : WebViewClient() {
                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                    super.onPageStarted(view, url, favicon)
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    view?.evaluateJavascript("""
                        console.log = function(message) {
                            Android.consoleLog(message);
                        };
                    """.trimIndent(), null)
                    view?.evaluateJavascript(
                        "if (typeof onWebViewReady === 'function') { onWebViewReady(); }",
                        null
                    )
                }

                override fun onReceivedError(
                    view: WebView?,
                    request: WebResourceRequest?,
                    error: WebResourceError?
                ) {
                    super.onReceivedError(view, request, error)
                    Log.e("MapFragment", "WebView error: ${error?.description}")
                }
            }
        }

        binding.mapWebView.loadUrl("file:///android_asset/map.html")
    }

    private fun loadBusinesses() {
        lifecycleScope.launch {
            try {
                val db = AppDatabase.getDatabase(requireContext())
                val businesses = if (userId != null) {
                    db.businessDao().getBusinessesByUserId(userId!!)
                } else {
                    db.businessDao().getAllBusinesses()
                }

                // Debug log to check businesses
                Log.d("MapFragment", "Number of businesses: ${businesses.size}")
                businesses.forEach { business ->
                    Log.d("MapFragment", "Business: ${business.name}, Address: ${business.street} ${business.streetNumber}")
                }

                val businessesJson = businesses.map { business ->
                    """{
                        "id": ${business.businessId},
                        "name": "${business.name.replace("\"", "\\\"")}",
                        "address": "${business.street} ${business.streetNumber}, Tel Aviv, Israel",
                        "category": "${business.category}",
                        "latitude": ${business.latitude ?: "null"},
                        "longitude": ${business.longitude ?: "null"}
                    }"""
                }.joinToString(",", "[", "]")

                // Debug log to check JSON
                Log.d("MapFragment", "Businesses JSON: $businessesJson")

                val script = "loadBusinesses($businessesJson, ${focusBusinessId});"
                binding.mapWebView.evaluateJavascript(script) { result ->
                    Log.d("MapFragment", "Script evaluation result: $result")
                }
            } catch (e: Exception) {
                Log.e("MapFragment", "Error loading businesses", e)
            }
        }
    }

    @JavascriptInterface
    fun consoleLog(message: String) {
        Log.d("WebView Console", message)
    }

    @JavascriptInterface
    fun sendCoordinates(lat: Double, lng: Double) {
        Log.d("MapFragment", "Received coordinates: $lat, $lng")
    }

    @JavascriptInterface
    fun onSearchError(error: String) {
        Log.e("MapFragment", "Search error: $error")
        activity?.runOnUiThread {
            Toast.makeText(requireContext(), "Error finding location: $error", Toast.LENGTH_SHORT).show()
        }
    }

    @JavascriptInterface
    fun saveBusinessCoordinates(businessId: Long, latitude: Double, longitude: Double) {
        lifecycleScope.launch {
            try {
                val businessDao = AppDatabase.getDatabase(requireContext()).businessDao()
                businessDao.updateBusinessCoordinates(businessId, latitude, longitude)
                Log.d("MapFragment", "Saved coordinates for business $businessId: $latitude, $longitude")
            } catch (e: Exception) {
                Log.e("MapFragment", "Error saving coordinates for business $businessId", e)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.mapWebView.apply {
            clearCache(true)
            clearHistory()
            loadUrl("about:blank")
            onPause()
            removeAllViews()
            destroy()
        }
        _binding = null
    }
}