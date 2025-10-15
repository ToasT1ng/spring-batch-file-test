package io.toast1ng.springbatchfiletest.batch

import org.springframework.batch.item.ItemReader
import java.io.BufferedReader
import java.io.File
import java.io.FileReader

class LogFileItemReader(
    private val inputFile: File
) : ItemReader<String> {

    private var reader: BufferedReader? = null
    private var initialized = false

    override fun read(): String? {
        if (!initialized) {
            reader = BufferedReader(FileReader(inputFile))
            initialized = true
        }

        return reader?.readLine()?.takeIf { it.isNotBlank() }
    }

    fun close() {
        reader?.close()
    }
}
