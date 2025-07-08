package com.example.nfccard

import android.nfc.NdefRecord
import android.util.Base64
import java.nio.charset.Charset
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

/**
 * NFC工具类，提供数据加密、解密和格式化功能
 */
object NFCUtils {
    
    private const val ALGORITHM = "AES"
    private const val TRANSFORMATION = "AES/ECB/PKCS5Padding"
    
    /**
     * 创建文本NDEF记录
     */
    fun createTextRecord(text: String, language: String = "en"): NdefRecord {
        val langBytes = language.toByteArray(Charset.forName("US-ASCII"))
        val textBytes = text.toByteArray(Charset.forName("UTF-8"))
        val langLength = langBytes.size
        val textLength = textBytes.size
        val payload = ByteArray(1 + langLength + textLength)
        
        // 设置状态字节（语言代码长度）
        payload[0] = langLength.toByte()
        
        // 复制语言代码
        System.arraycopy(langBytes, 0, payload, 1, langLength)
        
        // 复制文本
        System.arraycopy(textBytes, 0, payload, 1 + langLength, textLength)
        
        return NdefRecord(NdefRecord.TNF_WELL_KNOWN, NdefRecord.RTD_TEXT, ByteArray(0), payload)
    }
    
    /**
     * 解析文本NDEF记录
     */
    fun parseTextRecord(record: NdefRecord): String? {
        return try {
            if (record.tnf == NdefRecord.TNF_WELL_KNOWN && 
                record.type.contentEquals(NdefRecord.RTD_TEXT)) {
                
                val payload = record.payload
                if (payload.isNotEmpty()) {
                    val languageCodeLength = payload[0].toInt() and 0x3F
                    val textBytes = payload.copyOfRange(1 + languageCodeLength, payload.size)
                    String(textBytes, Charset.forName("UTF-8"))
                } else {
                    null
                }
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * 生成密钥
     */
    private fun generateKey(password: String): SecretKeySpec {
        val digest = MessageDigest.getInstance("SHA-256")
        val keyBytes = digest.digest(password.toByteArray(Charset.forName("UTF-8")))
        return SecretKeySpec(keyBytes.copyOf(16), ALGORITHM) // 使用前16字节作为AES密钥
    }
    
    /**
     * 加密文本
     */
    fun encryptText(text: String, password: String): String {
        return try {
            val key = generateKey(password)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, key)
            val encryptedBytes = cipher.doFinal(text.toByteArray(Charset.forName("UTF-8")))
            Base64.encodeToString(encryptedBytes, Base64.DEFAULT)
        } catch (e: Exception) {
            throw Exception("加密失败: ${e.message}")
        }
    }
    
    /**
     * 解密文本
     */
    fun decryptText(encryptedText: String, password: String): String {
        return try {
            val key = generateKey(password)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, key)
            val encryptedBytes = Base64.decode(encryptedText, Base64.DEFAULT)
            val decryptedBytes = cipher.doFinal(encryptedBytes)
            String(decryptedBytes, Charset.forName("UTF-8"))
        } catch (e: Exception) {
            throw Exception("解密失败: ${e.message}")
        }
    }
    
    /**
     * 验证文本是否为加密格式
     */
    fun isEncryptedText(text: String): Boolean {
        return try {
            Base64.decode(text, Base64.DEFAULT)
            // 简单检查：加密后的文本通常不包含常见的可读字符模式
            !text.matches(Regex(".*[a-zA-Z]{3,}.*"))
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * 格式化字节数组为十六进制字符串
     */
    fun bytesToHex(bytes: ByteArray): String {
        val hexChars = "0123456789ABCDEF"
        val result = StringBuilder(bytes.size * 2)
        bytes.forEach {
            val i = it.toInt()
            result.append(hexChars[i shr 4 and 0x0f])
            result.append(hexChars[i and 0x0f])
        }
        return result.toString()
    }
    
    /**
     * 验证数据长度是否适合NFC卡
     */
    fun validateDataLength(data: String, maxSize: Int): Boolean {
        val record = createTextRecord(data)
        val messageSize = record.toByteArray().size + 3 // 添加消息头的大小
        return messageSize <= maxSize
    }
    
    /**
     * 清理和验证输入文本
     */
    fun sanitizeInput(input: String): String {
        return input.trim()
            .replace(Regex("[\\x00-\\x1F\\x7F]"), "") // 移除控制字符
            .take(1000) // 限制最大长度
    }
    
    /**
     * 检查文本是否包含有效字符
     */
    fun isValidText(text: String): Boolean {
        if (text.isBlank()) return false
        
        // 检查是否包含有效的字母数字字符
        return text.matches(Regex(".*[a-zA-Z0-9].*"))
    }
    
    /**
     * 获取NFC卡的基本信息
     */
    fun getTagInfo(tag: android.nfc.Tag): Map<String, String> {
        val info = mutableMapOf<String, String>()
        
        try {
            info["ID"] = bytesToHex(tag.id)
            info["技术类型"] = tag.techList.joinToString(", ") { 
                it.substringAfterLast(".")
            }
            
            // 尝试获取NDEF信息
            val ndef = android.nfc.tech.Ndef.get(tag)
            if (ndef != null) {
                try {
                    ndef.connect()
                    info["类型"] = ndef.type
                    info["最大容量"] = "${ndef.maxSize} 字节"
                    info["是否可写"] = if (ndef.isWritable) "是" else "否"
                    ndef.close()
                } catch (e: Exception) {
                    info["NDEF状态"] = "无法读取"
                }
            }
        } catch (e: Exception) {
            info["错误"] = e.message ?: "未知错误"
        }
        
        return info
    }
}
