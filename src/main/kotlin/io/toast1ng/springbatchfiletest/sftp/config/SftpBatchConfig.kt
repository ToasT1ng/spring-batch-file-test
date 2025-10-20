package io.toast1ng.springbatchfiletest.sftp.config

import io.toast1ng.springbatchfiletest.sftp.SftpCopyProperties
import io.toast1ng.springbatchfiletest.sftp.SshdClientConfig
import io.toast1ng.springbatchfiletest.sftp.batch.SftpItemProcessor
import io.toast1ng.springbatchfiletest.sftp.batch.SftpItemReader
import io.toast1ng.springbatchfiletest.sftp.batch.SftpItemWriter
import io.toast1ng.springbatchfiletest.sftp.domain.SftpFileInfo
import org.apache.sshd.sftp.client.SftpClient
import org.slf4j.LoggerFactory
import org.springframework.batch.core.Job
import org.springframework.batch.core.Step
import org.springframework.batch.core.job.builder.JobBuilder
import org.springframework.batch.core.launch.support.RunIdIncrementer
import org.springframework.batch.core.repository.JobRepository
import org.springframework.batch.core.step.builder.StepBuilder
import org.springframework.batch.item.ItemProcessor
import org.springframework.batch.item.ItemReader
import org.springframework.batch.item.ItemWriter
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.transaction.PlatformTransactionManager

@Configuration
@ConditionalOnProperty(prefix = "sftp", name = ["enabled"], havingValue = "true", matchIfMissing = false)
class SftpBatchConfig(
    private val properties: SftpCopyProperties,
    private val clientConfig: SshdClientConfig
) {
    private val logger = LoggerFactory.getLogger(SftpBatchConfig::class.java)

    /**
     * SFTP Client를 ThreadLocal로 관리
     * 각 Job 실행마다 새로운 클라이언트 생성
     */
    private val sftpClientHolder = ThreadLocal<SftpClient>()

    @Bean
    fun sftpItemReader(): ItemReader<SftpFileInfo> {
        val sftpClient = getSftpClient()
        return SftpItemReader(
            sftpClient = sftpClient,
            targets = properties.targets,
            localBaseDir = properties.localBaseDir
        )
    }

    @Bean
    fun sftpItemProcessor(): ItemProcessor<SftpFileInfo, SftpFileInfo> {
        return SftpItemProcessor()
    }

    @Bean
    fun sftpItemWriter(): ItemWriter<SftpFileInfo> {
        val sftpClient = getSftpClient()
        return SftpItemWriter(sftpClient)
    }

    @Bean
    fun sftpCopyStep(
        jobRepository: JobRepository,
        transactionManager: PlatformTransactionManager,
        @Qualifier("sftpItemReader") reader: ItemReader<SftpFileInfo>,
        @Qualifier("sftpItemProcessor") processor: ItemProcessor<SftpFileInfo, SftpFileInfo>,
        @Qualifier("sftpItemWriter") writer: ItemWriter<SftpFileInfo>
    ): Step {
        return StepBuilder("sftpCopyStep", jobRepository)
            .chunk<SftpFileInfo, SftpFileInfo>(10, transactionManager)
            .reader(reader)
            .processor(processor)
            .writer(writer)
            .build()
    }

    @Bean
    fun sftpCopyJob(
        jobRepository: JobRepository,
        @Qualifier("sftpCopyStep") sftpCopyStep: Step
    ): Job {
        return JobBuilder("sftpCopyJob", jobRepository)
            .incrementer(RunIdIncrementer())
            .start(sftpCopyStep)
            .build()
    }

    /**
     * SFTP Client를 가져오거나 새로 생성
     */
    private fun getSftpClient(): SftpClient {
        var client = sftpClientHolder.get()
        if (client == null || !client.isOpen) {
            logger.info("Creating new SFTP client")
            client = clientConfig.createSftpClient()
            sftpClientHolder.set(client)
        }
        return client
    }

    /**
     * SFTP Client 정리
     */
    fun closeSftpClient() {
        sftpClientHolder.get()?.let { client ->
            clientConfig.closeSftpClient(client)
            sftpClientHolder.remove()
        }
    }
}
