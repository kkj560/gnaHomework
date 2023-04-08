package com.example.gnahomework

import android.R
import android.annotation.SuppressLint
import android.app.*
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.os.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.app.AppOpsManagerCompat
import androidx.core.app.NotificationCompat
import dagger.hilt.android.AndroidEntryPoint
import java.util.*
import javax.inject.Inject


@AndroidEntryPoint
class MyService() : Service() {

    @Inject
    lateinit var repository: GameDataRepository

    private lateinit var usageStatsManager: UsageStatsManager
    private lateinit var packageInstaller: PackageInstaller

    var selectedPackageName = ""
    var isDownloading = false
    var isHandlerRunning = false
    val context = this

    override fun onCreate() {
        super.onCreate()

        packageInstaller = this.packageManager.packageInstaller
        if (usagePermissionCheck())
            usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

        if (overlayPermissionCheck() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val builder = NotificationCompat.Builder(this, "default")
            builder.setSmallIcon(R.mipmap.sym_def_app_icon)
            builder.setContentTitle("Foreground Service")
            builder.setContentText("포그라운드 서비스")
            builder.color = Color.Red.toArgb()
            val notificationIntent = Intent(this, MainActivity::class.java)
            notificationIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            val pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0)
            builder.setContentIntent(pendingIntent) // 알림 클릭 시 이동

            // 알림 표시
            val notificationManager =
                this.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                notificationManager.createNotificationChannel(
                    NotificationChannel(
                        "default",
                        "기본 채널",
                        NotificationManager.IMPORTANCE_DEFAULT
                    )
                )
            }
            notificationManager.notify(1, builder.build()) // id : 정의해야하는 각 알림의 고유한 int값
            val notification = builder.build()
            startForeground(1, notification)
        }
    }

    private fun usagePermissionCheck(): Boolean {
        val appOps = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode =
            appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), packageName)
        return mode == AppOpsManagerCompat.MODE_ALLOWED
    }
    private fun overlayPermissionCheck(): Boolean {
        val appOps = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_SYSTEM_ALERT_WINDOW,
            Process.myUid(),
            packageName
        )

        return mode == AppOpsManagerCompat.MODE_ALLOWED
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // packageName을 intent로 받는다.
        val fromIntentPackagename = intent?.getStringExtra("selectedPackageName")
        if (!fromIntentPackagename.isNullOrEmpty())
            selectedPackageName = fromIntentPackagename

        if (usagePermissionCheck() && !isHandlerRunning) {
            isHandlerRunning = true
            val handler = Handler(Looper.getMainLooper())
            handler.post(object : Runnable {
                override fun run() {
                    queryUsageEvents()
                    handler.postDelayed(this, 1000) // Query every second
                }
            })
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun queryUsageEvents() {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.SECOND, -1)
        val startTime = calendar.timeInMillis
        val endTime = System.currentTimeMillis()
        val usageEvents = usageStatsManager.queryEvents(startTime, endTime)

        while (usageEvents.hasNextEvent()) {
            val event = UsageEvents.Event()
            usageEvents.getNextEvent(event)
            onUsageEvent(event)
        }
    }

    @SuppressLint("Range")
    private fun onUsageEvent(event: UsageEvents.Event?) {
        val isForeGroundEvent =
            if (BuildConfig.VERSION_CODE >= 29) event?.eventType == UsageEvents.Event.ACTIVITY_RESUMED else event?.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND;

        if (!isDownloading && selectedPackageName.isNotEmpty() && isForeGroundEvent && event?.packageName == "com.android.vending") {
            // Play Store app has been brought to the foreground, register PackageInstall.SessionCallback()
            isDownloading = true
            val params =
                PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL)
            params.setAppPackageName(selectedPackageName)
            val sessionId = packageInstaller.createSession(params)
            packageInstaller.registerSessionCallback(MySessionCallback())
            packageInstaller.openSession(sessionId)
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        // Not used
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    inner class MySessionCallback() : PackageInstaller.SessionCallback() {

        var displayActivityFlag = false
        override fun onCreated(sessionId: Int) {
            //TODO("Not yet implemented")
        }

        override fun onBadgingChanged(sessionId: Int) {
            //TODO("Not yet implemented")
        }

        override fun onActiveChanged(sessionId: Int, active: Boolean) {
            if (!active) {
                // Installation has been canceled
                repository.handleProgress(selectedPackageName, -1)
                isDownloading = false
            }
        }

        override fun onProgressChanged(sessionId: Int, progress: Float) {
            // Display MainActivity once.
            if(!displayActivityFlag){
                displayActivityFlag = true
                val intent = Intent(context, MainActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                startActivity(intent)
            }
            repository.handleProgress(selectedPackageName, (progress * 100).toInt())
        }

        override fun onFinished(sessionId: Int, success: Boolean) {
            if(success){
                repository.handleProgress(selectedPackageName, 100)
            }else{
                repository.handleProgress(selectedPackageName, -1)
            }

            packageInstaller.unregisterSessionCallback(this)
            isDownloading = false
        }
    }
}