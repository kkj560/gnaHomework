package com.example.gnahomework

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class MyReceiver : BroadcastReceiver()  {
    @Inject
    lateinit var repository: GameDataRepository

    @RequiresApi(Build.VERSION_CODES.O_MR1)
    override fun onReceive(context: Context?, intent: Intent?) {

        if (intent != null) {
            val action = intent.action
            Timber.d("onReceive action : $action")
            when (action) {
                Intent.ACTION_PACKAGE_ADDED -> {
                    val packageName = intent.data?.schemeSpecificPart
                    repository.handleProgress(packageName, -1)
                    Timber.d("Package added: $packageName")
                }
                Intent.ACTION_PACKAGE_REMOVED -> {
                    val packageName = intent.data?.schemeSpecificPart
                    Timber.d("Package removed: $packageName")
                }
                Intent.ACTION_PACKAGE_REPLACED -> {
                    val packageName = intent.data?.schemeSpecificPart
                    Timber.d( "Package replaced: $packageName")
                }
                PackageInstaller.ACTION_SESSION_UPDATED -> {
                    val sessionId = intent.getIntExtra(PackageInstaller.EXTRA_SESSION_ID, -1)
                    val sessionInfo = context?.packageManager?.packageInstaller?.getSessionInfo(sessionId)
                    val packageName = intent.data?.schemeSpecificPart

                    sessionInfo?.let {
                        val progress = it.progress.toFloat() / it.size.toFloat()
                        repository.handleProgress(packageName, (progress*100).toInt())
                        Timber.d("OnActionSessionUpdate packageName : $packageName progress : $progress")
                    }
                }
            }
        }
    }
}