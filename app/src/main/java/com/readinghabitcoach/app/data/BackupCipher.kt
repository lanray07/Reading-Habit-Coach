package com.readinghabitcoach.app.data

import com.readinghabitcoach.app.i18n.L10n
import com.readinghabitcoach.app.R

import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/** Versioned authenticated local backup envelope. Passwords and keys are never persisted. */
object BackupCipher {
    private val magic = byteArrayOf(82,72,67,49) // RHC1
    private fun key(password: CharArray, salt: ByteArray): SecretKeySpec {
        val spec = PBEKeySpec(password, salt, 210_000, 256)
        return try { val bytes = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
            try { SecretKeySpec(bytes, "AES") } finally { bytes.fill(0) }
        } finally { spec.clearPassword() }
    }
    fun encrypt(plain: ByteArray, password: CharArray): ByteArray {
        require(password.size in 12..4096) { L10n.text(R.string.msg_f87725a7b3b0) }
        val salt=ByteArray(16).also { SecureRandom().nextBytes(it) }; val iv=ByteArray(12).also { SecureRandom().nextBytes(it) }
        val cipher=Cipher.getInstance("AES/GCM/NoPadding"); cipher.init(Cipher.ENCRYPT_MODE,key(password,salt),GCMParameterSpec(128,iv))
        val header=magic+salt+iv; cipher.updateAAD(header)
        return header+cipher.doFinal(plain)
    }
    fun decrypt(envelope: ByteArray, password: CharArray): ByteArray {
        require(envelope.size in 48..(11*1024*1024) && envelope.copyOfRange(0,4).contentEquals(magic)) { L10n.text(R.string.msg_49e4f20d2e2d) }
        require(password.size in 1..4096) { L10n.text(R.string.msg_859b992d9120) }
        val cipher=Cipher.getInstance("AES/GCM/NoPadding"); cipher.init(Cipher.DECRYPT_MODE,key(password,envelope.copyOfRange(4,20)),GCMParameterSpec(128,envelope.copyOfRange(20,32)))
        cipher.updateAAD(envelope.copyOfRange(0,32))
        return try { cipher.doFinal(envelope.copyOfRange(32,envelope.size)) } catch (_: javax.crypto.AEADBadTagException) { throw IllegalArgumentException(L10n.text(R.string.msg_c393922d7d78)) }
    }
}
