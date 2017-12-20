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

import java.io.ByteArrayOutputStream
import java.util.concurrent.LinkedBlockingDeque

internal class DirectKeyMode {
    private val keys = LinkedBlockingDeque<Key>()

    fun run(action: (DirectKeyMode) -> Unit) {
        val ttyConfig = stty("-g")
        try {
            stty("-icanon min 1") // set the console to be character-buffered instead of line-buffered
            stty("-echo") // disable character echoing
            print(Ansi().hideCursor())
            Thread {
                while (true) {
                    try {
                        handleKeyInput()
                        Thread.sleep(20)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }.apply {
                isDaemon = true
                start()
            }
            action(this)
        } finally {
            stty(ttyConfig.trim { it <= ' ' })
            printFlush(Ansi().showCursor().color(Color.DEFAULT).color(Color.DEFAULT.bg()))
        }
    }

    fun key() = keys.poll()

    fun screenSize(): Pair<Int, Int> {
        print(Ansi().cursorDown(1000).cursorRight(1000))
        return cursorPos()
    }

    fun cursorPos(): Pair<Int, Int> {
        printFlush(Ansi().getCursor())
        val pos = keys.takeFirst()
        val parts = pos.info!!.split(";")
        return Pair(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]))
    }

    private fun handleKeyInput() {
        val sin = System.`in`
        if (sin.available() != 0) {
            var key = sin.read()
            if (key == Key.ESC) {
                if (sin.available() == 0) {
                    keys.add(Key.key(Key.ESC))
                } else {
                    val sec = sin.read()
                    if (sec == '['.toInt()) {
                        var buf = ""
                        do {
                            key = sin.read()
                            buf += key.toChar()
                        } while (key.toChar() != '~' && key.toChar() !in 'A'..'Z' && key.toChar() !in 'a'..'z')
                        keys.add(Key.ansi(buf[buf.length - 1], buf.substring(0, buf.length - 1)))
                    } else {
                        keys.add(Key.key(Key.ESC))
                        keys.add(Key.key(sec))
                    }
                }
            } else {
                keys.add(Key.key(key))
            }
        }
    }

    private fun stty(args: String) = exec("sh", "-c", "stty $args < /dev/tty")

    fun exec(vararg cmd: String): String {
        val bout = ByteArrayOutputStream()
        val p = Runtime.getRuntime().exec(cmd)
        p.inputStream.copyTo(bout)
        p.errorStream.copyTo(bout)
        p.waitFor()
        return String(bout.toByteArray())
    }
}

