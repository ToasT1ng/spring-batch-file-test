package io.toast1ng.springbatchfiletest.sftp

import io.toast1ng.springbatchfiletest.sftp.config.SftpBatchConfig
import org.slf4j.LoggerFactory
import org.springframework.batch.core.Job
import org.springframework.batch.core.JobParametersBuilder
import org.springframework.batch.core.launch.JobLauncher
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
@EnableScheduling
@ConditionalOnProperty(prefix = "sftp", name = ["enabled"], havingValue = "true", matchIfMissing = false)
class SftpScheduler(
    private val jobLauncher: JobLauncher,
    @Qualifier("sftpCopyJob") private val sftpCopyJob: Job,
    private val sftpBatchConfig: SftpBatchConfig
) {
    private val logger = LoggerFactory.getLogger(SftpScheduler::class.java)

    /**
     * application.yml의 sftp.cron 설정에 따라 주기적으로 SFTP Batch Job 실행
     * 기본값: "0 0 * * * *" (매시간 정각)
     */
    @Scheduled(cron = "\${sftp.cron:0 0 * * * *}")
    fun scheduledCopy() {
        logger.info("=== SFTP Batch Job Started ===")
        try {
            val jobParameters = JobParametersBuilder()
                .addString("timestamp", LocalDateTime.now().toString())
                .toJobParameters()

            val jobExecution = jobLauncher.run(sftpCopyJob, jobParameters)
            logger.info("=== SFTP Batch Job Completed with status: ${jobExecution.status} ===")
        } catch (e: Exception) {
            logger.error("=== SFTP Batch Job Failed ===", e)
        } finally {
            // SFTP Client 정리
            sftpBatchConfig.closeSftpClient()
        }
    }
}
