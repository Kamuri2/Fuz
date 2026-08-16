package com.example.test

import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.id3.AbstractID3v2Tag
import org.jaudiotagger.tag.id3.framebody.FrameBodySYLT
import org.jaudiotagger.tag.id3.framebody.FrameBodyUSLT
import java.io.File

fun testTags() {
    val file = File("test.mp3") // Just to see if it compiles
    val audioFile = AudioFileIO.read(file)
    val tag = audioFile.tag
    if (tag is AbstractID3v2Tag) {
        val syltFrame = tag.getFirstField("SYLT")
        if (syltFrame != null) {
            val body = syltFrame.body
            if (body is FrameBodySYLT) {
                // it works
            }
        }
    }
}
