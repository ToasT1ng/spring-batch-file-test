package io.toast1ng.springbatchfiletest.batch

import com.auth0.jwt.JWT
import com.auth0.jwt.interfaces.DecodedJWT
import com.fasterxml.jackson.databind.ObjectMapper
import io.toast1ng.springbatchfiletest.domain.SomeEntity
import org.springframework.batch.item.ItemProcessor
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

class LogItemProcessor : ItemProcessor<String, SomeEntity> {

    private val objectMapper = ObjectMapper()
    private val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    override fun process(item: String): SomeEntity? {
        // Filter: only process lines containing [REQ]
        if (!item.contains("[REQ]")) {
            return null
        }

        return try {
            parseLogLine(item)
        } catch (e: Exception) {
            println("Failed to process log line: $item. Error: ${e.message}")
            null
        }
    }

    private fun parseLogLine(logLine: String): SomeEntity {
        // Extract ID (T로 시작하는 문자열)
        val idRegex = Regex("(T\\d+)")
        val id = idRegex.find(logLine)?.value
            ?: throw IllegalArgumentException("ID not found in log line")

        // Extract date and time
        val dateTimeRegex = Regex("(\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2})")
        val dateTimeStr = dateTimeRegex.find(logLine)?.value
            ?: throw IllegalArgumentException("DateTime not found in log line")
        val dateTime = LocalDateTime.parse(dateTimeStr, dateTimeFormatter)

        // Extract JWT token (마지막 대괄호 안의 값)
        val jwtRegex = Regex("\\[([eyJa-zA-Z0-9._-]+)]\\s*$")
        val jwt = jwtRegex.find(logLine)?.groupValues?.get(1)
            ?: throw IllegalArgumentException("JWT not found in log line")

        // Decode JWT and extract name
        val name = decodeJwtAndGetName(jwt)

        return SomeEntity(
            id = id,
            dateTime = dateTime,
            jwt = jwt,
            name = name
        )
    }

    private fun decodeJwtAndGetName(token: String): String {
        return try {
            val decodedJWT: DecodedJWT = JWT.decode(token)
            val payload = String(Base64.getUrlDecoder().decode(decodedJWT.payload))
            val jsonNode = objectMapper.readTree(payload)
            jsonNode.get("name")?.asText() ?: "Unknown"
        } catch (e: Exception) {
            println("Failed to decode JWT: ${e.message}")
            "Unknown"
        }
    }
}
