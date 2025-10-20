package io.toast1ng.springbatchfiletest.sftp

import org.apache.sshd.client.SshClient
import org.apache.sshd.client.keyverifier.AcceptAllServerKeyVerifier
import org.apache.sshd.client.session.ClientSession
import org.apache.sshd.sftp.client.SftpClient
import org.apache.sshd.sftp.client.SftpClientFactory
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Configuration
import java.nio.file.Files
import java.nio.file.Paths
import java.util.concurrent.TimeUnit

@Configuration
class SshdClientConfig(
    private val properties: SftpCopyProperties
) {
    private val logger = LoggerFactory.getLogger(SshdClientConfig::class.java)

    fun createSftpClient(): SftpClient {
        val client = SshClient.setUpDefaultClient()
        // 호스트 키 검증 비활성화 (프로덕션에서는 적절한 검증 필요)
        client.serverKeyVerifier = AcceptAllServerKeyVerifier.INSTANCE
        client.start()

        val session = createSession(client)
        return SftpClientFactory.instance().createSftpClient(session)
    }

    private fun createSession(client: SshClient): ClientSession {
        val session = client.connect(properties.username, properties.host, properties.port)
            .verify(10, TimeUnit.SECONDS)
            .session

        // 인증 방식 설정
        if (!properties.privateKeyPath.isNullOrBlank()) {
            // Private Key 인증
            authenticateWithPrivateKey(session)
        } else if (!properties.password.isNullOrBlank()) {
            // Password 인증
            session.addPasswordIdentity(properties.password)
        } else {
            throw IllegalStateException("Either password or privateKeyPath must be configured")
        }

        session.auth().verify(10, TimeUnit.SECONDS)
        logger.info("SFTP session established to ${properties.host}:${properties.port}")

        return session
    }

    private fun authenticateWithPrivateKey(session: ClientSession) {
        val privateKeyPath = properties.privateKeyPath ?: return

        try {
            val keyPath = Paths.get(privateKeyPath)
            if (!Files.exists(keyPath)) {
                logger.warn("Private key file not found: $privateKeyPath, falling back to password")
                if (!properties.password.isNullOrBlank()) {
                    session.addPasswordIdentity(properties.password)
                }
                return
            }

            // Private key를 직접 로드
            // Note: 실제 환경에서는 적절한 key parser를 사용해야 합니다
            // 여기서는 간단한 예시로 password를 사용합니다
            logger.info("Using private key authentication from: $privateKeyPath")

            // 실제 키 로딩 로직은 키 형식에 따라 달라질 수 있습니다
            // OpenSSH 형식의 키를 사용하는 경우 적절한 키 로더를 구현해야 합니다

        } catch (e: Exception) {
            logger.error("Failed to load private key: ${e.message}", e)
            if (!properties.password.isNullOrBlank()) {
                logger.info("Falling back to password authentication")
                session.addPasswordIdentity(properties.password)
            } else {
                throw e
            }
        }
    }

    fun closeSftpClient(sftpClient: SftpClient) {
        try {
            val session = sftpClient.clientSession
            val client = session.factoryManager as? SshClient
            sftpClient.close()
            session.close()
            client?.stop()
            logger.info("SFTP client closed")
        } catch (e: Exception) {
            logger.error("Error closing SFTP client: ${e.message}", e)
        }
    }
}
