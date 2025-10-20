package io.toast1ng.springbatchfiletest.sftp.batch

import io.toast1ng.springbatchfiletest.sftp.domain.SftpFileInfo
import org.apache.sshd.sftp.client.SftpClient
import org.slf4j.LoggerFactory
import org.springframework.batch.item.ItemReader
import java.nio.file.Paths
import java.util.*

/**
 * SFTP 서버에서 파일 목록을 읽어오는 ItemReader
 * targets 설정에서 지정한 파일/디렉토리를 재귀적으로 탐색하여 파일 리스트를 반환
 */
class SftpItemReader(
    private val sftpClient: SftpClient,
    private val targets: List<String>,
    private val localBaseDir: String
) : ItemReader<SftpFileInfo> {

    private val logger = LoggerFactory.getLogger(SftpItemReader::class.java)
    private val fileQueue: Queue<SftpFileInfo> = LinkedList()
    private var initialized = false

    override fun read(): SftpFileInfo? {
        if (!initialized) {
            initialize()
            initialized = true
        }

        return fileQueue.poll()
    }

    /**
     * 초기화: targets를 순회하며 모든 파일을 큐에 추가
     */
    private fun initialize() {
        logger.info("Initializing SFTP ItemReader with ${targets.size} target(s)")

        targets.forEach { target ->
            try {
                collectFiles(target)
            } catch (e: Exception) {
                logger.error("Failed to collect files from target: $target", e)
            }
        }

        logger.info("Total ${fileQueue.size} file(s) ready to process")
    }

    /**
     * 원격 경로에서 파일 정보를 수집 (재귀)
     */
    private fun collectFiles(remotePath: String) {
        val attributes = sftpClient.stat(remotePath)

        if (attributes.isDirectory) {
            // 디렉토리인 경우 하위 파일 탐색
            collectFilesFromDirectory(remotePath)
        } else {
            // 파일인 경우 큐에 추가
            val localPath = getLocalFilePath(remotePath)
            fileQueue.add(
                SftpFileInfo(
                    remotePath = remotePath,
                    localPath = localPath,
                    isDirectory = false,
                    size = attributes.size,
                    attributes = attributes
                )
            )
            logger.debug("Added file to queue: $remotePath")
        }
    }

    /**
     * 디렉토리 내부의 파일을 재귀적으로 수집
     */
    private fun collectFilesFromDirectory(remoteDir: String) {
        logger.debug("Scanning directory: $remoteDir")

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
                    // 하위 디렉토리 재귀 탐색
                    collectFilesFromDirectory(remoteFilePath)
                } else {
                    // 파일을 큐에 추가
                    val localPath = getLocalFilePath(remoteFilePath)
                    fileQueue.add(
                        SftpFileInfo(
                            remotePath = remoteFilePath,
                            localPath = localPath,
                            isDirectory = false,
                            size = entry.attributes.size,
                            attributes = entry.attributes
                        )
                    )
                    logger.debug("Added file to queue: $remoteFilePath")
                }
            } catch (e: Exception) {
                logger.error("Failed to process: $remoteFilePath", e)
            }
        }
    }

    /**
     * 원격 경로를 로컬 경로로 변환
     */
    private fun getLocalFilePath(remotePath: String): String {
        val normalizedRemotePath = remotePath.removePrefix("/")
        return Paths.get(localBaseDir, normalizedRemotePath).toString()
    }
}
