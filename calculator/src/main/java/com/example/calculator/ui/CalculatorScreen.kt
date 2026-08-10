package com.example.calculator.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.calculator.CalcOperator
import com.example.calculator.HistoryItem
import com.example.calculator.ui.theme.AccentPalette
import com.example.calculator.ui.theme.AccentPalettes
import com.example.calculator.ui.theme.AccentNames
import com.example.calculator.ui.theme.AppBackground
import com.example.calculator.ui.theme.DigitBackground
import com.example.calculator.ui.theme.DigitText
import com.example.calculator.ui.theme.DisplayBackground
import com.example.calculator.ui.theme.ErrorRed
import com.example.calculator.ui.theme.ExpressionColor
import com.example.calculator.ui.theme.KeypadBackground
import com.example.calculator.ui.theme.SecondaryText
import com.example.calculator.ui.theme.SheetBackground

@Composable
fun CalculatorScreen(viewModel: CalculatorViewModel) {
    val state by viewModel.uiState.collectAsState()
    CalculatorContent(
        state = state,
        onDigit = viewModel::onDigit,
        onDot = viewModel::onDot,
        onOperator = viewModel::onOperator,
        onEquals = viewModel::onEquals,
        onClear = viewModel::onClear,
        onBackspace = viewModel::onBackspace,
        onToggleHistory = viewModel::onToggleHistory,
        onToggleSettings = viewModel::onToggleSettings,
        onDismissHistory = viewModel::onDismissHistory,
        onDismissSettings = viewModel::onDismissSettings,
        onUseHistory = viewModel::onUseHistory,
        onDeleteHistory = viewModel::onDeleteHistory,
        onClearHistory = viewModel::onClearHistory,
        onSelectAccent = viewModel::onSelectAccent,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CalculatorContent(
    state: UiState,
    onDigit: (Int) -> Unit,
    onDot: () -> Unit,
    onOperator: (CalcOperator) -> Unit,
    onEquals: () -> Unit,
    onClear: () -> Unit,
    onBackspace: () -> Unit,
    onToggleHistory: () -> Unit,
    onToggleSettings: () -> Unit,
    onDismissHistory: () -> Unit,
    onDismissSettings: () -> Unit,
    onUseHistory: (HistoryItem) -> Unit,
    onDeleteHistory: (HistoryItem) -> Unit,
    onClearHistory: () -> Unit,
    onSelectAccent: (Int) -> Unit,
) {
    val accent = AccentPalettes[state.accentIndex.coerceIn(0, AccentPalettes.lastIndex)]

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
            .systemBarsPadding(),
    ) {
        Column(Modifier.fillMaxSize()) {
            DisplayArea(
                state = state,
                onToggleHistory = onToggleHistory,
                onToggleSettings = onToggleSettings,
            )
            Keypad(
                accent = accent,
                onDigit = onDigit,
                onDot = onDot,
                onOperator = onOperator,
                onClear = onClear,
                onBackspace = onBackspace,
                onEquals = onEquals,
            )
        }
    }

    if (state.showHistory) {
        HistorySheet(
            items = state.history,
            accent = accent,
            onDismiss = onDismissHistory,
            onUse = onUseHistory,
            onDelete = onDeleteHistory,
            onClearAll = onClearHistory,
        )
    }

    if (state.showSettings) {
        SettingsDialog(
            currentAccent = state.accentIndex,
            onSelectAccent = onSelectAccent,
            onClearHistory = onClearHistory,
            onDismiss = onDismissSettings,
        )
    }
}

@Composable
private fun ColumnScope.DisplayArea(
    state: UiState,
    onToggleHistory: () -> Unit,
    onToggleSettings: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1.08f)
            .background(DisplayBackground)
            .padding(start = 14.dp, end = 22.dp, top = 8.dp, bottom = 8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onToggleSettings, modifier = Modifier.size(38.dp)) {
                Icon(
                    imageVector = Icons.Filled.Settings,
                    contentDescription = "设置",
                    tint = SecondaryText,
                )
            }
            Spacer(Modifier.weight(1f))
        }

        Spacer(Modifier.weight(1f))

        state.history.take(3).forEach { item ->
            Text(
                text = "${item.expression} ${item.result}",
                color = SecondaryText,
                fontSize = 13.sp,
                lineHeight = 17.sp,
                textAlign = TextAlign.End,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Spacer(Modifier.height(12.dp))

        if (state.expression.isNotEmpty()) {
            Text(
                text = state.expression,
                color = ExpressionColor,
                fontSize = 22.sp,
                textAlign = TextAlign.End,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                softWrap = false,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(4.dp))
        }

        Text(
            text = state.display,
            color = if (state.isError) ErrorRed else Color.White,
            fontSize = displayFontSize(state.display),
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.End,
            maxLines = 1,
            softWrap = false,
            modifier = Modifier.fillMaxWidth(),
        )

        Row(Modifier.fillMaxWidth().padding(top = 8.dp)) {
            IconButton(onClick = onToggleHistory, modifier = Modifier.size(38.dp)) {
                Icon(
                    imageVector = Icons.Filled.KeyboardArrowDown,
                    contentDescription = "历史记录",
                    tint = SecondaryText,
                )
            }
            Spacer(Modifier.weight(1f))
        }
    }
}

@Composable
private fun ColumnScope.Keypad(
    accent: AccentPalette,
    onDigit: (Int) -> Unit,
    onDot: () -> Unit,
    onOperator: (CalcOperator) -> Unit,
    onClear: () -> Unit,
    onBackspace: () -> Unit,
    onEquals: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1.92f)
            .background(KeypadBackground)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Column(
            modifier = Modifier.weight(4f).fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                KeyButton("C", accent.function, Color.White, Modifier.weight(1f), fontSize = 24.sp, onClick = onClear)
                KeyButton("÷", accent.operator, Color.White, Modifier.weight(1f), onClick = { onOperator(CalcOperator.DIVIDE) })
                KeyButton("×", accent.operator, Color.White, Modifier.weight(1f), onClick = { onOperator(CalcOperator.MULTIPLY) })
                KeyButton("⌫", accent.function, Color.White, Modifier.weight(1f), fontSize = 24.sp, onClick = onBackspace)
            }
            Row(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                KeyButton("7", DigitBackground, DigitText, Modifier.weight(1f), onClick = { onDigit(7) })
                KeyButton("8", DigitBackground, DigitText, Modifier.weight(1f), onClick = { onDigit(8) })
                KeyButton("9", DigitBackground, DigitText, Modifier.weight(1f), onClick = { onDigit(9) })
                KeyButton("−", accent.operator, Color.White, Modifier.weight(1f), onClick = { onOperator(CalcOperator.SUBTRACT) })
            }
            Row(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                KeyButton("4", DigitBackground, DigitText, Modifier.weight(1f), onClick = { onDigit(4) })
                KeyButton("5", DigitBackground, DigitText, Modifier.weight(1f), onClick = { onDigit(5) })
                KeyButton("6", DigitBackground, DigitText, Modifier.weight(1f), onClick = { onDigit(6) })
                KeyButton("+", accent.operator, Color.White, Modifier.weight(1f), onClick = { onOperator(CalcOperator.ADD) })
            }
            Row(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                KeyButton("1", DigitBackground, DigitText, Modifier.weight(1f), onClick = { onDigit(1) })
                KeyButton("2", DigitBackground, DigitText, Modifier.weight(1f), onClick = { onDigit(2) })
                KeyButton("3", DigitBackground, DigitText, Modifier.weight(1f), onClick = { onDigit(3) })
                Spacer(Modifier.weight(1f))
            }
            Row(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                KeyButton("0", DigitBackground, DigitText, Modifier.weight(2f), onClick = { onDigit(0) })
                KeyButton(".", accent.operator, Color.White, Modifier.weight(1f), onClick = onDot)
                Spacer(Modifier.weight(1f))
            }
        }
        KeyButton(
            label = "=",
            background = accent.equal,
            contentColor = Color.White,
            modifier = Modifier.weight(1f).fillMaxHeight(),
            fontSize = 30.sp,
            onClick = onEquals,
        )
    }
}

@Composable
private fun KeyButton(
    label: String,
    background: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 26.sp,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(background)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = contentColor,
            fontSize = fontSize,
            fontWeight = FontWeight.SemiBold,
            fontFamily = if (label.all { it.isDigit() || it == '.' }) {
                FontFamily.Monospace
            } else {
                FontFamily.Default
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HistorySheet(
    items: List<HistoryItem>,
    accent: AccentPalette,
    onDismiss: () -> Unit,
    onUse: (HistoryItem) -> Unit,
    onDelete: (HistoryItem) -> Unit,
    onClearAll: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = SheetBackground,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "历史记录",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.weight(1f))
                if (items.isNotEmpty()) {
                    TextButton(onClick = onClearAll) {
                        Text("清空", color = accent.operator)
                    }
                }
            }

            if (items.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(140.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("暂无记录", color = SecondaryText, fontSize = 15.sp)
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp),
                ) {
                    items(
                        items = items,
                        key = { "${it.timestamp}-${it.expression}-${it.result}" },
                    ) { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onUse(item) }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    text = item.expression,
                                    color = SecondaryText,
                                    fontSize = 14.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = "= ${item.result}",
                                    color = Color.White,
                                    fontSize = 20.sp,
                                    fontFamily = FontFamily.Monospace,
                                    maxLines = 1,
                                )
                            }
                            IconButton(onClick = { onDelete(item) }) {
                                Icon(
                                    imageVector = Icons.Filled.Delete,
                                    contentDescription = "删除",
                                    tint = SecondaryText,
                                )
                            }
                        }
                        HorizontalDivider(color = Color(0xFF2A2E33))
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsDialog(
    currentAccent: Int,
    onSelectAccent: (Int) -> Unit,
    onClearHistory: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SheetBackground,
        titleContentColor = Color.White,
        textContentColor = Color.White,
        title = {
            Text("设置", fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("按键配色", color = SecondaryText, fontSize = 14.sp)

                AccentNames.chunked(4).forEach { rowNames ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        rowNames.forEach { name ->
                            val index = AccentNames.indexOf(name)
                            val palette = AccentPalettes[index]
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { onSelectAccent(index) }
                                    .padding(6.dp),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(palette.function)
                                        .then(
                                            if (currentAccent == index) {
                                                Modifier.border(3.dp, Color.White, CircleShape)
                                            } else {
                                                Modifier
                                            },
                                        ),
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = name,
                                    color = if (currentAccent == index) Color.White else SecondaryText,
                                    fontSize = 11.sp,
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(color = Color(0xFF2A2E33))

                TextButton(onClick = { onClearHistory(); onDismiss() }) {
                    Text("清空历史记录", color = ErrorRed)
                }
                Text(
                    text = "极简计算器 · UI 参照小明计算器风格",
                    color = SecondaryText,
                    fontSize = 12.sp,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭", color = AccentPalettes[currentAccent.coerceIn(0, AccentPalettes.lastIndex)].operator)
            }
        },
    )
}

private fun displayFontSize(text: String): TextUnit = when {
    text.length > 15 -> 28.sp
    text.length > 11 -> 34.sp
    text.length > 8 -> 42.sp
    text.length > 5 -> 50.sp
    else -> 58.sp
}
