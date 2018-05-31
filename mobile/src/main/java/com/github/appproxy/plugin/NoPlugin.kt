package com.github.appproxy.plugin

import com.github.appproxy.App.Companion.app

object NoPlugin : Plugin() {
    override val id: String get() = ""
    override val label: CharSequence get() = app.getText(com.github.appproxy.R.string.plugin_disabled)
}
