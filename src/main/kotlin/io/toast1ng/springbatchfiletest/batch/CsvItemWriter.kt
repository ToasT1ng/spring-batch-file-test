package io.toast1ng.springbatchfiletest.batch

import io.toast1ng.springbatchfiletest.domain.SomeEntity
import org.springframework.batch.item.Chunk
import org.springframework.batch.item.ItemWriter
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.time.format.DateTimeFormatter

class CsvItemWriter(
    private val outputFile: File
) : ItemWriter<SomeEntity> {

    private val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    override fun write(chunk: Chunk<out SomeEntity>) {
        // Group by JWT token
        val groupedByJwt = chunk.items.groupBy { it.jwt }

        BufferedWriter(FileWriter(outputFile, false)).use { writer ->
            groupedByJwt.forEach { (_, entities) ->
                entities.forEach { entity ->
                    val line = buildCsvLine(entity)
                    writer.write(line)
                    writer.newLine()
                }
            }
        }
    }

    private fun buildCsvLine(entity: SomeEntity): String {
        return listOf(
            entity.id,
            entity.dateTime.format(dateTimeFormatter),
            entity.jwt,
            entity.name
        ).joinToString(",") { escapeCsv(it) }
    }

    private fun escapeCsv(value: String): String {
        return if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            "\"${value.replace("\"", "\"\"")}\""
        } else {
            "\"$value\""
        }
    }
}
