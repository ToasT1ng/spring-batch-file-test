package io.toast1ng.springbatchfiletest.sftp

import io.toast1ng.springbatchfiletest.sftp.batch.SftpItemProcessor
import io.toast1ng.springbatchfiletest.sftp.batch.SftpItemReader
import io.toast1ng.springbatchfiletest.sftp.batch.SftpItemWriter
import io.toast1ng.springbatchfiletest.sftp.domain.SftpFileInfo
import org.apache.sshd.sftp.client.SftpClient
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.mockito.Mockito.*
import org.springframework.batch.item.Chunk
import java.io.ByteArrayInputStream
import java.io.File
import java.nio.file.Path
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * SFTP Batch 컴포넌트 통합 테스트
 * Mock SFTP Client를 사용하여 Reader, Processor, Writer 테스트
 */
class SftpBatchIntegrationTest {

    @TempDir
    lateinit var tempDir: Path

    private lateinit var mockSftpClient: SftpClient
    private lateinit var localBaseDir: String

    @BeforeEach
    fun setUp() {
        mockSftpClient = mock(SftpClient::class.java)
        localBaseDir = tempDir.toString()
    }

    @AfterEach
    fun tearDown() {
        // Cleanup if needed
    }

    @Test
    fun `test SftpItemReader reads single file`() {
        // Given
        val remotePath = "/remote/test.txt"
        val targets = listOf(remotePath)

        val mockAttributes = mock(SftpClient.Attributes::class.java)
        `when`(mockAttributes.isDirectory).thenReturn(false)
        `when`(mockAttributes.size).thenReturn(100L)
        `when`(mockSftpClient.stat(remotePath)).thenReturn(mockAttributes)

        val reader = SftpItemReader(mockSftpClient, targets, localBaseDir)

        // When
        val item1 = reader.read()
        val item2 = reader.read()

        // Then
        assertNotNull(item1)
        assertEquals(remotePath, item1.remotePath)
        assertEquals(false, item1.isDirectory)
        assertEquals(100L, item1.size)
        assertEquals(null, item2) // No more items
    }

    @Test
    fun `test SftpItemReader reads directory with files`() {
        // Given
        val remoteDir = "/remote/logs"
        val targets = listOf(remoteDir)

        val mockDirAttributes = mock(SftpClient.Attributes::class.java)
        `when`(mockDirAttributes.isDirectory).thenReturn(true)
        `when`(mockSftpClient.stat(remoteDir)).thenReturn(mockDirAttributes)

        // Mock directory entries
        val mockEntry1 = mock(SftpClient.DirEntry::class.java)
        val mockEntry1Attr = mock(SftpClient.Attributes::class.java)
        `when`(mockEntry1.filename).thenReturn("file1.log")
        `when`(mockEntry1.attributes).thenReturn(mockEntry1Attr)
        `when`(mockEntry1Attr.isDirectory).thenReturn(false)
        `when`(mockEntry1Attr.size).thenReturn(200L)

        val mockEntry2 = mock(SftpClient.DirEntry::class.java)
        val mockEntry2Attr = mock(SftpClient.Attributes::class.java)
        `when`(mockEntry2.filename).thenReturn("file2.log")
        `when`(mockEntry2.attributes).thenReturn(mockEntry2Attr)
        `when`(mockEntry2Attr.isDirectory).thenReturn(false)
        `when`(mockEntry2Attr.size).thenReturn(300L)

        `when`(mockSftpClient.readDir(remoteDir)).thenReturn(listOf(mockEntry1, mockEntry2))

        val reader = SftpItemReader(mockSftpClient, targets, localBaseDir)

        // When
        val item1 = reader.read()
        val item2 = reader.read()
        val item3 = reader.read()

        // Then
        assertNotNull(item1)
        assertNotNull(item2)
        assertEquals(null, item3)
        assertEquals("$remoteDir/file1.log", item1.remotePath)
        assertEquals("$remoteDir/file2.log", item2.remotePath)
    }

    @Test
    fun `test SftpItemProcessor creates local directories`() {
        // Given
        val processor = SftpItemProcessor()
        val localPath = File(localBaseDir, "subdir/test.txt").absolutePath
        val fileInfo = SftpFileInfo(
            remotePath = "/remote/subdir/test.txt",
            localPath = localPath,
            isDirectory = false,
            size = 100L
        )

        // When
        val result = processor.process(fileInfo)

        // Then
        assertNotNull(result)
        assertEquals(fileInfo.remotePath, result.remotePath)
        assertTrue(File(localBaseDir, "subdir").exists())
    }

    @Test
    fun `test SftpItemWriter downloads files`() {
        // Given
        val writer = SftpItemWriter(mockSftpClient)
        val fileContent = "Hello, SFTP!".toByteArray()
        val inputStream = ByteArrayInputStream(fileContent)

        val localPath = File(localBaseDir, "downloaded.txt").absolutePath
        val fileInfo = SftpFileInfo(
            remotePath = "/remote/test.txt",
            localPath = localPath,
            isDirectory = false,
            size = fileContent.size.toLong()
        )

        `when`(mockSftpClient.read("/remote/test.txt")).thenReturn(inputStream)

        val chunk = Chunk(listOf(fileInfo))

        // When
        writer.write(chunk)

        // Then
        val downloadedFile = File(localPath)
        assertTrue(downloadedFile.exists())
        assertEquals("Hello, SFTP!", downloadedFile.readText())
    }

    @Test
    fun `test full pipeline - Reader to Writer`() {
        // Given
        val remotePath = "/remote/pipeline.txt"
        val targets = listOf(remotePath)

        val mockAttributes = mock(SftpClient.Attributes::class.java)
        `when`(mockAttributes.isDirectory).thenReturn(false)
        `when`(mockAttributes.size).thenReturn(50L)
        `when`(mockSftpClient.stat(remotePath)).thenReturn(mockAttributes)

        val fileContent = "Pipeline test content".toByteArray()
        val inputStream = ByteArrayInputStream(fileContent)
        `when`(mockSftpClient.read(remotePath)).thenReturn(inputStream)

        val reader = SftpItemReader(mockSftpClient, targets, localBaseDir)
        val processor = SftpItemProcessor()
        val writer = SftpItemWriter(mockSftpClient)

        // When
        val item = reader.read()
        assertNotNull(item)

        val processed = processor.process(item)
        assertNotNull(processed)

        val chunk = Chunk(listOf(processed))
        writer.write(chunk)

        // Then
        val downloadedFile = File(processed.localPath)
        assertTrue(downloadedFile.exists())
        assertEquals("Pipeline test content", downloadedFile.readText())
    }
}
