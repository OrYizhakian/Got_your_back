package com.example.gis_test

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.gis_test.databinding.MapviewBinding

class MapFragment : Fragment() {
    private var _binding: MapviewBinding? = null
    private val binding get() = _binding!!
    private val viewModel: UserViewModel by activityViewModels() // SharedViewModel

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

        // Observe address changes in SharedViewModel
        viewModel.user.observe(viewLifecycleOwner) { user ->
            val address = "${user.businessStreet} ${user.businessStreetNumber}"
            if (address.isNotBlank()) {
                searchAddressInMap(address)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // Function to send address to the WebView for searching
    fun searchAddressInMap(address: String) {
        val webView: WebView = binding.mapWebView
        webView.evaluateJavascript("searchAddress('$address');", null)
    }

    // JavaScript Interface for communication
    inner class WebAppInterface {
        @JavascriptInterface
        fun sendCoordinates(lat: Double, lng: Double) {
            // Update the user data in SharedViewModel with the selected coordinates
            viewModel.latitude.value = lat
            viewModel.longitude.value = lng
        }
    }
}
