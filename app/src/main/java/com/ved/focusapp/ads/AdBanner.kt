package com.ved.focusapp.ads

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.ved.focusapp.R

@Composable
fun AdBanner(
    modifier: Modifier = Modifier,
    adUnitId: String? = null
) {
    val context = LocalContext.current
    val unitId = adUnitId ?: context.getString(R.string.ad_unit_id_banner)
    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            AdView(ctx).apply {
                setAdSize(AdSize.BANNER)
                adUnitId = unitId
                loadAd(AdRequest.Builder().build())
            }
        }
    )
}
