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
package guru.nidi.firetop

internal class Ansi {
    private val s = StringBuilder()

    fun s(a: String) = s.append(a)
    fun s(a: Char) = s.append(a)
    fun sf(format: String, vararg args: Any?) = s.append(String.format(format, *args))

    fun eraseLine() = apply { esc("K") }

    fun cursorUp(y: Int) = apply { esc("${y}A") }
    fun cursorDown(y: Int) = apply { esc("${y}B") }
    fun cursorRight(x: Int) = apply { esc("${x}C") }
    fun cursorLeft(x: Int) = apply { esc("${x}D") }

    fun setCursor(y: Int, x: Int) = apply { esc("$y;${x}H") }
    fun getCursor() = apply { esc("6n") }

    fun hideCursor() = apply { esc("?25l") }
    fun showCursor() = apply { esc("?25h") }

    fun color(color: Color) = apply { esc("" + color.value() + "m") }

    private fun esc(a: String) = s.append(27.toChar()).append('[').append(a)

    override fun toString() = s.toString()
}

fun printFlush(a: Any) {
    print(a)
    System.out.flush()
}