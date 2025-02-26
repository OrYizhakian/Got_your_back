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
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class MapFragment : Fragment() {
    private var _binding: MapviewBinding? = null
    private val binding get() = _binding!!
    private var focusBusinessId: Long? = null
    private var userId: Long? = null
    private var firebaseUserId: String? = null
    private val firestore = FirebaseFirestore.getInstance()

    @JavascriptInterface
    fun onMapReady() {
        activity?.runOnUiThread {
            loadAllBusinesses()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let { args ->
            focusBusinessId = args.getLong("focusBusinessId", -1L).takeIf { it != -1L }
            userId = args.getLong("userId", -1L).takeIf { it != -1L }
            firebaseUserId = args.getString("firebaseUserId")

            Log.d("MapFragment", "Received arguments: focusBusinessId=$focusBusinessId, " +
                    "userId=$userId, firebaseUserId=$firebaseUserId")
        }
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

    override fun onResume() {
        super.onResume()
        loadAllBusinesses()
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
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    view?.evaluateJavascript(
                        "if (typeof onWebViewReady === 'function') { onWebViewReady(); }",
                        null
                    )
                }
            }
        }

        binding.mapWebView.loadUrl("file:///android_asset/map.html")
    }

    private fun loadAllBusinesses() {
        lifecycleScope.launch {
            try {
                val businesses = mutableListOf<Business>()

                val localBusinesses = withContext(Dispatchers.IO) {
                    if (userId != null) {
                        AppDatabase.getDatabase(requireContext())
                            .businessDao()
                            .getBusinessesByUserId(userId!!)
                    } else {
                        AppDatabase.getDatabase(requireContext())
                            .businessDao()
                            .getAllBusinesses()
                    }
                }
                businesses.addAll(localBusinesses)

                val firebaseBusinesses = withContext(Dispatchers.IO) {
                    firestore.collection("businesses").get().await().documents.mapNotNull { document ->
                        try {
                            Business(
                                businessId = document.id.hashCode().toLong(),
                                userId = (-1L).toString(),
                                name = document.getString("name") ?: return@mapNotNull null,
                                category = document.getString("category") ?: return@mapNotNull null,
                                street = document.getString("street") ?: return@mapNotNull null,
                                streetNumber = document.getString("streetNumber") ?: return@mapNotNull null,
                                openingHours = document.getString("openingHours") ?: "",
                                closingHours = document.getString("closingHours") ?: "",
                                description = document.getString("description"),
                                latitude = document.getDouble("latitude"),
                                longitude = document.getDouble("longitude")
                            )
                        } catch (e: Exception) {
                            null
                        }
                    }
                }
                businesses.addAll(firebaseBusinesses)

                val businessesJson = JSONArray().apply {
                    businesses.forEach { business ->
                        put(JSONObject().apply {
                            put("id", business.businessId)
                            put("name", business.name)
                            put("address", "${business.street} ${business.streetNumber}")
                            put("category", business.category)
                            put("latitude", business.latitude)
                            put("longitude", business.longitude)
                        })
                    }
                }

                binding.mapWebView.evaluateJavascript("loadBusinesses($businessesJson, $focusBusinessId);", null)
            } catch (e: Exception) {
                Log.e("MapFragment", "Error loading businesses", e)
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
