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
package com.oboenikui.androiddevchallenge.domain

import kotlin.math.max

class CountDownTimer(private val durationMillis: Long) {

    private var start = 0L
    private var stop: Long? = 0L

    val isFinished: Boolean
        get() = durationMillis < ((stop ?: now()) - start) / NANOS_PER_MILLI

    val remain: Long
        get() = max(
            0L,
            durationMillis - ((stop ?: now()) - start) / NANOS_PER_MILLI
        )

    fun start() {
        stop?.let {
            start = now() - (it - start)
            stop = null
        }
    }

    fun pause() {
        stop = now()
    }

    fun reset() {
        start = now()
        stop = stop?.let { start }
    }

    private fun now() = System.nanoTime()

    companion object {
        private const val NANOS_PER_MILLI = 1000000L
    }
}
