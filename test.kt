fun main() {
    try {
        val r = Regex("(\\[|<)\\d{1,3}:\\d{1,2}")
        println("VALID")
    } catch (e: Exception) {
        println("ERROR: " + e.message)
    }
}
