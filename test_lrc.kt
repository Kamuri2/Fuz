import com.example.data.*
import java.io.File

fun main() {
    val sampleLrc = """
[00:12.34] Hello World
[01:23.456] Something else
[01:23] No fraction
<02:34.56> Brackets
Plain text
    """.trimIndent()
    val lines = LrcParser.parse(sampleLrc)
    println(lines)
}
