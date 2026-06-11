package com.khangkietson.smarthome.ui.home.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.khangkietson.smarthome.data.model.Device
import com.khangkietson.smarthome.data.model.DeviceType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceControlSheet(
    device: Device,
    onDismiss: () -> Unit,
    onToggle: () -> Unit,
    onBrightnessChange: (Int) -> Unit,
    onFanSpeedChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, end = 24.dp, bottom = 48.dp, top = 8.dp)
        ) {
            Text(
                text = device.name,
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = device.room,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (device.type == DeviceType.DOOR) "Mở cửa" else "Nguồn thiết bị",
                    fontWeight = FontWeight.Medium,
                    fontSize = 18.sp
                )
                Switch(
                    checked = if (device.type == DeviceType.DOOR) device.isOpen else device.isOn,
                    onCheckedChange = { onToggle() }
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            when (device.type) {
                DeviceType.LIGHT -> {
                    Text(
                        text = "Độ sáng: ${device.brightness}%",
                        fontWeight = FontWeight.Medium,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Slider(
                        value = device.brightness.toFloat(),
                        onValueChange = { onBrightnessChange(it.toInt()) },
                        valueRange = 0f..100f,
                        enabled = device.isOn,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                DeviceType.FAN -> {
                    Text(
                        text = "Tốc độ quạt",
                        fontWeight = FontWeight.Medium,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        listOf(1, 2, 3).forEach { speed ->
                            val isSelected = device.speed == speed && device.isOn
                            val label = when (speed) {
                                1 -> "Thấp"
                                2 -> "Trung"
                                3 -> "Cao"
                                else -> ""
                            }
                            
                            Button(
                                onClick = { onFanSpeedChange(speed) },
                                enabled = device.isOn,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                modifier = Modifier.weight(1f).padding(horizontal = 4.dp)
                            ) {
                                Text(text = label)
                            }
                        }
                    }
                }
                DeviceType.DOOR -> {
                    Spacer(modifier = Modifier.height(16.dp))
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (device.isOpen) Icons.Outlined.LockOpen else Icons.Outlined.Lock,
                                contentDescription = null,
                                modifier = Modifier.size(32.dp),
                                tint = if (device.isOpen) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (device.isOpen) "Cửa đang mở" else "Cửa đang khóa",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (device.isOpen) "Hãy đảm bảo an toàn trước khi đóng cửa." else "Cửa chính đã được khóa an toàn.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
