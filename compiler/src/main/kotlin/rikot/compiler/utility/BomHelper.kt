@file:Suppress("ObjectPropertyName", "LiftReturnOrAssignment")
package rikot.compiler.utility

import java.io.File
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets.*

private val UTF_32BE: Charset get() = Charset.forName("UTF-32BE")
private val UTF_32LE: Charset get() = Charset.forName("UTF-32LE")

private const val `00`: Byte = 0
private const val BB: Byte = -69
private const val BF: Byte = -65
private const val EF: Byte = -17
private const val FE: Byte = -2
private const val FF: Byte = -1

/**
 * Reads the entire content of this URL as a String using the charset specified by BOM or UTF-8 no BOM is found.
 * Support the same set of BOMs as in the Kotlin compiler.
 *
 * See also [the implementation in Kotlin compiler](https://git.io/JJF5k)
 */
fun File.readTextDetectingBom(): String {
  val a = readBytes()
  val n = a.size

  when {
    n >= 2 -> when (a[0]) {
      `00` -> when {
        n >= 4 && a[1] == `00` && a[2] == FE && a[3] == FF ->
          // UTF-32 (BE): 00 00 FE FF
          return String(a, 4, n - 4, UTF_32BE)
      }
      EF -> when {
        n >= 3 && a[1] == BB && a[2] == BF ->
          // UTF-8: EF BB BF
          return String(a, 3, n - 3, UTF_8)
      }
      FE -> when (a[1]) {
        FF ->
          // UTF-16 (BE): FE FF
          return String(a, 2, n - 2, UTF_16BE)
      }
      FF -> when (a[1]) {
        FE -> when {
          n >= 4 && a[2] == `00` && a[3] == `00` ->
            // UTF-32 (LE): FF FE 00 00
            return String(a, 4, n - 4, UTF_32LE)
          else ->
            // UTF-16 (LE): FF FE
            return String(a, 2, n - 2, UTF_16LE)
        }
      }
    }
  }
  return String(a, UTF_8)
}
