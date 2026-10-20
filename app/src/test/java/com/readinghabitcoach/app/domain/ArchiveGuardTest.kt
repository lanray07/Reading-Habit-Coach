package com.readinghabitcoach.app.domain

import com.readinghabitcoach.app.reader.ArchiveGuard
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.util.zip.*

class ArchiveGuardTest {
    @Test fun `unsafe archive names are rejected`() {
        listOf("../secret", "a/../../b", "/absolute", "C:/drive", "\\windows", "a\\..\\b").forEach { assertFalse(it, ArchiveGuard.safeName(it)) }
        assertTrue(ArchiveGuard.safeName("EPUB/chapters/chapter1.xhtml"))
    }
    private fun epub(extra: Pair<String, ByteArray>? = null): File {
        val file = File.createTempFile("coach-test", ".epub")
        ZipOutputStream(file.outputStream()).use { zip ->
            listOf("mimetype" to "application/epub+zip".toByteArray(), "META-INF/container.xml" to "<container/>".toByteArray()).plus(listOfNotNull(extra)).forEach { (name, data) -> zip.putNextEntry(ZipEntry(name)); zip.write(data); zip.closeEntry() }
        }
        return file
    }
    @Test fun `safe unencrypted archive passes preflight`() { val f=epub(); try { ArchiveGuard.validateEpub(f) } finally { f.delete() } }
    @Test fun `traversal archive fails preflight`() { val f=epub("../private.txt" to byteArrayOf(1)); try { assertThrows(IllegalArgumentException::class.java) { ArchiveGuard.validateEpub(f) } } finally { f.delete() } }
    @Test fun `encrypted archive fails preflight`() { val f=epub("META-INF/encryption.xml" to "<encryption/>".toByteArray()); try { assertThrows(IllegalArgumentException::class.java) { ArchiveGuard.validateEpub(f) } } finally { f.delete() } }
    @Test fun `high compression archive fails preflight`() { val f=epub("EPUB/bomb.xhtml" to ByteArray(2*1024*1024)); try { assertThrows(IllegalArgumentException::class.java) { ArchiveGuard.validateEpub(f) } } finally { f.delete() } }
}
