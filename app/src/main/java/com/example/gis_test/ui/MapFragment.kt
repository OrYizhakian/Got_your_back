    package com.example.gis_test.ui

    import android.content.Context
    import android.graphics.Bitmap
    import android.graphics.BitmapFactory
    import android.graphics.Canvas
    import android.graphics.Color
    import android.graphics.Paint
    import android.graphics.RectF
    import android.graphics.drawable.BitmapDrawable
    import android.os.Bundle
    import android.util.Log
    import android.view.LayoutInflater
    import android.view.View
    import android.view.ViewGroup
    import android.widget.ImageButton
    import android.widget.PopupMenu
    import androidx.appcompat.app.AlertDialog
    import androidx.core.content.ContextCompat
    import androidx.fragment.app.Fragment
    import androidx.lifecycle.lifecycleScope
    import com.example.GotYourBack.R
    import com.example.gis_test.data.Business
    import com.google.android.gms.maps.CameraUpdateFactory
    import com.google.android.gms.maps.GoogleMap
    import com.google.android.gms.maps.OnMapReadyCallback
    import com.google.android.gms.maps.SupportMapFragment
    import com.google.android.gms.maps.model.CameraPosition
    import com.google.android.gms.maps.model.LatLng
    import com.google.android.gms.maps.model.MarkerOptions
    import com.google.firebase.firestore.FirebaseFirestore
    import kotlinx.coroutines.Dispatchers
    import kotlinx.coroutines.launch
    import kotlinx.coroutines.tasks.await
    import kotlinx.coroutines.withContext
    import com.google.android.gms.maps.model.BitmapDescriptor
    import com.google.android.gms.maps.model.BitmapDescriptorFactory
    import com.google.android.gms.maps.model.MapStyleOptions

    class MapFragment : Fragment(), OnMapReadyCallback {
        private lateinit var googleMap: GoogleMap
        private val firestore = FirebaseFirestore.getInstance()

        private var focusLatitude: Double? = null
        private var focusLongitude: Double? = null
        private val selectedCategories = mutableSetOf<String>() // Stores selected categories


        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            arguments?.let { args ->
                focusLatitude = args.getDouble("latitude", 0.0).takeIf { it != 0.0 }
                focusLongitude = args.getDouble("longitude", 0.0).takeIf { it != 0.0 }
                Log.d("MapFragment", "Received coordinates: lat=$focusLatitude, lon=$focusLongitude")
            }
        }

        override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
        ): View {
            val view = inflater.inflate(R.layout.mapview, container, false)

            val mapFragment =
                childFragmentManager.findFragmentById(R.id.map_fragment) as? SupportMapFragment
            mapFragment?.getMapAsync(this)

            // ✅ Find the burger button
            val burgerButton: ImageButton = view.findViewById(R.id.burger_button)

            // ✅ Show menu when clicked
            burgerButton.setOnClickListener {
                showMenu(it)
            }

            return view
        }


        override fun onMapReady(map: GoogleMap) {
            googleMap = map
            // ✅ Disable default Google businesses
            googleMap.setMapStyle(
                MapStyleOptions.loadRawResourceStyle(requireContext(), R.raw.map_style)
            )

            val defaultLocation = LatLng(32.0853, 34.7818)
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 13f))

            googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(
                CameraPosition.Builder()
                    .target(defaultLocation)
                    .zoom(13f)
                    .tilt(10f) // Forces compass to appear
                    .build()
            ))
            googleMap.setPadding(0, 150, 0, 150) // Top padding = 150, Bottom padding = 300

            googleMap.uiSettings.apply {
                isZoomControlsEnabled = true
                isCompassEnabled = true // ✅ Ensure compass is visible
                isMapToolbarEnabled = false // ✅ Remove bottom navigation toolbar
            }
            // **Focus on a specific business if provided**
            if (focusLatitude != null && focusLongitude != null) {
                val focusedLocation = LatLng(focusLatitude!!, focusLongitude!!)
                focusOnBusiness(focusedLocation)
            }

            // Load businesses
            loadBusinessesFromFirestore()

            // Handle info window click (to show popup)
            googleMap.setOnInfoWindowClickListener { marker ->
                val business = marker.tag as? Business
                business?.let {
                    showDescriptionPopup(it)
                }
            }
        }




        private fun focusOnBusiness(location: LatLng) {
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 15f))
            googleMap.addMarker(
                MarkerOptions().position(location).title("Selected Business")
            )
        }

        private fun loadBusinessesFromFirestore() {
            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    val businessesSnapshot = withContext(Dispatchers.IO) {
                        firestore.collection("businesses").get().await()
                    }

                    if (businessesSnapshot.isEmpty) {
                        Log.e("MapFragment", "No businesses found in Firebase!")
                        return@launch
                    }

                    googleMap.clear() // Remove all markers before reloading

                    for (document in businessesSnapshot.documents) {
                        val name = document.getString("name") ?: continue
                        val latitude = document.getDouble("latitude") ?: continue
                        val longitude = document.getDouble("longitude") ?: continue
                        val category = document.getString("category") ?: "No category"
                        val description = document.getString("description") ?: "No description"
                        val street = document.getString("street") ?: "Unknown street"
                        val streetNumber = document.getString("streetNumber") ?: ""
                        val openingHours = document.getString("openingHours") ?: "Closed"
                        val closingHours = document.getString("closingHours") ?: "Closed"

                        // **Filter businesses based on selected categories**
                        if (selectedCategories.isNotEmpty() && !selectedCategories.contains(category)) {
                            continue
                        }

                        val position = LatLng(latitude, longitude)

                        val marker = googleMap.addMarker(
                            MarkerOptions()
                                .position(position)
                                .title(name)
                                .snippet("📍 $street $streetNumber\n🕒 $openingHours - $closingHours")
                                .icon(getCategoryIcon(category,requireContext())) // ✅ Use category-specific icons
                        )

                        // Attach the full Business object to the marker
                        marker?.tag = Business(
                            businessId = 0, // Not needed
                            businessIdFirestore = document.id,
                            userId = "",
                            name = name,
                            category = category,
                            street = street,
                            streetNumber = streetNumber,
                            openingHours = openingHours,
                            closingHours = closingHours,
                            description = description,
                            latitude = latitude,
                            longitude = longitude
                        )
                    }
                } catch (e: Exception) {
                    Log.e("MapFragment", "Error loading businesses from Firestore", e)
                }
            }
        }



        private fun showDescriptionPopup(business: Business) {
            val context = requireContext()

            // Create a simple AlertDialog to show the business description
            val builder = AlertDialog.Builder(context)
            builder.setTitle(business.name) // Business Name as title

            // Show the full description
            builder.setMessage("📍 ${business.street} ${business.streetNumber}\n\n" +
                    "🕒 ${business.openingHours} - ${business.closingHours}\n\n" +
                    "🏷️ ${business.category}\n\n" +
                    "📝 ${business.description}")

            builder.setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
            builder.show()
        }



        private fun getCategoryIcon(category: String, context: Context): BitmapDescriptor {
            val color = when (category) {
                "Restaurant" -> Color.RED
                "Coffee place" -> Color.parseColor("#FFA500") // Orange
                "Beauty salon" -> Color.parseColor("#FFC0CB") // Pink
                "Grocery store" -> Color.GREEN
                "Clothes store" -> Color.BLUE
                "Book store" -> Color.parseColor("#87CEEB") // Sky Blue
                "Gym" -> Color.YELLOW
                "Pharmacy" -> Color.CYAN
                "Hardware store" -> Color.parseColor("#8A2BE2") // Violet
                "Jewelry store" -> Color.MAGENTA
                else -> Color.DKGRAY
            }

            val iconRes = when (category) {
                "Restaurant" -> R.drawable.icons8_restaurant
                "Coffee place" -> R.drawable.icons8_coffee
                "Beauty salon" -> R.drawable.icons8_beauty_salon
                "Grocery store" -> R.drawable.icons8_buying
                "Clothes store" -> R.drawable.icons8_clothes
                "Book store" -> R.drawable.icons8_book
                "Gym" -> R.drawable.icons8_gym
                "Pharmacy" -> R.drawable.icons8_pharmacy
                "Hardware store" -> R.drawable.icons8_hammer
                "Jewelry store" -> R.drawable.icons8_diamond
                else -> R.drawable.ic_launcher_foreground // Default icon
            }

            return BitmapDescriptorFactory.fromBitmap(createTearDropMarker(color, iconRes, context))
        }


        private fun createTearDropMarker(color: Int, iconRes: Int, context: Context): Bitmap {
            val width = 100
            val height = 150

            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            val paint = Paint().apply {
                isAntiAlias = true
                this.color = color
                style = Paint.Style.FILL
            }

            // Draw the tear-drop shape
            val ovalRect = RectF(0f, 0f, width.toFloat(), width.toFloat()) // Circular part
            canvas.drawOval(ovalRect, paint)

            val path = android.graphics.Path().apply {
                moveTo(width / 2f, height.toFloat()) // Bottom point
                lineTo(width.toFloat(), width.toFloat()) // Right
                lineTo(0f, width.toFloat()) // Left
                close()
            }
            canvas.drawPath(path, paint)

            // Draw the category icon in the center
            val icon = BitmapFactory.decodeResource(context.resources, iconRes)
            val resizedIcon = Bitmap.createScaledBitmap(icon, 50, 50, false) // Resize to fit

            val iconX = (width - resizedIcon.width) / 2f
            val iconY = (width - resizedIcon.height) / 2f
            canvas.drawBitmap(resizedIcon, iconX, iconY, null)

            return bitmap
        }





        private fun showCategoryFilterDialog() {
            val categories = arrayOf(
                "Restaurant", "Coffee place", "Beauty salon", "Grocery store",
                "Clothes store", "Book store", "Gym", "Pharmacy",
                "Hardware store", "Jewelry store"
            )

            val selectedArray = BooleanArray(categories.size) { selectedCategories.contains(categories[it]) }

            AlertDialog.Builder(requireContext())
                .setTitle("Select Categories")
                .setMultiChoiceItems(categories, selectedArray) { _, which, isChecked ->
                    if (isChecked) selectedCategories.add(categories[which])
                    else selectedCategories.remove(categories[which])
                }
                .setPositiveButton("Apply") { _, _ ->
                    loadBusinessesFromFirestore() // Reload businesses with new filter
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        private fun applyMapTheme(theme: String) {
            val styleRes = when (theme) {
                "default" -> R.raw.map_style
                "Night" -> R.raw.night_mode_style
                "Retro" -> R.raw.retro_style
                "Dark" -> R.raw.dark_mode_style
                "Light" -> R.raw.light_mode_style
                else -> R.raw.map_style
            }
            if (styleRes != null) {
                googleMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(requireContext(), styleRes))
            } else {
                googleMap.setMapStyle(null) // Reset to default Google Maps style
            }        }

        private fun showThemeSelectionDialog() {
            val themes = arrayOf("Standard", "Night", "Retro", "Dark", "Light")

            AlertDialog.Builder(requireContext())
                .setTitle("Select Map Theme")
                .setItems(themes) { _, which ->
                    val selectedTheme = themes[which]
                    applyMapTheme(selectedTheme) // ✅ Apply selected theme
                }
                .show()
        }

        private fun showMenu(anchor: View) {
            val popupMenu = PopupMenu(requireContext(), anchor)
            popupMenu.menuInflater.inflate(R.menu.map_options_menu, popupMenu.menu)

            // Handle menu item clicks
            popupMenu.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.menu_change_theme -> {
                        showThemeSelectionDialog() // ✅ Open Theme Selection
                        true
                    }
                    R.id.menu_filter -> {
                        showCategoryFilterDialog() // ✅ Open Filter Selection
                        true
                    }
                    else -> false
                }
            }

            popupMenu.show()
        }







    }
