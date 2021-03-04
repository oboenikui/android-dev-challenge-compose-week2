/*
 * Copyright 2021 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.oboenikui.androiddevchallenge.ui.component

import android.util.Log
import androidx.compose.animation.Crossfade
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.AnimationVector
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.TwoWayConverter
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateValue
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material.Button
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.FabPosition
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.oboenikui.androiddevchallenge.domain.CountDownTimer
import com.oboenikui.androiddevchallenge.week2.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.Timer
import java.util.TimerTask
import java.util.concurrent.TimeUnit
import kotlin.concurrent.schedule
import kotlin.math.ceil
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.time.toDuration

@OptIn(ExperimentalTime::class)
@Composable
fun CountDownTimerPage(timerViewModel: TimerViewModel = viewModel()) {
    val targetState by timerViewModel.timerState.collectAsState()
    val duration by timerViewModel.duration.collectAsState()
    Crossfade(targetState = targetState) { state ->
        when (state) {
            TimerState.NotStarted -> {
                NotStartedScreen(
                    duration = duration,
                    onValueChange = {
                        timerViewModel.setDuration(it)
                    },
                    onStart = {
                        timerViewModel.start()
                    }
                )
            }
            TimerState.Start -> {
                StartedScreen(
                    currentProgress = {
                        timerViewModel.progress
                    },
                    currentRemain = {
                        timerViewModel.remainDuration
                    },
                    onPause = { timerViewModel.pause() },
                )
            }
            TimerState.Pause -> {
                PausedScreen(
                    progress = (timerViewModel.remainDuration / timerViewModel.duration.value).toFloat(),
                    remain = timerViewModel.remainDuration,
                    onStart = { timerViewModel.start() },
                    onReset = { timerViewModel.reset() },
                )
            }
            TimerState.End -> {
                TimeUpScreen(onStop = { timerViewModel.stop() })
            }
        }
    }
}

@OptIn(ExperimentalTime::class, ExperimentalAnimationApi::class)
@Composable
fun NotStartedScreen(
    duration: Duration,
    onValueChange: (Duration) -> Unit,
    onStart: () -> Unit,
) {
    Scaffold(
        floatingActionButton = {
            if (duration != Duration.ZERO) {
                FloatingActionButton(
                    onClick = onStart,
                ) {
                    val painter = painterResource(id = R.drawable.ic_play_arrow)
                    Image(painter = painter, contentDescription = null)
                }
            }
        },
        floatingActionButtonPosition = FabPosition.Center,
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .padding(bottom = 72.dp),
            contentAlignment = Alignment.Center
        ) {
            TimePicker(
                duration = duration,
                onValueChange = onValueChange,
            )
        }
    }
}

@OptIn(ExperimentalTime::class)
@Preview(widthDp = 240)
@Composable
fun PreviewNotStartedScreen() {
    NotStartedScreen(Duration.ZERO, {}, {})
}

@OptIn(ExperimentalTime::class)
@Composable
fun StartedScreen(
    currentProgress: () -> Float,
    currentRemain: () -> Duration,
    onPause: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val startProgress = currentProgress()

    var remain by remember { mutableStateOf(currentRemain()) }
    var progress by remember { mutableStateOf(startProgress) }

    scope.launch {
        animate(
            initialValue = currentProgress(),
            targetValue = 0f,
            initialVelocity = 0f,
            animationSpec = tween(currentRemain().inMilliseconds.toInt(), easing = LinearEasing),
        ) { _, _ ->
            progress = currentProgress()
            remain = currentRemain()
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = onPause) {
                val painter = painterResource(id = R.drawable.ic_pause)
                Image(painter = painter, contentDescription = null)
            }
        },
        floatingActionButtonPosition = FabPosition.Center,
    ) {
        Column(Modifier.fillMaxSize()) {
            Box(
                Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .padding(16.dp),
                    progress = progress,
                )
                TimeDisplay(duration = remain)
            }
        }
    }
}

@OptIn(ExperimentalTime::class, ExperimentalAnimationApi::class)
@Composable
fun PausedScreen(
    progress: Float,
    remain: Duration,
    onStart: () -> Unit,
    onReset: () -> Unit,
) {

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = onStart) {
                val painter = painterResource(id = R.drawable.ic_play_arrow)
                Image(painter = painter, contentDescription = null)
            }
        },
        floatingActionButtonPosition = FabPosition.Center,
    ) {
        val transition = rememberInfiniteTransition()
        val visible by transition.animateValue<Boolean, AnimationVector1D>(
            initialValue = false,
            targetValue = true,
            typeConverter = TwoWayConverter(
                convertFromVector = {
                    it.value > 0.5
                },
                convertToVector = {
                    if (it) {
                        AnimationVector(1f)
                    } else {
                        AnimationVector(0f)
                    }
                },
            ),
            animationSpec = infiniteRepeatable(
                animation = tween(1000, easing = LinearEasing)
            ),
        )
        Box(
            Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .padding(16.dp),
                progress = progress,
            )

            Box(contentAlignment = Alignment.Center) {
                if (visible) {
                    TimeDisplay(duration = remain)
                }
                TextButton(modifier = Modifier.padding(top = 144.dp), onClick = onReset) {
                    Text("Reset")
                }
            }
        }
    }
}

@OptIn(ExperimentalTime::class)
@Composable
fun TimeUpScreen(
    onStop: () -> Unit,
) {
    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = onStop) {
                val painter = painterResource(id = R.drawable.ic_stop)
                Image(painter = painter, contentDescription = null)
            }
        },
        floatingActionButtonPosition = FabPosition.Center,
    ) {
        val transition = rememberInfiniteTransition()
        val visible by transition.animateValue<Boolean, AnimationVector1D>(
            initialValue = false,
            targetValue = true,
            typeConverter = TwoWayConverter(
                convertFromVector = {
                    it.value > 0.5
                },
                convertToVector = {
                    if (it) {
                        AnimationVector(1f)
                    } else {
                        AnimationVector(0f)
                    }
                },
            ),
            animationSpec = infiniteRepeatable(
                animation = tween(1000, easing = LinearEasing)
            ),
        )
        Box(
            Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            if (visible) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .padding(16.dp),
                    progress = 1f,
                )
            }

            Box(contentAlignment = Alignment.Center) {
                TimeDisplay(duration = Duration.ZERO)
            }
        }
    }
}

@OptIn(ExperimentalTime::class)
@Composable
fun TimeDisplay(duration: Duration) {
    val inSeconds = ceil(duration.inSeconds).toInt()
    val durationText =
        "%02d:%02d:%02d".format(
            inSeconds / 60 / 60,
            inSeconds / 60 % 60,
            inSeconds % 60
        )
    Text(text = durationText, style = TextStyle.Default.copy(fontSize = 64.sp))
}

@OptIn(ExperimentalTime::class)
@Composable
fun TimePicker(
    modifier: Modifier = Modifier,
    duration: Duration = Duration.ZERO,
    onValueChange: (Duration) -> Unit,
) {
    var timerInput by remember {
        mutableStateOf(
            listOf(
                duration.inHours.toInt() / 10,
                duration.inHours.toInt() % 10,
                (duration.inMinutes.toInt() % 60) / 10,
                duration.inMinutes.toInt() % 10,
                (duration.inSeconds.toInt() % 60) / 10,
                duration.inSeconds.toInt() % 10,
            )
        )
    }

    val onClickNumberButton = { i: Int ->
        timerInput = timerInput.slice(1..5) + listOf(i)
        val next = (timerInput[0] * 10 + timerInput[1]).toDuration(TimeUnit.HOURS) +
            (timerInput[2] * 10 + timerInput[3]).toDuration(TimeUnit.MINUTES) +
            (timerInput[4] * 10 + timerInput[5]).toDuration(TimeUnit.SECONDS)
        onValueChange(next)
    }
    val clearTimer = {
        timerInput = listOf(0, 0, 0, 0, 0, 0)
        onValueChange(Duration.ZERO)
    }
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Row(Modifier.padding(bottom = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Row(Modifier.padding(start = 32.dp), verticalAlignment = Alignment.Bottom) {

                Text("%02d".format(timerInput[0] * 10 + timerInput[1]), fontSize = 64.sp)
                Text("h")
                Text("%02d".format(timerInput[2] * 10 + timerInput[3]), fontSize = 64.sp)
                Text("m")
                Text("%02d".format(timerInput[4] * 10 + timerInput[5]), fontSize = 64.sp)
                Text("s")
            }

            TextButton(modifier = Modifier.size(32.dp), onClick = clearTimer) {
                val painter = painterResource(id = R.drawable.ic_close)
                Image(
                    painter = painter,
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(Color.Gray)
                )
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            NumberButton(
                num = 7,
                onClick = onClickNumberButton,
            )
            NumberButton(
                num = 8,
                onClick = onClickNumberButton,
            )
            NumberButton(
                num = 9,
                onClick = onClickNumberButton,
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            NumberButton(
                num = 4,
                onClick = onClickNumberButton,
            )
            NumberButton(
                num = 5,
                onClick = onClickNumberButton,
            )
            NumberButton(
                num = 6,
                onClick = onClickNumberButton,
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            NumberButton(
                num = 1,
                onClick = onClickNumberButton,
            )
            NumberButton(
                num = 2,
                onClick = onClickNumberButton,
            )
            NumberButton(
                num = 3,
                onClick = onClickNumberButton,
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            NumberButton(
                num = 0,
                onClick = onClickNumberButton,
            )
        }
    }
}

@Composable
fun NumberButton(num: Int, onClick: (Int) -> Unit) {

    Button(
        modifier = Modifier
            .sizeIn(maxWidth = 96.dp)
            .padding(8.dp)
            .fillMaxWidth(1f)
            .aspectRatio(1f),
        shape = MaterialTheme.shapes.small.copy(CornerSize(percent = 50)),
        onClick = { onClick(num) },
    ) {
        Text(
            text = num.toString(),
            fontSize = 20.sp,
        )
    }
}

@OptIn(ExperimentalTime::class)
@Preview
@Composable
fun PreviewTimePicker() {
    TimePicker(
        duration = Duration.ZERO,
        onValueChange = {
        }
    )
}

@OptIn(ExperimentalTime::class)
class TimerViewModel : ViewModel() {
    private val _timerEvent = MutableStateFlow(TimerState.NotStarted)
    val timerState: StateFlow<TimerState>
        get() = _timerEvent

    private val _duration = MutableStateFlow(Duration.ZERO)
    val duration: StateFlow<Duration>
        get() = _duration

    private var timerTask: TimerTask? = null

    private var countDownTimer: CountDownTimer = CountDownTimer(0)

    val remainDuration: Duration
        get() = countDownTimer.remain.toDuration(TimeUnit.MILLISECONDS)

    val progress: Float
        get() = (countDownTimer.remain.toDouble() / _duration.value.inMilliseconds).toFloat()

    fun start() {
        Log.d(TAG, "start")
        viewModelScope.launch {
            _timerEvent.emit(TimerState.Start)
        }
        val durationMillis = countDownTimer.remain
        Log.d(TAG, "duration: $durationMillis")
        countDownTimer.start()
        timerTask = Timer(true).schedule(delay = durationMillis) {

            Log.d(TAG, "fixedRateTimer")
            viewModelScope.launch {
                _timerEvent.emit(TimerState.End)
            }
        }
    }

    fun pause() {
        val timerTask = timerTask ?: return
        Log.d(TAG, "pause")
        timerTask.cancel()
        countDownTimer.pause()
        viewModelScope.launch {
            _timerEvent.emit(TimerState.Pause)
        }
    }

    fun reset() {
        Log.d(TAG, "reset")
        countDownTimer.reset()
        viewModelScope.launch {
            _timerEvent.emit(TimerState.NotStarted)
        }
    }

    fun stop() {
        Log.d(TAG, "stop")
        countDownTimer.pause()
        countDownTimer.reset()
        viewModelScope.launch {
            _timerEvent.emit(TimerState.NotStarted)
        }
    }

    fun setDuration(duration: Duration) {
        Log.d(TAG, "setDuration")
        countDownTimer = CountDownTimer(duration.toLongMilliseconds())
        viewModelScope.launch {
            _timerEvent.emit(TimerState.NotStarted)
            _duration.emit(duration)
        }
    }

    companion object {
        const val TAG = "TimerViewModel"
    }
}

enum class TimerState {
    // Initial State or Before start
    NotStarted,

    // Timer working
    Start,

    // Timer is temporary stopping
    Pause,

    // Timer is end
    End,
}
