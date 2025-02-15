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

    private fun loadAllBusinesses() {
        lifecycleScope.launch {
            try {
                val businesses = mutableListOf<Business>()

                // Load Room businesses
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
                Log.d("MapFragment", "Loaded ${localBusinesses.size} local businesses")

                // Load Firebase businesses
                val firebaseBusinesses = withContext(Dispatchers.IO) {
                    val query = if (firebaseUserId != null) {
                        firestore.collection("businesses")
                            .whereEqualTo("userId", firebaseUserId)
                    } else {
                        firestore.collection("businesses")
                    }

                    query.get().await().documents.mapNotNull { document ->
                        try {
                            Business(
                                businessId = document.id.hashCode().toLong(),
                                userId = -1L,
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
                            Log.e("MapFragment", "Error parsing Firebase business: ${e.message}")
                            null
                        }
                    }
                }
                businesses.addAll(firebaseBusinesses)
                Log.d("MapFragment", "Loaded ${firebaseBusinesses.size} Firebase businesses")

                // Convert to JSON for the map
                val businessesJson = JSONArray().apply {
                    businesses.forEach { business ->
                        put(JSONObject().apply {
                            put("id", business.businessId)
                            put("name", business.name)
                            put("address", "${business.street} ${business.streetNumber}, Tel Aviv, Israel")
                            put("category", business.category)
                            put("latitude", business.latitude)
                            put("longitude", business.longitude)
                        })
                    }
                }

                // Log focused business details
                if (focusBusinessId != null) {
                    val focusedBusiness = businesses.find { it.businessId == focusBusinessId }
                    Log.d("MapFragment", "Focus business details: " +
                            "ID=${focusedBusiness?.businessId}, " +
                            "Name=${focusedBusiness?.name}, " +
                            "Lat=${focusedBusiness?.latitude}, " +
                            "Lng=${focusedBusiness?.longitude}")
                }

                // Load businesses into the map
                val script = "loadBusinesses($businessesJson, $focusBusinessId);"
                binding.mapWebView.evaluateJavascript(script) { result ->
                    Log.d("MapFragment", "Map script evaluation result: $result")
                }

            } catch (e: Exception) {
                Log.e("MapFragment", "Error loading businesses", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        requireContext(),
                        "Error loading businesses: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
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
                // Update Room database
                AppDatabase.getDatabase(requireContext())
                    .businessDao()
                    .updateBusinessCoordinates(businessId, latitude, longitude)

                // Update Firebase
                val businessQuery = firestore.collection("businesses")
                    .whereEqualTo("businessId", businessId)
                    .get()
                    .await()

                businessQuery.documents.firstOrNull()?.reference?.update(
                    mapOf(
                        "latitude" to latitude,
                        "longitude" to longitude
                    )
                )?.await()

                Log.d("MapFragment", "Updated coordinates for business $businessId: $latitude, $longitude")
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