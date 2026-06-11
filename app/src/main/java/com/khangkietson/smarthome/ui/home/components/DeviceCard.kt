package com.khangkietson.smarthome.ui.home.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.Air
import androidx.compose.material.icons.outlined.DoorFront
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.khangkietson.smarthome.data.model.Device
import com.khangkietson.smarthome.data.model.DeviceType

@Composable
fun DeviceCard(
    device: Device,
    onToggle: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val icon = when (device.type) {
        DeviceType.LIGHT -> Icons.Outlined.Lightbulb
        DeviceType.FAN -> Icons.Outlined.Air
        DeviceType.DOOR -> Icons.Outlined.DoorFront
    }

    val isActive = if (device.type == DeviceType.DOOR) device.isOpen else device.isOn

    val activeColor = when (device.type) {
        DeviceType.LIGHT -> MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
        DeviceType.FAN -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)
        DeviceType.DOOR -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f)
    }
    val inactiveColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    
    val backgroundColor by animateColorAsState(
        targetValue = if (isActive) activeColor else inactiveColor,
        animationSpec = tween(durationMillis = 300),
        label = "bgColor"
    )

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        modifier = modifier
            .padding(8.dp)
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = if (isActive) {
                        when (device.type) {
                            DeviceType.LIGHT -> MaterialTheme.colorScheme.primary
                            DeviceType.FAN -> MaterialTheme.colorScheme.secondary
                            DeviceType.DOOR -> MaterialTheme.colorScheme.tertiary
                        }
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
                
                Switch(
                    checked = isActive,
                    onCheckedChange = { onToggle() }
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = device.name,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = device.room,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            val statusText = when (device.type) {
                DeviceType.LIGHT -> if (device.isOn) "Bật • ${device.brightness}%" else "Tắt"
                DeviceType.FAN -> if (device.isOn) "Bật • Cấp ${device.speed}" else "Tắt"
                DeviceType.DOOR -> if (device.isOpen) "Đang mở" else "Khóa"
            }
            
            Text(
                text = statusText,
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp,
                color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
