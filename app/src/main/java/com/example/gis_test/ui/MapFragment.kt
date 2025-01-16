package com.example.gis_test.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.gis_test.databinding.MapviewBinding

class MapFragment : Fragment() {
    private var _binding: MapviewBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = MapviewBinding.inflate(inflater, container, false)

        // Initialize WebView
        val webView: WebView = binding.mapWebView
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.settings.cacheMode = WebSettings.LOAD_NO_CACHE

        // Add JavaScript Interface for communication
        webView.addJavascriptInterface(WebAppInterface(), "Android")

        // Ensure WebView stays within the app
        webView.webViewClient = WebViewClient()

        // Load the map.html file from assets
        webView.loadUrl("file:///android_asset/map.html")

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Get the address passed from arguments
        val street = arguments?.getString("street")?.let { "$it, Tel Aviv, Israel" } ?: ""
        if (street.isNotBlank()) {
            Toast.makeText(requireContext(), "Searching for: $street", Toast.LENGTH_SHORT).show()
            searchAddressInMap(street) // Send the address to the WebView
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // Function to send address to the WebView for searching
    private fun searchAddressInMap(address: String) {
        val webView: WebView = binding.mapWebView
        webView.evaluateJavascript("searchAddress('$address');", null)
    }

    // JavaScript Interface for communication
    inner class WebAppInterface {
        @JavascriptInterface
        fun sendCoordinates(lat: Double, lng: Double) {
            // Handle coordinates received from the JavaScript function
            println("Received coordinates: Latitude = $lat, Longitude = $lng")
            Toast.makeText(requireContext(), "Coordinates: $lat, $lng", Toast.LENGTH_SHORT).show()
        }
    }
}
