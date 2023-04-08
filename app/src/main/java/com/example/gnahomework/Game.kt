package com.example.gnahomework

import android.content.Intent
import android.net.Uri
import android.widget.ProgressBar
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import timber.log.Timber


data class Game(
    @SerializedName("game_name") @Expose var appName: String,
    @SerializedName("package_name") @Expose var packageName: String,
    @SerializedName("icon") @Expose var iconUri: String
)

data class GameData(
    val appName: String,
    val packageName: String,
    val iconUri: String,
    val progress: Int
)

@Composable
fun game(
    game: GameData
) {
    val context = LocalContext.current
    val intentToEnterStore = remember {
        Intent(
            Intent.ACTION_VIEW,
            Uri.parse("https://play.google.com/store/apps/details?id=${game.packageName}")
        )
    }

    Row(modifier = Modifier.fillMaxWidth().padding(4.dp).clickable {
        context.startService(Intent(context, MyService::class.java).putExtra("selectedPackageName", "${game.packageName}"))
        context.startActivity(intentToEnterStore)
    }) {
        Image(
            painter = rememberAsyncImagePainter(
                ImageRequest.Builder(LocalContext.current)
                    .data(game.iconUri)
                    .crossfade(true)
                    .build()
            ),
            contentDescription = "${game.appName}",
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(64.dp).clip(RoundedCornerShape(8.dp))
        )

        Column(modifier = Modifier.padding(start = 8.dp)) {
            Text("${game.appName}")
            if (game.progress != -1 && game.progress != 100) {
                Row() {
                    LinearProgressIndicator(progress = game.progress / 100f)
                    Text("${game.progress}%")
                }

            }
        }
    }
}