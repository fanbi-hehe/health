package com.example.health.ui.training

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.ImageLoader
import coil.compose.SubcomposeAsyncImage
import coil.decode.GifDecoder
import coil.request.ImageRequest
import com.example.health.data.local.entity.ExerciseLibrary
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseDetailScreen(
    exercise: ExerciseLibrary,
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val gson = remember { Gson() }
    var showFullscreen by remember { mutableStateOf(false) }

    val secondaryMuscles: List<String> = remember(exercise.secondaryMuscles) {
        try {
            val listType = object : TypeToken<List<String>>() {}.type
            gson.fromJson(exercise.secondaryMuscles, listType) ?: emptyList()
        } catch (_: Exception) { emptyList() }
    }
    val steps: List<String> = remember(exercise.instructionSteps) {
        try {
            val listType = object : TypeToken<List<String>>() {}.type
            gson.fromJson(exercise.instructionSteps, listType) ?: emptyList()
        } catch (_: Exception) { emptyList() }
    }

    // 全屏查看弹窗
    if (showFullscreen) {
        FullscreenMediaDialog(
            exercise = exercise,
            onDismiss = { showFullscreen = false }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(exercise.name) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // ── 媒体展示（可点击放大） ──
            val hasGif = exercise.gifUrl.isNotEmpty()
            val hasImage = exercise.image.isNotEmpty()

            if (hasGif || hasImage) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    if (hasGif) {
                        AnimatedGifViewer(assetPath = exercise.gifUrl)
                    } else {
                        StaticAssetImage(assetPath = exercise.image)
                    }
                    // 放大按钮
                    IconButton(
                        onClick = { showFullscreen = true },
                        modifier = Modifier.align(Alignment.BottomEnd)
                    ) {
                        Icon(
                            Icons.Default.ZoomIn,
                            contentDescription = "放大查看",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // ── 信息标签 ──
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                InfoChip("部位", exercise.bodyPart)
                if (exercise.equipment.isNotEmpty()) InfoChip("器械", exercise.equipment)
                if (exercise.target.isNotEmpty()) InfoChip("目标", exercise.target)
            }

            if (secondaryMuscles.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "辅助肌群: ${secondaryMuscles.joinToString("、")}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── 完整说明 ──
            if (exercise.instructions.isNotEmpty()) {
                Text("动作说明", style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(4.dp))
                Text(exercise.instructions, style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(16.dp))
            }

            // ── 分步教学 ──
            if (steps.isNotEmpty()) {
                Text("分步教学", style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(8.dp))
                steps.forEachIndexed { index, step ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("${index + 1}", style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(step, style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

// ──────────────────────────────────────────────────────────
// 全屏查看弹窗
// ──────────────────────────────────────────────────────────
@Composable
private fun FullscreenMediaDialog(exercise: ExerciseLibrary, onDismiss: () -> Unit) {
    val context = LocalContext.current

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background.copy(alpha = 0.95f))
                .clickable(onClick = onDismiss)
        ) {
            if (exercise.gifUrl.isNotEmpty()) {
                AnimatedGifViewer(assetPath = exercise.gifUrl)
            } else if (exercise.image.isNotEmpty()) {
                StaticAssetImage(assetPath = exercise.image)
            }

            // 关闭按钮
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)
            ) {
                Icon(Icons.Default.Close, contentDescription = "关闭",
                    tint = MaterialTheme.colorScheme.onBackground)
            }
        }
    }
}

// ──────────────────────────────────────────────────────────
// 静态图片
// ──────────────────────────────────────────────────────────
@Composable
private fun StaticAssetImage(assetPath: String) {
    val context = LocalContext.current
    val bitmap = remember(assetPath) {
        try {
            context.assets.open(assetPath).use { BitmapFactory.decodeStream(it) }
        } catch (_: Exception) { null }
    }
    if (bitmap != null) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = null,
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)),
            contentScale = ContentScale.FillWidth
        )
    }
}

// ──────────────────────────────────────────────────────────
// GIF 动图（自定义 ImageLoader + GifDecoder）
// ──────────────────────────────────────────────────────────
@Composable
private fun AnimatedGifViewer(assetPath: String) {
    val context = LocalContext.current

    val cacheFile = remember(assetPath) {
        try {
            val input = context.assets.open(assetPath)
            val output = java.io.File(context.cacheDir, assetPath.substringAfterLast('/'))
            if (!output.exists()) {
                output.parentFile?.mkdirs()
                output.outputStream().use { out -> input.copyTo(out) }
            }
            input.close()
            output
        } catch (_: Exception) { null }
    }

    val gifImageLoader = remember {
        ImageLoader.Builder(context)
            .components {
                add(GifDecoder.Factory())
            }
            .build()
    }

    if (cacheFile != null && cacheFile.exists()) {
        SubcomposeAsyncImage(
            model = ImageRequest.Builder(context)
                .data(cacheFile)
                .build(),
            contentDescription = "动作演示",
            imageLoader = gifImageLoader,
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)),
            contentScale = ContentScale.FillWidth
        )
    }
}

// ──────────────────────────────────────────────────────────
// 信息小标签
// ──────────────────────────────────────────────────────────
@Composable
private fun InfoChip(label: String, value: String) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = "$label: $value",
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelMedium
        )
    }
}
