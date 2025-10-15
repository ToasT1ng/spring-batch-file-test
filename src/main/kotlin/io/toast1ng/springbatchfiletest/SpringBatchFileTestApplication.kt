package io.toast1ng.springbatchfiletest

import io.toast1ng.springbatchfiletest.service.MultiFileJobLauncher
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean

@SpringBootApplication
class SpringBatchFileTestApplication {

    @Bean
    fun run(multiFileJobLauncher: MultiFileJobLauncher) = CommandLineRunner {
        multiFileJobLauncher.processAllFiles()
    }
}

fun main(args: Array<String>) {
    runApplication<SpringBatchFileTestApplication>(*args)
}
