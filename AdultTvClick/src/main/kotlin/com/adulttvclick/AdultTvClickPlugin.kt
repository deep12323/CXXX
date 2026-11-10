package com.adulttvclick

import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin
import android.content.Context

@CloudstreamPlugin
class AdultTvClickPlugin: Plugin() {
    override fun load(context: Context) {
        registerMainAPI(AdultTvClick())
    }
}
