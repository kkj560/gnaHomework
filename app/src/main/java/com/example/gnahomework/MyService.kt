package com.example.gnahomework

import android.R
import android.annotation.SuppressLint
import android.app.*
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageInstaller
import android.os.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.app.AppOpsManagerCompat
import androidx.core.app.NotificationCompat
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import java.util.*
import javax.inject.Inject


@AndroidEntryPoint
class MyService() : Service() {

    @Inject
    lateinit var repository: GameDataRepository

    private lateinit var usageStatsManager: UsageStatsManager
    private val br = MyReceiver()

    override fun onCreate() {
        super.onCreate()

        if (checkUsageStatsPermission())
            usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_REPLACED)
            addAction(PackageInstaller.ACTION_SESSION_UPDATED)
        }

        registerReceiver(br, filter)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
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
            val notificationManager = this.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
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

    private fun checkUsageStatsPermission(): Boolean {
        val appOps = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode =
            appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), packageName)
        return mode == AppOpsManagerCompat.MODE_ALLOWED
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (checkUsageStatsPermission()) {
            val handler = Handler(Looper.getMainLooper())
            handler.post(object : Runnable {
                override fun run() {
                    queryUsageEvents()
                    //checkDownloadStatus()
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

        val isForeGroundEvent = if(BuildConfig.VERSION_CODE >= 29) event?.eventType == UsageEvents.Event.ACTIVITY_RESUMED else event?.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND;

        if (isForeGroundEvent && event?.packageName == "com.android.vending") {
            // Play Store app has been brought to the foreground, check if a download is in progress
            var progress = 0
            var packageName: String? = ""

            val downloadManager = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

            val activeDownloads = DownloadManager.Query().apply {
                setFilterByStatus(DownloadManager.STATUS_RUNNING)
            }

            val cursor = downloadManager.query(activeDownloads)
            Timber.d("cursor count - ${cursor.count}")
            while(cursor.moveToNext()){
                val bytesDownloaded =
                    cursor.getLong(cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                val bytesTotal =
                    cursor.getLong(cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
                progress = (bytesDownloaded.toFloat() / bytesTotal.toFloat() * 100).toInt()

                packageName = cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_URI))?.substringAfterLast("=")

                Timber.d("packageName : $packageName, progress : $progress")
                repository.handleProgress(packageName, progress)
            }
            cursor.close()
        }
    }

    @SuppressLint("Range")
    private fun checkDownloadStatus(){
        var progress = 0
        var packageName: String? = ""

        val downloadManager = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val activeDownloads = DownloadManager.Query().apply {
            setFilterByStatus(DownloadManager.STATUS_RUNNING)
        }
        val cursor = downloadManager.query(activeDownloads)
        Timber.d("cursor count - ${cursor.count}")
        while(cursor.moveToNext()){
            val bytesDownloaded =
                cursor.getLong(cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
            val bytesTotal =
                cursor.getLong(cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
            progress = (bytesDownloaded.toFloat() / bytesTotal.toFloat() * 100).toInt()

            packageName = cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_URI))?.substringAfterLast("=")

            Timber.d("packageName : $packageName, progress : $progress")
            repository.handleProgress(packageName, progress)
        }
        cursor.close()
    }

    override fun onBind(intent: Intent?): IBinder? {
        // Not used
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(br)
    }
}