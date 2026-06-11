package com.khangkietson.smarthome.ui.gesture

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import android.content.Context
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BackHand
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import com.khangkietson.smarthome.R
import androidx.compose.material.icons.filled.QuestionMark
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.mediapipe.tasks.vision.gesturerecognizer.GestureRecognizerResult
import com.khangkietson.smarthome.data.model.HandGesture
import com.khangkietson.smarthome.gesture.GestureClassifier
import com.khangkietson.smarthome.ui.gesture.components.CameraPreview
import com.khangkietson.smarthome.ui.gesture.components.HandOverlay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GestureScreen(
    viewModel: GestureViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    var detectionResult by remember { mutableStateOf<GestureRecognizerResult?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val classifier = remember { GestureClassifier() }

    val context = LocalContext.current
    val sharedPreferences = remember { context.getSharedPreferences("smart_home_prefs", Context.MODE_PRIVATE) }
    var showTutorial by remember {
        mutableStateOf(!sharedPreferences.getBoolean("has_shown_gesture_tutorial", false))
    }
    var showSettings by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Điều khiển bằng cử chỉ",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                },
                actions = {
                    IconButton(onClick = { showSettings = true }) {
                        Icon(
                            imageVector = Icons.Outlined.Settings,
                            contentDescription = "Cấu hình cử chỉ",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            if (errorMessage != null) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                ) {
                    Text(
                        text = errorMessage ?: "",
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }

            Box(
                modifier = Modifier
                    .weight(1.0f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(4.dp),
                contentAlignment = Alignment.Center
            ) {
                if (!showTutorial) {
                    CameraPreview(
                        onResults = { result, _ ->
                            detectionResult = result
                            val command = classifier.classify(result)
                            val confirmed = classifier.getConfirmedGesture()
                            if (confirmed != uiState.currentGesture || command.confidence != uiState.confidence) {
                                viewModel.onGestureDetected(confirmed, command.confidence)
                            }
                        },
                        onError = { error ->
                            errorMessage = error
                        },
                        modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp))
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Camera sẽ mở sau khi đóng hướng dẫn",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 14.sp
                        )
                    }
                }

                HandOverlay(
                    resultProvider = { detectionResult },
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val gestureIcon = when (uiState.currentGesture) {
                        HandGesture.OPEN_PALM -> Icons.Filled.BackHand
                        HandGesture.CLOSED_FIST -> ImageVector.vectorResource(id = R.drawable.ic_hand_fist)
                        HandGesture.THUMB_UP -> Icons.Filled.ThumbUp
                        HandGesture.THUMB_DOWN -> Icons.Filled.ThumbDown
                        HandGesture.VICTORY -> ImageVector.vectorResource(id = R.drawable.ic_hand_peace)
                        HandGesture.POINTING_UP -> Icons.Filled.TouchApp
                        HandGesture.TWO_FINGERS_CLOSED -> Icons.Filled.TouchApp
                        HandGesture.NONE -> Icons.Filled.QuestionMark
                    }
                    val gestureName = when (uiState.currentGesture) {
                        HandGesture.OPEN_PALM -> "Xòe bàn tay"
                        HandGesture.CLOSED_FIST -> "Nắm tay"
                        HandGesture.THUMB_UP -> "Giơ ngón cái"
                        HandGesture.THUMB_DOWN -> "Úp ngón cái"
                        HandGesture.VICTORY -> "Ký hiệu chữ V"
                        HandGesture.POINTING_UP -> "Chỉ lên trời"
                        HandGesture.TWO_FINGERS_CLOSED -> "Giơ 2 ngón khép"
                        HandGesture.NONE -> "Không có cử chỉ"
                    }

                    val gestureMappings by viewModel.gestureMappings.collectAsState()
                    val actionMapped = if (uiState.currentGesture == HandGesture.NONE) {
                        "Hành động: Chờ cử chỉ hợp lệ..."
                    } else {
                        "Hành động: ${gestureMappings[uiState.currentGesture]?.description ?: "Không làm gì"}"
                    }

                    Icon(
                        imageVector = gestureIcon,
                        contentDescription = null,
                        modifier = Modifier.size(36.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "Cử chỉ hiện tại: $gestureName",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = actionMapped,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Lịch sử điều khiển",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(0.8f).fillMaxWidth()
            ) {
                if (uiState.commandHistory.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "Chưa có lệnh nào được thực thi",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(12.dp)
                    ) {
                        items(uiState.commandHistory) { historyItem ->
                            Text(
                                text = historyItem,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    if (showTutorial) {
        AlertDialog(
            onDismissRequest = { /* Không cho phép đóng bằng cách nhấn bên ngoài */ },
            title = {
                Text(
                    text = "Hướng dẫn sử dụng cử chỉ",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Column {
                    Text(
                        text = "Các cử chỉ mặc định được lập trình bao gồm:",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    val tutorialSteps = listOf(
                        Icons.Filled.BackHand to "Xòe bàn tay: Bật tất cả bóng đèn",
                        ImageVector.vectorResource(id = R.drawable.ic_hand_fist) to "Nắm tay: Tắt tất cả bóng đèn",
                        Icons.Filled.ThumbUp to "Giơ ngón cái: Bật quạt phòng khách",
                        Icons.Filled.ThumbDown to "Úp ngón cái: Tắt quạt phòng khách",
                        ImageVector.vectorResource(id = R.drawable.ic_hand_peace) to "Ký hiệu chữ V: Mở cửa chính",
                        Icons.Filled.TouchApp to "Chỉ lên trời: Tăng tốc độ quạt",
                        Icons.Filled.TouchApp to "Giơ 2 ngón khép: Đóng cửa chính"
                    )
                    tutorialSteps.forEach { (icon, text) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = text,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Bạn có thể thay đổi cử chỉ ở nút cấu hình (biểu tượng bánh răng) trên góc phải màn hình.",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        sharedPreferences.edit().putBoolean("has_shown_gesture_tutorial", true).apply()
                        showTutorial = false
                    }
                ) {
                    Text("OK")
                }
            }
        )
    }

    if (showSettings) {
        ModalBottomSheet(
            onDismissRequest = { showSettings = false }
        ) {
            GestureSettingsContent(viewModel = viewModel)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GestureSettingsContent(
    viewModel: GestureViewModel
) {
    val gestureMappings by viewModel.gestureMappings.collectAsState()
    val gestures = listOf(
        HandGesture.OPEN_PALM to ("Xòe bàn tay" to Icons.Filled.BackHand),
        HandGesture.CLOSED_FIST to ("Nắm tay" to ImageVector.vectorResource(id = R.drawable.ic_hand_fist)),
        HandGesture.THUMB_UP to ("Giơ ngón cái" to Icons.Filled.ThumbUp),
        HandGesture.THUMB_DOWN to ("Úp ngón cái" to Icons.Filled.ThumbDown),
        HandGesture.VICTORY to ("Ký hiệu chữ V" to ImageVector.vectorResource(id = R.drawable.ic_hand_peace)),
        HandGesture.POINTING_UP to ("Chỉ lên trời" to Icons.Filled.TouchApp),
        HandGesture.TWO_FINGERS_CLOSED to ("Giơ 2 ngón khép" to Icons.Filled.TouchApp)
    )
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .padding(bottom = 24.dp)
    ) {
        Text(
            text = "Cấu hình cử chỉ",
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        LazyColumn {
            items(gestures) { (gesture, pair) ->
                val (labelText, icon) = pair
                var expanded by remember { mutableStateOf(false) }
                val currentAction = gestureMappings[gesture] ?: GestureAction.NONE
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = labelText,
                            fontWeight = FontWeight.Medium,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    
                    Box {
                        Button(
                            onClick = { expanded = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(text = currentAction.description, fontSize = 13.sp)
                        }
                        
                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            GestureAction.values().forEach { action ->
                                DropdownMenuItem(
                                    text = { Text(text = action.description) },
                                    onClick = {
                                        viewModel.updateGestureMapping(gesture, action)
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
