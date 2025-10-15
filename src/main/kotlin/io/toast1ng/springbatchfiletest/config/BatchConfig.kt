package io.toast1ng.springbatchfiletest.config

import io.toast1ng.springbatchfiletest.batch.CsvItemWriter
import io.toast1ng.springbatchfiletest.batch.LogFileItemReader
import io.toast1ng.springbatchfiletest.batch.LogItemProcessor
import io.toast1ng.springbatchfiletest.domain.SomeEntity
import org.springframework.batch.core.Job
import org.springframework.batch.core.Step
import org.springframework.batch.core.configuration.annotation.StepScope
import org.springframework.batch.core.job.builder.JobBuilder
import org.springframework.batch.core.launch.support.RunIdIncrementer
import org.springframework.batch.core.repository.JobRepository
import org.springframework.batch.core.step.builder.StepBuilder
import org.springframework.batch.item.ItemProcessor
import org.springframework.batch.item.ItemReader
import org.springframework.batch.item.ItemWriter
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.transaction.PlatformTransactionManager
import java.io.File

@Configuration
class BatchConfig {

    @Bean
    @StepScope
    fun logFileItemReader(
        @Value("#{jobParameters['inputFile']}") inputFilePath: String?
    ): ItemReader<String> {
        val filePath = inputFilePath ?: "input/logs.txt"
        return LogFileItemReader(File(filePath))
    }

    @Bean
    @StepScope
    fun logItemProcessor(): ItemProcessor<String, SomeEntity> {
        return LogItemProcessor()
    }

    @Bean
    @StepScope
    fun csvItemWriter(
        @Value("#{jobParameters['outputFile']}") outputFilePath: String?
    ): ItemWriter<SomeEntity>  {
        val filePath = outputFilePath ?: "output/result.csv"
        val outputFile = File(filePath)
        outputFile.parentFile?.mkdirs()
        return CsvItemWriter(outputFile)
    }

    @Bean
    fun logProcessingStep(
        jobRepository: JobRepository,
        transactionManager: PlatformTransactionManager,
        logFileItemReader: ItemReader<String>,
        logItemProcessor: ItemProcessor<String, SomeEntity>,
        csvItemWriter: ItemWriter<SomeEntity>
    ): Step {
        return StepBuilder("logProcessingStep", jobRepository)
            .chunk<String, SomeEntity>(10, transactionManager)
            .reader(logFileItemReader)
            .processor(logItemProcessor)
            .writer(csvItemWriter)
            .build()
    }

    @Bean
    fun logProcessingJob(
        jobRepository: JobRepository,
        logProcessingStep: Step
    ): Job {
        return JobBuilder("logProcessingJob", jobRepository)
            .incrementer(RunIdIncrementer())
            .start(logProcessingStep)
            .build()
    }
}
