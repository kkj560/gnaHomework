package com.example.gnahomework


import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.AlertDialog
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner

@Composable
fun mainScreen(
    isUsagePermitted: () -> Boolean = { false },
    isOverlayPermitted: () -> Boolean = { false },
    startService: () -> Unit = {},
    gameList: List<GameData>? = null
) {
    val context = LocalContext.current

    var showDialogForUsagePermission by remember { mutableStateOf(false) }
    var showDialogForOverlayPermission by remember { mutableStateOf(false) }
    var dismissDialog by remember { mutableStateOf(false) }

    OnLifecycleEvent { owner, event ->
        // 권한이 허가 안 되어 있으면 허가 받기.
        when (event) {
            Lifecycle.Event.ON_RESUME -> {
                if (!dismissDialog) {
                    if (!isUsagePermitted()) {
                        showDialogForUsagePermission = true
                    } else if (!isOverlayPermitted()) {
                        showDialogForUsagePermission = false
                        showDialogForOverlayPermission = true
                    } else {
                        showDialogForUsagePermission = false
                        showDialogForOverlayPermission = false
                        startService()
                    }
                }
            }
            else -> { /* other stuff */
            }
        }
    }


    Box(modifier = Modifier.fillMaxSize()) {
        if (gameList != null) {
            LazyColumn(
                state = rememberLazyListState(),
                modifier = Modifier.fillMaxSize()
            ) {
                items(gameList, key = { "${it.packageName}_${it.progress}"}) { it ->
                    game(it)
                }
            }
        }
        if (showDialogForUsagePermission) {
            AlertDialog(
                onDismissRequest = {
                    showDialogForUsagePermission = false
                    dismissDialog = true
                },
                title = {
                    Text(
                        text = "안내",
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                },
                text = {
                    Column {
                        Text(
                            text = "사용정보 접근 허용이 필요합니다.",
                            modifier = Modifier.padding(bottom = 5.dp)
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val intent = Intent(
                                Settings.ACTION_USAGE_ACCESS_SETTINGS,
                                Uri.fromParts("package", context.packageName, null)
                            )
                            context.startActivity(intent)
                        }) {
                        Text(text = "설정하기")
                    }
                }
            )
        }
        if (showDialogForOverlayPermission) {
            AlertDialog(
                onDismissRequest = {
                    showDialogForOverlayPermission = false
                    dismissDialog = true
                },
                title = {
                    Text(
                        text = "안내",
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                },
                text = {
                    Column {
                        Text(
                            text = "다른 앱 위에 표시 허용이 필요합니다.",
                            modifier = Modifier.padding(bottom = 5.dp)
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val intent = Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.fromParts("package", context.packageName, null)
                            )
                            context.startActivity(intent)
                        }) {
                        Text(text = "설정하기")
                    }
                }
            )
        }
    }
}

@Composable
fun OnLifecycleEvent(onEvent: (owner: LifecycleOwner, event: Lifecycle.Event) -> Unit) {
    val eventHandler = rememberUpdatedState(onEvent)
    val lifecycleOwner = rememberUpdatedState(LocalLifecycleOwner.current)

    DisposableEffect(lifecycleOwner.value) {
        val lifecycle = lifecycleOwner.value.lifecycle
        val observer = LifecycleEventObserver { owner, event ->
            eventHandler.value(owner, event)
        }

        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
        }
    }
}