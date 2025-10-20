package io.toast1ng.springbatchfiletest.sftp

import org.apache.sshd.sftp.client.SftpClient
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

@Service
class SftpCopyService(
    private val properties: SftpCopyProperties,
    private val clientConfig: SshdClientConfig
) {
    private val logger = LoggerFactory.getLogger(SftpCopyService::class.java)

    /**
     * 설정된 모든 타겟을 B 서버에서 A 서버(로컬)로 복사
     */
    fun copyAllTargets() {
        if (!properties.enabled) {
            logger.info("SFTP copy is disabled")
            return
        }

        if (properties.targets.isEmpty()) {
            logger.warn("No targets configured for SFTP copy")
            return
        }

        logger.info("Starting SFTP copy process for ${properties.targets.size} target(s)")

        var sftpClient: SftpClient? = null
        try {
            sftpClient = clientConfig.createSftpClient()

            properties.targets.forEach { target ->
                try {
                    copyTarget(sftpClient, target)
                } catch (e: Exception) {
                    logger.error("Failed to copy target: $target", e)
                }
            }

            logger.info("SFTP copy process completed")
        } catch (e: Exception) {
            logger.error("SFTP connection failed: ${e.message}", e)
            throw e
        } finally {
            sftpClient?.let { clientConfig.closeSftpClient(it) }
        }
    }

    /**
     * 단일 타겟(파일 또는 디렉토리)을 복사
     */
    private fun copyTarget(sftpClient: SftpClient, remotePath: String) {
        logger.info("Processing target: $remotePath")

        val attributes = sftpClient.stat(remotePath)

        if (attributes.isDirectory) {
            copyDirectory(sftpClient, remotePath)
        } else {
            copyFile(sftpClient, remotePath)
        }
    }

    /**
     * 원격 디렉토리를 재귀적으로 복사
     */
    private fun copyDirectory(sftpClient: SftpClient, remoteDir: String) {
        logger.info("Copying directory: $remoteDir")

        val localDir = createLocalDirectory(remoteDir)

        // 디렉토리 내 모든 파일 및 하위 디렉토리 탐색
        val entries = sftpClient.readDir(remoteDir)

        entries.forEach { entry ->
            val fileName = entry.filename
            // . 과 .. 은 스킵
            if (fileName == "." || fileName == "..") {
                return@forEach
            }

            val remoteFilePath = "$remoteDir/$fileName"

            try {
                if (entry.attributes.isDirectory) {
                    // 하위 디렉토리 재귀 복사
                    copyDirectory(sftpClient, remoteFilePath)
                } else {
                    // 파일 복사
                    copyFile(sftpClient, remoteFilePath)
                }
            } catch (e: Exception) {
                logger.error("Failed to copy: $remoteFilePath", e)
            }
        }

        logger.info("Directory copied: $remoteDir -> $localDir")
    }

    /**
     * 단일 파일 복사
     */
    private fun copyFile(sftpClient: SftpClient, remoteFilePath: String) {
        val localFile = getLocalFilePath(remoteFilePath)

        // 부모 디렉토리가 없으면 생성
        localFile.parentFile?.mkdirs()

        logger.info("Downloading: $remoteFilePath -> ${localFile.absolutePath}")

        sftpClient.read(remoteFilePath).use { inputStream ->
            FileOutputStream(localFile).use { outputStream ->
                inputStream.copyTo(outputStream)
            }
        }

        logger.info("File downloaded: ${localFile.absolutePath} (${localFile.length()} bytes)")
    }

    /**
     * 원격 디렉토리 경로에 대응하는 로컬 디렉토리 생성
     */
    private fun createLocalDirectory(remotePath: String): Path {
        val localPath = getLocalFilePath(remotePath).toPath()
        Files.createDirectories(localPath)
        return localPath
    }

    /**
     * 원격 경로를 로컬 경로로 변환
     */
    private fun getLocalFilePath(remotePath: String): File {
        // 원격 경로를 로컬 base directory 하위에 매핑
        val normalizedRemotePath = remotePath.removePrefix("/")
        return Paths.get(properties.localBaseDir, normalizedRemotePath).toFile()
    }
}
