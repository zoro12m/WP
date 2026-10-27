package com.sowarapp.android.ui.details

import android.Manifest
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sowarapp.android.R
import com.sowarapp.android.ui.AppViewModelProvider
import com.sowarapp.android.ui.components.BackTopBar
import com.sowarapp.android.ui.components.CaptionedImage
import com.sowarapp.android.ui.components.EmptyState
import com.sowarapp.android.ui.components.IMAGE_ASPECT_RATIO

@Composable
fun ImageDetailsScreen(
    onBack: () -> Unit,
    viewModel: ImageDetailsViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val isFavorite by viewModel.isFavorite.collectAsStateWithLifecycle()
    val isBusy by viewModel.isBusy.collectAsStateWithLifecycle()

    val shareChooserTitle = stringResource(R.string.share_chooser_title)
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is DetailsEvent.ShowMessage ->
                    snackbarHostState.showSnackbar(context.getString(event.messageRes))
                is DetailsEvent.OpenShareSheet ->
                    openShareSheet(context, event, shareChooserTitle)
            }
        }
    }

    // Android 9 and older ask the user before an app can write to the gallery.
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) viewModel.download() else viewModel.onStoragePermissionDenied()
    }
    val onDownload = {
        if (needsStoragePermission(context)) {
            permissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        } else {
            viewModel.download()
        }
    }

    Scaffold(
        topBar = { BackTopBar(title = stringResource(R.string.details_title), onBack = onBack) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        val image = viewModel.image
        if (image == null) {
            EmptyState(
                icon = Icons.Filled.Info,
                title = stringResource(R.string.image_not_found),
                message = "",
                modifier = Modifier.padding(innerPadding),
            )
            return@Scaffold
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            Card(
                shape = MaterialTheme.shapes.extraLarge,
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                modifier = Modifier
                    .widthIn(max = 480.dp)
                    .fillMaxWidth()
                    .aspectRatio(IMAGE_ASPECT_RATIO)
                    .semantics { contentDescription = image.caption },
            ) {
                CaptionedImage(image = image, modifier = Modifier.fillMaxSize())
            }

            if (isBusy) {
                LinearProgressIndicator(modifier = Modifier.widthIn(max = 480.dp).fillMaxWidth())
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.widthIn(max = 480.dp).fillMaxWidth(),
            ) {
                ActionButton(
                    label = stringResource(R.string.action_download),
                    painter = painterResource(R.drawable.ic_download),
                    enabled = !isBusy,
                    onClick = onDownload,
                    modifier = Modifier.weight(1f),
                )
                ActionButton(
                    label = stringResource(R.string.action_share),
                    painter = rememberVectorPainter(Icons.Filled.Share),
                    enabled = !isBusy,
                    onClick = viewModel::share,
                    modifier = Modifier.weight(1f),
                )
                ActionButton(
                    label = stringResource(R.string.action_favorite),
                    painter = rememberVectorPainter(
                        if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder
                    ),
                    // Screen readers hear what the tap will do.
                    accessibilityLabel = stringResource(
                        if (isFavorite) R.string.action_remove_favorite else R.string.action_add_favorite
                    ),
                    enabled = true,
                    onClick = viewModel::toggleFavorite,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun ActionButton(
    label: String,
    painter: Painter,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    accessibilityLabel: String = label,
) {
    FilledTonalButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.semantics { contentDescription = accessibilityLabel },
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(painter = painter, contentDescription = null)
            Text(label, style = MaterialTheme.typography.labelMedium)
        }
    }
}

private fun needsStoragePermission(context: Context): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.Q &&
        ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) !=
        PackageManager.PERMISSION_GRANTED

/** Opens Android's share sheet (WhatsApp, Telegram, etc.) with the image file attached. */
private fun openShareSheet(context: Context, event: DetailsEvent.OpenShareSheet, chooserTitle: String) {
    val sendIntent = Intent(Intent.ACTION_SEND).apply {
        type = "image/jpeg"
        putExtra(Intent.EXTRA_STREAM, event.uri)
        putExtra(Intent.EXTRA_TEXT, event.caption)
        // Lets the receiving app read this one file, and shows a preview in the share sheet.
        clipData = ClipData.newRawUri(null, event.uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(sendIntent, chooserTitle))
}
