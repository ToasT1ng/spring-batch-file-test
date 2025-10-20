package io.toast1ng.springbatchfiletest.sftp

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties(prefix = "sftp")
data class SftpCopyProperties(
    var host: String = "localhost",
    var port: Int = 22,
    var username: String = "",
    var password: String? = null,
    var privateKeyPath: String? = null,
    var targets: List<String> = emptyList(),
    var localBaseDir: String = "sftp-downloads",
    var enabled: Boolean = true,
    var cron: String = "0 0 * * * *" // 매시간 정각에 실행
)
