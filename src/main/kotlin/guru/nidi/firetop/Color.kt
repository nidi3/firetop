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

internal class Color private constructor(private val value: Int, private val fore: Boolean = true, private val bright: Boolean = true) {
    companion object {
        val BLACK = Color(0)
        val RED = Color(1)
        val GREEN = Color(2)
        val YELLOW = Color(3)
        val BLUE = Color(4)
        val MAGENTA = Color(5)
        val CYAN = Color(6)
        val WHITE = Color(7)
        val DEFAULT = Color(9, bright = false)
    }

    fun fg() = Color(value, true, bright)
    fun bg() = Color(value, false, bright)
    fun bright() = Color(value, fore, true)
    fun dark() = Color(value, fore, false)

    fun value() = value + (if (fore) 30 else 40) + if (bright) 60 else 0
}
