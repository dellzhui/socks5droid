package com.github.appproxy.utils

import android.annotation.TargetApi
import com.github.appproxy.App.Companion.app
import com.github.appproxy.bg.BaseService
import com.github.appproxy.database.Profile
import com.github.appproxy.database.ProfileManager
import com.github.appproxy.preference.DataStore
import java.io.File
import java.io.FileNotFoundException
import java.io.ObjectInputStream
import java.io.ObjectOutputStream

@TargetApi(24)
object DirectBoot {
    private val file = File(app.deviceContext.noBackupFilesDir, "directBootProfile")

    fun getDeviceProfile(): Profile? = try {
        ObjectInputStream(file.inputStream()).use { it.readObject() as Profile }
    } catch (_: FileNotFoundException) { null }

    fun clean() {
        file.delete()
        File(app.deviceContext.noBackupFilesDir, BaseService.CONFIG_FILE).delete()
    }

    fun update() {
        val profile = ProfileManager.getProfile(DataStore.profileId)    // app.currentProfile will call this
        if (profile == null) clean() else ObjectOutputStream(file.outputStream()).use { it.writeObject(profile) }
    }
}
