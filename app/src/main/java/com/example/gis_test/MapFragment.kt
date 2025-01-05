package com.example.gis_test

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.fragment.app.Fragment
import com.example.gis_test.databinding.MapviewBinding

class MapFragment: Fragment() {
    private var _binding: MapviewBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(

        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the fragment layout
        val view = inflater.inflate(R.layout.mapview, container, false)

        // Find the WebView in the fragment's layout
        val webView: WebView = view.findViewById(R.id.mapWebView)

        // Enable JavaScript
        val webSettings: WebSettings = webView.settings
        webSettings.javaScriptEnabled = true

        // Ensure WebView stays within the app
        webView.webViewClient = WebViewClient()

        // Load the map.html file from assets
        webView.loadUrl("file:///android_asset/map.html")

        return view
        }



    override fun onDestroyView() {
        super.onDestroyView()
        _binding=null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }
}