package com.ved.focusapp.ui.stats

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ved.focusapp.R
import com.ved.focusapp.data.PreferencesStorage
import android.content.Context
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun StatsScreen(
    storage: PreferencesStorage,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var totalSessions by remember { mutableStateOf(0) }
    var todayMinutes by remember { mutableStateOf(0) }
    var streak by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        totalSessions = storage.totalSessions
        val today = storage.todayKey()
        todayMinutes = storage.getDailyMinutes(today)
        streak = computeStreak(storage, today)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(24.dp)
    ) {
        StatCard(
            title = stringResource(R.string.total_focus_sessions),
            value = totalSessions.toString()
        )
        Spacer(modifier = Modifier.height(16.dp))
        StatCard(
            title = stringResource(R.string.todays_focus_time),
            value = formatDuration(context, todayMinutes)
        )
        Spacer(modifier = Modifier.height(16.dp))
        StatCard(
            title = stringResource(R.string.current_streak),
            value = stringResource(R.string.streak_days, streak)
        )
    }
}

@Composable
private fun StatCard(
    title: String,
    value: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

private fun formatDuration(context: Context, minutes: Int): String {
    return if (minutes < 60) {
        context.getString(R.string.time_min, minutes)
    } else {
        val h = minutes / 60
        val m = minutes % 60
        context.getString(R.string.time_h_min, h, m)
    }
}

private fun computeStreak(storage: PreferencesStorage, todayKey: String): Int {
    if (storage.getDailyMinutes(todayKey) == 0) return 0
    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    var streak = 0
    val cal = Calendar.getInstance()
    for (_i in 0..365) {
        val key = dateFormat.format(cal.time)
        if (storage.getDailyMinutes(key) > 0) {
            streak++
            cal.add(Calendar.DAY_OF_YEAR, -1)
        } else break
    }
    return streak
}
