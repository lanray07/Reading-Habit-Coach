package com.readinghabitcoach.app.domain

import com.readinghabitcoach.app.data.BackupCipher
import org.junit.Assert.*
import org.junit.Test

class BackupCipherTest {
    private val password="a long private passphrase".toCharArray()
    @Test fun `authenticated backup round trips unicode`() {
        val plain="Private notes: café, العربية, हिन्दी".toByteArray()
        val encrypted=BackupCipher.encrypt(plain,password)
        assertFalse(encrypted.toString(Charsets.UTF_8).contains("Private notes"))
        assertArrayEquals(plain,BackupCipher.decrypt(encrypted,password))
    }
    @Test fun `wrong password and tampering fail closed`() {
        val encrypted=BackupCipher.encrypt("private".toByteArray(),password)
        assertThrows(IllegalArgumentException::class.java){BackupCipher.decrypt(encrypted,"wrong password".toCharArray())}
        encrypted[encrypted.lastIndex]=(encrypted.last().toInt() xor 1).toByte()
        assertThrows(IllegalArgumentException::class.java){BackupCipher.decrypt(encrypted,password)}
    }
    @Test fun `random salt and IV make separate backups different`() {
        assertFalse(BackupCipher.encrypt(byteArrayOf(1),password).contentEquals(BackupCipher.encrypt(byteArrayOf(1),password)))
    }
}
