package io.toast1ng.springbatchfiletest.sftp.batch

import io.toast1ng.springbatchfiletest.sftp.domain.SftpFileInfo
import org.slf4j.LoggerFactory
import org.springframework.batch.item.ItemProcessor
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths

/**
 * SFTP 파일 정보를 처리하는 ItemProcessor
 * 다운로드 전 로컬 디렉토리를 생성하고, 필요한 검증 수행
 */
class SftpItemProcessor : ItemProcessor<SftpFileInfo, SftpFileInfo> {

    private val logger = LoggerFactory.getLogger(SftpItemProcessor::class.java)

    override fun process(item: SftpFileInfo): SftpFileInfo {
        logger.debug("Processing file: ${item.remotePath}")

        // 로컬 디렉토리가 없으면 생성
        val localFile = File(item.localPath)
        localFile.parentFile?.let { parentDir ->
            if (!parentDir.exists()) {
                Files.createDirectories(parentDir.toPath())
                logger.debug("Created directory: ${parentDir.absolutePath}")
            }
        }

        // 이미 존재하는 파일인지 확인 (선택적: 스킵 또는 덮어쓰기)
        if (localFile.exists()) {
            logger.debug("Local file already exists: ${item.localPath} (will overwrite)")
        }

        // 추가적인 비즈니스 로직 수행 가능
        // 예: 파일 크기 검증, 파일 타입 필터링 등

        return item
    }
}
