package com.example.gis_test.ui

import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.*
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.gis_test.databinding.MapviewBinding

class MapFragment : Fragment() {
    private var _binding: MapviewBinding? = null
    private val binding get() = _binding!!
    private var pendingSearch: String? = null

    @JavascriptInterface
    fun onMapReady() {
        Log.d("MapFragment", "onMapReady called")
        activity?.runOnUiThread {
            pendingSearch?.let { address ->
                Log.d("MapFragment", "Executing pending search for: $address")
                searchAddressInMap(address)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = MapviewBinding.inflate(inflater, container, false)

        // Store pending search if provided in arguments
        pendingSearch = arguments?.getString("street")?.let { "$it, Tel Aviv, Israel" }
        Log.d("MapFragment", "Pending search address: $pendingSearch")

        // Initialize WebView
        val webView: WebView = binding.mapWebView
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            cacheMode = WebSettings.LOAD_NO_CACHE
            // Add debugging for WebView
            WebView.setWebContentsDebuggingEnabled(true)
        }

        // Add JavaScript Interface for communication
        webView.addJavascriptInterface(this, "Android")

        // Set up WebViewClient to handle page load
        webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                Log.d("MapFragment", "Page loading started: $url")
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                Log.d("MapFragment", "Page loading finished: $url")
                // Inject console log interceptor
                view?.evaluateJavascript("""
                    console.log = function(message) {
                        Android.consoleLog(message);
                    };
                """.trimIndent(), null)
                // Notify JavaScript that WebView is ready
                view?.evaluateJavascript("if (typeof onWebViewReady === 'function') { onWebViewReady(); }", null)
            }

            override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                super.onReceivedError(view, request, error)
                Log.e("MapFragment", "WebView error: ${error?.description}")
            }
        }

        // Load the map.html file from assets
        webView.loadUrl("file:///android_asset/map.html")

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        pendingSearch?.let { address ->
            Log.d("MapFragment", "Ready to search for address: $address")
            Toast.makeText(requireContext(), "Searching for: $address", Toast.LENGTH_SHORT).show()
        }
    }

    // Function to send address to the WebView for searching
    // Update just the searchAddressInMap function in your MapFragment:

    private fun searchAddressInMap(address: String) {
        try {
            val escapedAddress = address.replace("'", "\\'") // Escape single quotes
            val searchScript = """
            console.log('Executing search for: $escapedAddress');
            if (typeof searchAddress === 'function') { 
                try {
                    searchAddress('$escapedAddress');
                } catch (error) {
                    console.log('Error executing searchAddress:', error);
                    Android.onSearchError('Error: ' + error.message);
                }
            } else { 
                console.log('searchAddress function not found'); 
                Android.onSearchError('Search function not available');
            }
        """.trimIndent()

            Log.d("MapFragment", "Executing search script: $searchScript")
            binding.mapWebView.evaluateJavascript(searchScript) { result ->
                Log.d("MapFragment", "Search script result: $result")
            }
        } catch (e: Exception) {
            Log.e("MapFragment", "Error searching address", e)
            Toast.makeText(
                requireContext(),
                "Error searching address: ${e.message}",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    @JavascriptInterface
    fun consoleLog(message: String) {
        Log.d("WebView Console", message)
    }

    // JavaScript Interface for communication
    @JavascriptInterface
    fun sendCoordinates(lat: Double, lng: Double) {
        Log.d("MapFragment", "Received coordinates: $lat, $lng")
        activity?.runOnUiThread {
            Toast.makeText(
                requireContext(),
                "Location found at: $lat, $lng",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    @JavascriptInterface
    fun onSearchError(error: String) {
        Log.e("MapFragment", "Search error: $error")
        activity?.runOnUiThread {
            Toast.makeText(
                requireContext(),
                "Error finding location: $error",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}