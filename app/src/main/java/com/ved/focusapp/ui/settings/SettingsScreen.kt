package com.ved.focusapp.ui.settings

import android.content.Intent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ved.focusapp.R
import com.ved.focusapp.data.PreferencesStorage
import com.ved.focusapp.dnd.DndHelper
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(
    storage: PreferencesStorage,
    modifier: Modifier = Modifier
) {
    var focusMinutes by remember { mutableIntStateOf(storage.focusMinutes) }
    var shortBreakMinutes by remember { mutableIntStateOf(storage.shortBreakMinutes) }
    var longBreakMinutes by remember { mutableIntStateOf(storage.longBreakMinutes) }
    var autoStartNext by remember { mutableStateOf(storage.autoStartNext) }
    var soundEnabled by remember { mutableStateOf(storage.soundEnabled) }
    var vibrationEnabled by remember { mutableStateOf(storage.vibrationEnabled) }

    LaunchedEffect(Unit) {
        focusMinutes = storage.focusMinutes
        shortBreakMinutes = storage.shortBreakMinutes
        longBreakMinutes = storage.longBreakMinutes
        autoStartNext = storage.autoStartNext
        soundEnabled = storage.soundEnabled
        vibrationEnabled = storage.vibrationEnabled
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        Text(
            text = stringResource(R.string.timer_minutes),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(8.dp))
        DurationRow(
            label = stringResource(R.string.focus_minutes),
            value = focusMinutes,
            range = 1..60,
            onValueChange = { focusMinutes = it; storage.focusMinutes = it }
        )
        DurationRow(
            label = stringResource(R.string.short_break_minutes),
            value = shortBreakMinutes,
            range = 1..30,
            onValueChange = { shortBreakMinutes = it; storage.shortBreakMinutes = it }
        )
        DurationRow(
            label = stringResource(R.string.long_break_minutes),
            value = longBreakMinutes,
            range = 1..60,
            onValueChange = { longBreakMinutes = it; storage.longBreakMinutes = it }
        )

        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = stringResource(R.string.behaviour),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.auto_start_next),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f)
            )
            Switch(
                checked = autoStartNext,
                onCheckedChange = { autoStartNext = it; storage.autoStartNext = it }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = stringResource(R.string.sound_haptics),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.sound),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f)
            )
            Switch(
                checked = soundEnabled,
                onCheckedChange = { soundEnabled = it; storage.soundEnabled = it }
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.vibration),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f)
            )
            Switch(
                checked = vibrationEnabled,
                onCheckedChange = { vibrationEnabled = it; storage.vibrationEnabled = it }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
        val context = LocalContext.current
        val notificationManager = context.getSystemService(android.content.Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        val hasDndAccess = DndHelper.canManageDnd(notificationManager)
        Text(
            text = stringResource(R.string.do_not_disturb),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.dnd_description),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        if (hasDndAccess) {
            Text(
                text = stringResource(R.string.dnd_access_granted),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
        } else {
            OutlinedButton(
                onClick = {
                    DndHelper.intentForDndSettings()?.let { intent ->
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(intent)
                    }
                }
            ) {
                Text(stringResource(R.string.dnd_open_settings))
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = stringResource(R.string.privacy_policy),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.privacy_policy_text),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun DurationRow(
    label: String,
    value: Int,
    range: IntRange,
    onValueChange: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = "$value min",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Slider(
            value = value.toFloat(),
            onValueChange = { onValueChange(it.roundToInt().coerceIn(range)) },
            valueRange = range.first.toFloat()..range.last.toFloat(),
            steps = (range.last - range.first - 1).coerceAtLeast(0),
            modifier = Modifier.weight(1.5f),
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary
            )
        )
    }
}
