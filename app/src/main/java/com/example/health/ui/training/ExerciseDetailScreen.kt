package com.example.health.ui.training

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
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

    // 解析辅助肌群和分步说明
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
            // ── 图片 / GIF ──
            if (exercise.gifUrl.isNotEmpty()) {
                GifViewer(assetPath = exercise.gifUrl)
                Spacer(modifier = Modifier.height(8.dp))
            } else if (exercise.image.isNotEmpty()) {
                StaticAssetImage(assetPath = exercise.image)
                Spacer(modifier = Modifier.height(8.dp))
            }

            // ── 基本信息标签 ──
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
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        // 步骤编号圆点
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${index + 1}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = step,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

// ── 静态图片（assets） ──
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
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp)),
            contentScale = ContentScale.Fit
        )
    }
}

// ── GIF 动图阅读器（Coil + GifDecoder） ──
@Composable
private fun GifViewer(assetPath: String) {
    val context = LocalContext.current

    // 将 asset 文件复制到缓存目录（Coil 需要文件/URI 路径）
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

    if (cacheFile != null && cacheFile.exists()) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(cacheFile)
                .build(),
            contentDescription = "动作演示",
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp)),
            contentScale = ContentScale.Fit
        )
    }
}

// ── 信息小标签 ──
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
