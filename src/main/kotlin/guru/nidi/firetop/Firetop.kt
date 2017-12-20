/*
 * Copyright © 2017 Stefan Niederhauser (nidin@gmx.ch)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
@file:JvmName(name = "Firetop")

package guru.nidi.firetop

import guru.nidi.firetop.Color.Companion.BLUE
import guru.nidi.firetop.Color.Companion.DEFAULT
import guru.nidi.firetop.Color.Companion.RED
import guru.nidi.firetop.Color.Companion.WHITE
import guru.nidi.firetop.Color.Companion.YELLOW

private val colors = listOf(WHITE.dark(), RED.bright(), YELLOW.bright(), BLUE.bright())
private val chars = charArrayOf(' ', '.', ':', '^', '*', 'x', 's', 'S', '#', '$')

private val top = Holder<List<Triple<Int, String, Double>>>(emptyList())

fun main(vararg args: String) {
    val delta = if (args.isNotEmpty()) args[0].toIntOrNull() ?: 0 else 0
    run(30, delta, 2)
}

fun run(interval: Long, initDelta: Int, marginBottom: Int) {
    val processors = Runtime.getRuntime().availableProcessors()

    DirectKeyMode().run { dkm ->
        try {
            startMeasureCpu(dkm)

            val dim = dkm.screenSize()
            val height = dim.first - marginBottom
            val width = dim.second
            val size = width * height
            val value = IntArray(size + width + 1)
            var delta = initDelta

            do {
                val key = dkm.key()
                val cpu = top.value.map { it.third }.sum()
                print(Ansi().setCursor(height, 1).eraseLine().color(DEFAULT).sf("Load: %3.0f%%   Norm: %3.0f%%   Delta: %3d%%", cpu, cpu / processors, delta))
                if (key != null) {
                    if (key.isUp()) delta += 5
                    if (key.isDown()) delta -= 5
                }
                var flame = (cpu / processors).toInt() + delta
                flame = Math.max(flame, 0)
                for (i in 0 until width / 9) {
                    value[(Math.random() * width).toInt() + width * (height - 1)] = flame
                }
                for (i in 0 until size) {
                    value[i] = (value[i] + value[i + 1] + value[i + width] + value[i + width + 1]) / 4
                    val col = if (value[i] > 15) 3 else if (value[i] > 9) 2 else if (value[i] > 4) 1 else 0
                    if (i < size - width - 1) {
                        val y = 1 + i / width
                        val x = 1 + i % width
                        print(Ansi().color(colors[col]).setCursor(y, x).s(chars[if (value[i] > 9) 9 else value[i]]))
                        if (x == 1) {
                            top.value.takeWhile { it.third > 20 }.forEach {
                                val cy = 1 + height * (1 - Math.min(1.0, it.third / 100))
                                if (cy.toInt() == y - 1) {
                                    val cx = (width - 12) / 26.0 * (it.second.first().toUpperCase() - 'A')
                                    val name = it.second.substring(0, Math.min(12, it.second.length))
                                    print(Ansi().color(DEFAULT).setCursor(cy.toInt(), cx.toInt()).s(name))
                                }
                            }
                        }
                    }
                }
                Thread.sleep(interval)
            } while (key == null || !key.isEsc() && key.code.toChar() != 'q')
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            printFlush(Ansi().setCursor(1000, 1))
        }
    }
}

private fun startMeasureCpu(dkm: DirectKeyMode) {
    val histLen = 5
    fun emptyHist() = mutableListOf<Double>().apply { while (size < histLen) add(0.0) }

    Thread {
        val hist = mutableMapOf<Int, Pair<String, MutableList<Double>>>()
        while (true) {
            try {
                dkm.exec("ps", "-eo", "pcpu,pid,comm")
                        .split("\n")
                        .drop(1)
                        .forEach { stat ->
                            hist.getOrPut(extractPid(stat)) { Pair(extractCommand(stat), emptyHist()) }
                                    .second.add(extractCpu(stat))
                        }
                hist.values.forEach {
                    it.second.removeAt(0)
                    if (it.second.size < histLen) it.second.add(0.0)
                }
                top.value = hist
                        .map { Triple(it.key, it.value.first, it.value.second.average()) }
                        .sortedByDescending { it.third }
                Thread.sleep(1000)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }.apply {
        isDaemon = true
        start()
    }
}

private class Holder<T>(var value: T)

private fun extractCpu(s: String) = if (s.length > 4) s.substring(0, 4).toDoubleOrNull() ?: 0.0 else 0.0
private fun extractPid(s: String) = if (s.length > 11) s.substring(6, 11).trim().toInt() else 0
private fun extractCommand(s: String) = if (s.length > 13) s.substring(13).let { it.substring(it.lastIndexOf('/') + 1) } else "?"
