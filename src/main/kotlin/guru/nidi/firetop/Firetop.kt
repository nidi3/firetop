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
import guru.nidi.firetop.Color.Companion.RED
import guru.nidi.firetop.Color.Companion.WHITE
import guru.nidi.firetop.Color.Companion.YELLOW
import org.fusesource.jansi.Ansi.ansi

private val COLORS = listOf(WHITE.dark(), RED.bright(), YELLOW.bright(), BLUE.bright())
private val CHARS = charArrayOf(' ', '.', ':', '^', '*', 'x', 's', 'S', '#', '$')

private val top = Holder<List<Triple<Int, String, Double>>>(emptyList())

fun main(vararg args: String) {
    run()
}

fun run() {
    DirectKeyMode().run { dkm ->
        try {
            startMeasureCpu(dkm)

            val dim = dkm.screenSize()
            val height = dim.first - 8
            val width = dim.second
            val size = width * height
            val value = IntArray(size + width + 1)
            var delta = 0

            do {
                val key = dkm.key()
                val cpu = top.value.map { it.third }.sum()
                print(ansi().cursor(height, 1).eraseLine().fgDefault().a(String.format(
                        "Cpu: %3.0f%% Delta: %3d%%           ", cpu, delta)))
                if (key != null) {
                    System.out.flush()
                    if (key.isUp() && delta < 100) delta++
                    if (key.isDown() && delta > -100) delta--
                }
                var flame = cpu.toInt() + delta
                flame = Math.min(Math.max(flame, 0), 100)
                for (i in 0 until width / 9) {
                    value[(Math.random() * width).toInt() + width * (height - 1)] = flame
                }
                for (i in 0 until size) {
                    value[i] = (value[i] + value[i + 1] + value[i + width] + value[i + width + 1]) / 4
                    val col = if (value[i] > 15) 3 else if (value[i] > 9) 2 else if (value[i] > 4) 1 else 0
                    if (i < size - width - 1) {
                        val y = 1 + i / width
                        val x = 1 + i % width
                        print("\u001b[" + COLORS[col].value() + "m")
                        print(ansi().cursor(y, x).a(CHARS[if (value[i] > 9) 9 else value[i]]))
                        if (x == 1) {
                            top.value.takeWhile { it.third > 20 }.forEach {
                                val cy = 1 + height * (1 - Math.min(1.0, it.third / 100))
                                if (cy.toInt() == y - 1) {
                                    val cx = (width - 12) / 26.0 * (it.second.first().toUpperCase() - 'A')
                                    val name = it.second.substring(0, Math.min(12, it.second.length))
                                    print(ansi().fgDefault().cursor(cy.toInt(), cx.toInt()).a(name))
                                }
                            }
                        }
                    }
                }
                Thread.sleep(50)
            } while (key == null || !key.isEsc() && key.code.toChar() != 'q')
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            print(ansi().cursor(1000, 1))
            System.out.flush()
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
