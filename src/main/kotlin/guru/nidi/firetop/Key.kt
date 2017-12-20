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

internal class Key(val code: Int, val ansi: Char, val info: String?) {
    companion object {
        val ESC = 27
        fun key(code: Int) = Key(code, 0.toChar(), null)
        fun ansi(type: Char, info: String) = Key(-1, type, info)
    }

    fun isLeft() = ansi == 'D'
    fun isRight() = ansi == 'C'
    fun isUp() = ansi == 'A'
    fun isDown() = ansi == 'B'
    fun isEsc() = code == ESC
    fun isEnter() = code == 10
    fun isBackspace() = code == 127
    fun isHome() = isTilde(1)
    fun isInsert() = isTilde(2)
    fun isDelete() = isTilde(3)
    fun isEnd() = isTilde(4)
    fun isPageUp() = isTilde(5)
    fun isPageDown() = isTilde(6)
    fun isBasic() = code in 32..126

    private fun isTilde(code: Int) = ansi == '~' && Integer.toString(code).equals(info, ignoreCase = true)

    override fun toString() = if (code >= 0) Integer.toString(code) else "ansi($info)$ansi"
}