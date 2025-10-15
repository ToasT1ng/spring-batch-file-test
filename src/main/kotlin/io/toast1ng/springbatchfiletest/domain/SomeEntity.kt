package io.toast1ng.springbatchfiletest.domain

import java.time.LocalDateTime

data class SomeEntity(
    val id: String,
    val dateTime: LocalDateTime,
    val jwt: String,
    val name: String
)
