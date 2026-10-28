package com.sowarapp.android.ui.details

import android.Manifest
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sowarapp.android.R
import com.sowarapp.android.data.model.SowarImage
import com.sowarapp.android.ui.AppViewModelProvider
import com.sowarapp.android.ui.components.CaptionStyle
import com.sowarapp.android.ui.components.CaptionedImage
import com.sowarapp.android.ui.components.EmptyState
import com.sowarapp.android.ui.components.RoundIconButton
import com.sowarapp.android.ui.theme.SowarTheme
import com.sowarapp.android.ui.theme.ViewerBackground

/**
 * Full-screen image viewer on a dark background: swipe to the next image of the same category,
 * pinch or double-tap to zoom, and Download / Share / Favorite at the bottom.
 */
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

    // The viewer is always dark, like a photo gallery, whatever the phone's theme.
    SowarTheme(darkTheme = true) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(ViewerBackground),
        ) {
            val images = viewModel.images
            val image = viewModel.image
            if (viewModel.isLoaded && (images.isEmpty() || image == null)) {
                EmptyState(
                    icon = Icons.Filled.Info,
                    title = stringResource(R.string.image_not_found),
                    message = "",
                    modifier = Modifier.align(Alignment.Center),
                )
            } else if (image != null) {
                val pagerState = rememberPagerState(initialPage = viewModel.initialPage) { images.size }
                LaunchedEffect(pagerState, images) {
                    snapshotFlow { pagerState.currentPage }.collect { page ->
                        images.getOrNull(page)?.let { viewModel.onImageShown(it.id) }
                    }
                }
                HorizontalPager(
                    state = pagerState,
                    key = { page -> images[page].id },
                    beyondViewportPageCount = 1,
                    modifier = Modifier.fillMaxSize(),
                ) { page ->
                    ZoomableImage(image = images[page], isCurrentPage = pagerState.currentPage == page)
                }

                ViewerTopBar(
                    title = viewModel.categoryTitle.orEmpty(),
                    counter = stringResource(R.string.page_counter, pagerState.currentPage + 1, images.size),
                    onBack = onBack,
                    modifier = Modifier.align(Alignment.TopCenter),
                )

                ViewerBottomPanel(
                    image = image,
                    isFavorite = isFavorite,
                    isBusy = isBusy,
                    onDownload = onDownload,
                    onShare = viewModel::share,
                    onToggleFavorite = viewModel::toggleFavorite,
                    snackbarHostState = snackbarHostState,
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
            }

            if (image == null) {
                // Loading, or nothing to show: still offer a way back.
                RoundIconButton(
                    icon = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.back),
                    onClick = onBack,
                    modifier = Modifier
                        .statusBarsPadding()
                        .padding(12.dp),
                )
            }
        }
    }
}

@Composable
private fun ViewerTopBar(
    title: String,
    counter: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.6f), Color.Transparent)))
            .statusBarsPadding()
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        RoundIconButton(
            icon = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = stringResource(R.string.back),
            onClick = onBack,
        )
        Spacer(Modifier.width(12.dp))
        Column {
            Text(title, style = MaterialTheme.typography.titleMedium, color = Color.White)
            Text(counter, style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.7f))
        }
    }
}

@Composable
private fun ViewerBottomPanel(
    image: SowarImage,
    isFavorite: Boolean,
    isBusy: Boolean,
    onDownload: () -> Unit,
    onShare: () -> Unit,
    onToggleFavorite: () -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))))
            .navigationBarsPadding()
            .padding(start = 20.dp, end = 20.dp, top = 40.dp, bottom = 16.dp),
    ) {
        SnackbarHost(snackbarHostState)

        // Photo credit: required by the licenses of the photos (e.g. CC BY-SA).
        image.credit?.let { credit ->
            Text(
                text = stringResource(R.string.photo_credit, credit.author, credit.license),
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.7f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        if (isBusy) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
        }

        Row(
            horizontalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
        ) {
            ActionCircle(
                label = stringResource(R.string.action_download),
                painter = painterResource(R.drawable.ic_download),
                enabled = !isBusy,
                onClick = onDownload,
            )
            ActionCircle(
                label = stringResource(R.string.action_share),
                painter = rememberVectorPainter(Icons.Filled.Share),
                enabled = !isBusy,
                onClick = onShare,
            )
            FavoriteCircle(isFavorite = isFavorite, onClick = onToggleFavorite)
        }
    }
}

/** A round button with a label under it. The whole thing is one tap target for screen readers. */
@Composable
private fun ActionCircle(
    label: String,
    painter: Painter,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    accessibilityLabel: String = label,
    highlighted: Boolean = false,
    iconScale: Float = 1f,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(MaterialTheme.shapes.medium)
            .clickable(enabled = enabled, onClick = onClick)
            .clearAndSetSemantics {
                contentDescription = accessibilityLabel
                role = Role.Button
            }
            .alpha(if (enabled) 1f else 0.5f)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(58.dp)
                .background(
                    if (highlighted) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.16f),
                    CircleShape,
                ),
        ) {
            Icon(
                painter = painter,
                contentDescription = null,
                tint = if (highlighted) MaterialTheme.colorScheme.onPrimary else Color.White,
                modifier = Modifier.scale(iconScale),
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = Color.White,
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}

/** The heart "pops" when an image is added to favorites. */
@Composable
private fun FavoriteCircle(isFavorite: Boolean, onClick: () -> Unit) {
    val heartScale = remember { Animatable(1f) }
    LaunchedEffect(isFavorite) {
        if (isFavorite) {
            heartScale.animateTo(1.35f, tween(durationMillis = 120))
            heartScale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioHighBouncy))
        }
    }
    ActionCircle(
        label = stringResource(R.string.action_favorite),
        painter = rememberVectorPainter(if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder),
        // Screen readers hear what the tap will do.
        accessibilityLabel = stringResource(
            if (isFavorite) R.string.action_remove_favorite else R.string.action_add_favorite
        ),
        enabled = true,
        highlighted = isFavorite,
        iconScale = heartScale.value,
        onClick = onClick,
    )
}

/**
 * One page of the viewer. Pinch with two fingers or double-tap to zoom; while zoomed in,
 * one finger moves the photo. When not zoomed, one-finger swipes go to the pager.
 */
@Composable
private fun ZoomableImage(image: SowarImage, isCurrentPage: Boolean) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    // Swiping to another page resets the zoom of this one.
    LaunchedEffect(isCurrentPage) {
        if (!isCurrentPage) {
            scale = 1f
            offset = Offset.Zero
        }
    }

    BoxWithConstraints(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = {
                        if (scale > 1f) {
                            scale = 1f
                            offset = Offset.Zero
                        } else {
                            scale = DOUBLE_TAP_ZOOM
                        }
                    },
                )
            }
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    do {
                        val event = awaitPointerEvent()
                        val fingers = event.changes.count { it.pressed }
                        if (fingers >= 2 || scale > 1f) {
                            val newScale = (scale * event.calculateZoom()).coerceIn(1f, MAX_ZOOM)
                            val pan = event.calculatePan()
                            // Don't let the photo be dragged off the screen.
                            val maxX = size.width * (newScale - 1f) / 2f
                            val maxY = size.height * (newScale - 1f) / 2f
                            offset = Offset(
                                (offset.x + pan.x).coerceIn(-maxX, maxX),
                                (offset.y + pan.y).coerceIn(-maxY, maxY),
                            )
                            scale = newScale
                            // Consumed: the pager won't treat this as a swipe.
                            event.changes.forEach { if (it.positionChanged()) it.consume() }
                        }
                    } while (event.changes.any { it.pressed })
                }
            },
    ) {
        val ratio = image.aspectRatio
        CaptionedImage(
            image = image,
            captionStyle = CaptionStyle.Overlay,
            modifier = Modifier
                // Fit the whole photo on screen, keeping its shape.
                .aspectRatio(ratio, matchHeightConstraintsFirst = ratio < maxWidth / maxHeight)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = offset.x
                    translationY = offset.y
                },
        )
    }
}

private const val MAX_ZOOM = 4f
private const val DOUBLE_TAP_ZOOM = 2.5f

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
