package io.toast1ng.springbatchfiletest.sftp.domain

import org.apache.sshd.sftp.client.SftpClient

/**
 * SFTP 파일 정보를 담는 도메인 클래스
 * Batch의 chunk 단위로 처리되는 아이템
 */
data class SftpFileInfo(
    val remotePath: String,              // 원격 파일 경로
    val localPath: String,               // 로컬 저장 경로
    val isDirectory: Boolean = false,    // 디렉토리 여부
    val size: Long = 0L,                 // 파일 크기
    val attributes: SftpClient.Attributes? = null  // SFTP 파일 속성
)
