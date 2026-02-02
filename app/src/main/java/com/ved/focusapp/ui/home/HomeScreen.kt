package com.ved.focusapp.ui.home

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ved.focusapp.R
import com.ved.focusapp.data.TimerPhase
import com.ved.focusapp.timer.TimerStateHolder

private const val RING_STROKE_FRACTION = 0.08f

@Composable
fun HomeScreen(
    viewModel: TimerStateHolder,
    modifier: Modifier = Modifier
) {
    val phase by viewModel.phase.collectAsState()
    val remainingSeconds by viewModel.remainingSeconds.collectAsState()
    val isRunning by viewModel.isRunning.collectAsState()
    val sessionsThisRound by viewModel.sessionsThisRound.collectAsState()
    val context = LocalContext.current

    val totalPhaseSeconds = when (phase) {
        TimerPhase.Focus -> viewModel.getFocusMinutes() * 60
        TimerPhase.ShortBreak -> viewModel.getShortBreakMinutes() * 60
        TimerPhase.LongBreak -> viewModel.getLongBreakMinutes() * 60
        TimerPhase.Idle -> 1
    }
    val progress = if (phase == TimerPhase.Idle || totalPhaseSeconds <= 0) 1f
    else (remainingSeconds.toFloat() / totalPhaseSeconds).coerceIn(0f, 1f)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = phaseLabel(phase, context),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(20.dp))
        CountdownRing(
            progress = progress,
            phase = phase,
            isRunning = isRunning,
            modifier = Modifier.size(220.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = viewModel.formatRemaining(remainingSeconds),
            style = MaterialTheme.typography.displayLarge,
            color = MaterialTheme.colorScheme.primary
        )
        if (phase != TimerPhase.Idle) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.sessions_this_round, sessionsThisRound),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
        Spacer(modifier = Modifier.height(32.dp))

        if (phase == TimerPhase.Idle) {
            Button(
                onClick = { viewModel.startPhase(TimerPhase.Focus) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.btn_focus_min, viewModel.getFocusMinutes()))
            }
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = { viewModel.startPhase(TimerPhase.ShortBreak) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.btn_short_break_min, viewModel.getShortBreakMinutes()))
            }
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(
                onClick = { viewModel.startPhase(TimerPhase.LongBreak) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.btn_long_break_min, viewModel.getLongBreakMinutes()))
            }
        } else {
            if (isRunning) {
                Button(
                    onClick = { viewModel.pause() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.btn_pause))
                }
            } else {
                Button(
                    onClick = { viewModel.resume() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.btn_resume))
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(
                onClick = { viewModel.reset() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.btn_reset))
            }
        }
    }
}

@Composable
private fun CountdownRing(
    progress: Float,
    phase: TimerPhase,
    isRunning: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "ring")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )
    val scale = if (phase != TimerPhase.Idle && isRunning) pulse else 1f
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val progressColor = when (phase) {
        TimerPhase.Focus -> MaterialTheme.colorScheme.primary
        TimerPhase.ShortBreak, TimerPhase.LongBreak -> MaterialTheme.colorScheme.tertiary
        TimerPhase.Idle -> MaterialTheme.colorScheme.outline
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
        ) {
            val strokeWidth = size.minDimension * RING_STROKE_FRACTION
            val radius = (size.minDimension - strokeWidth) / 2f
            val center = Offset(size.width / 2f, size.height / 2f)

            // Background ring (full circle)
            drawCircle(
                color = trackColor,
                radius = radius,
                center = center,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            // Progress arc: start from top (-90°), sweep clockwise by progress * 360°
            val sweepDegrees = progress * 360f
            drawArc(
                color = progressColor,
                startAngle = 270f,
                sweepAngle = sweepDegrees,
                useCenter = false,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 2f, radius * 2f),
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }
    }
}

private fun phaseLabel(phase: TimerPhase, context: android.content.Context): String {
    return when (phase) {
        TimerPhase.Idle -> context.getString(R.string.phase_ready)
        TimerPhase.Focus -> context.getString(R.string.phase_focus)
        TimerPhase.ShortBreak -> context.getString(R.string.phase_short_break)
        TimerPhase.LongBreak -> context.getString(R.string.phase_long_break)
    }
}

