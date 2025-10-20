package io.toast1ng.springbatchfiletest.sftp.batch

import io.toast1ng.springbatchfiletest.sftp.domain.SftpFileInfo
import org.apache.sshd.sftp.client.SftpClient
import org.slf4j.LoggerFactory
import org.springframework.batch.item.Chunk
import org.springframework.batch.item.ItemWriter
import java.io.File
import java.io.FileOutputStream

/**
 * SFTP 서버에서 파일을 다운로드하는 ItemWriter
 * Chunk 단위로 여러 파일을 한번에 처리
 */
class SftpItemWriter(
    private val sftpClient: SftpClient
) : ItemWriter<SftpFileInfo> {

    private val logger = LoggerFactory.getLogger(SftpItemWriter::class.java)

    override fun write(chunk: Chunk<out SftpFileInfo>) {
        chunk.forEach { fileInfo ->
            try {
                downloadFile(fileInfo)
            } catch (e: Exception) {
                logger.error("Failed to download file: ${fileInfo.remotePath}", e)
                // 실패한 파일은 로그만 남기고 계속 진행
                // 필요시 예외를 다시 던져서 배치 실패 처리 가능
            }
        }
    }

    /**
     * 단일 파일 다운로드
     */
    private fun downloadFile(fileInfo: SftpFileInfo) {
        val localFile = File(fileInfo.localPath)

        logger.info("Downloading: ${fileInfo.remotePath} -> ${localFile.absolutePath}")

        sftpClient.read(fileInfo.remotePath).use { inputStream ->
            FileOutputStream(localFile).use { outputStream ->
                inputStream.copyTo(outputStream)
            }
        }

        logger.info("Downloaded: ${localFile.name} (${localFile.length()} bytes)")
    }
}
