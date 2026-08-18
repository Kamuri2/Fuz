fun main() {
    val r = Regex("""(?:\[|<)\s*(\d{1,3}):(\d{2})(?:[.:](\d{1,3}))?\s*(?:\]|>)""")
    val t = "[00:12.34] Hello"
    val m = r.findAll(t).toList()
    println("Matches: " + m.size)
    for (x in m) {
        println(x.groupValues)
    }
}
