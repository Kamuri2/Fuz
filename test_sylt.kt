import org.jaudiotagger.tag.id3.framebody.FrameBodySYLT

fun main() {
    val body = FrameBodySYLT()
    // List methods using reflection
    body.javaClass.methods.forEach { println(it.name) }
}
