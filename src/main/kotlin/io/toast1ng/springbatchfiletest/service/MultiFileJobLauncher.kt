package io.toast1ng.springbatchfiletest.service

import org.springframework.batch.core.Job
import org.springframework.batch.core.JobParametersBuilder
import org.springframework.batch.core.launch.JobLauncher
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.io.File
import java.time.LocalDateTime

@Service
class MultiFileJobLauncher(
    private val jobLauncher: JobLauncher,
    private val logProcessingJob: Job
) {

    @Value("\${batch.input.folder:input}")
    private lateinit var inputFolderPath: String

    @Value("\${batch.output.folder:output}")
    private lateinit var outputFolderPath: String

    fun processAllFiles() {
        val inputFolder = File(inputFolderPath)

        if (!inputFolder.exists() || !inputFolder.isDirectory) {
            println("Input folder does not exist or is not a directory: $inputFolderPath")
            return
        }

        val files = inputFolder.listFiles { file ->
            file.isFile && (file.extension == "txt" || file.extension == "log")
        } ?: emptyArray()

        if (files.isEmpty()) {
            println("No files found in input folder: $inputFolderPath")
            return
        }

        val outputFolder = File(outputFolderPath)
        outputFolder.mkdirs()

        println("Processing ${files.size} file(s) from $inputFolderPath")

        files.forEach { inputFile ->
            processFile(inputFile)
        }

        println("All files processed successfully")
    }

    private fun processFile(inputFile: File) {
        val outputFileName = "${inputFile.nameWithoutExtension}.csv"
        val outputFile = File(outputFolderPath, outputFileName)

        println("Processing: ${inputFile.name} -> ${outputFile.name}")

        val jobParameters = JobParametersBuilder()
            .addString("inputFile", inputFile.absolutePath)
            .addString("outputFile", outputFile.absolutePath)
            .addString("timestamp", LocalDateTime.now().toString())
            .toJobParameters()

        try {
            val jobExecution = jobLauncher.run(logProcessingJob, jobParameters)
            println("Job finished with status: ${jobExecution.status}")
        } catch (e: Exception) {
            println("Error processing file ${inputFile.name}: ${e.message}")
        }
    }
}
