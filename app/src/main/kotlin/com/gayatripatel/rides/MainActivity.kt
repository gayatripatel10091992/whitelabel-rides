package com.gayatripatel.rides

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.gayatripatel.rides.core.Brands
import com.gayatripatel.rides.ui.BrandTheme

/**
 * Shared by all nine apps. The brand comes from the `brand` flavor's BuildConfig field; the screens come from
 * the `role` flavor's source set (`src/rider`, `src/driver`, `src/kiosk`), so a rider APK contains no driver code.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val brand = Brands.byId(BuildConfig.BRAND_ID)
        setContent {
            BrandTheme(brand) { RoleEntry(brand, this) }
        }
    }
}
