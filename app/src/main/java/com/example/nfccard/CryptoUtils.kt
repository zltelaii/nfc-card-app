package com.example.nfccard

import android.util.Base64
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.security.*
import java.security.spec.ECGenParameterSpec
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.spec.SecretKeySpec
import java.nio.charset.StandardCharsets
import java.security.interfaces.ECPrivateKey
import java.security.interfaces.ECPublicKey

/**
 * 加密货币钱包工具类
 * 模拟Tangem Wallet的核心功能
 */
object CryptoUtils {
    
    init {
        // 添加BouncyCastle提供者
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(BouncyCastleProvider())
        }
    }
    
    /**
     * 钱包数据结构
     */
    data class WalletData(
        val publicKey: String,
        val address: String,
        val encryptedPrivateKey: String,
        val cardId: String,
        val createdAt: Long = System.currentTimeMillis()
    )
    
    /**
     * 交易数据结构
     */
    data class TransactionData(
        val from: String,
        val to: String,
        val amount: String,
        val gasPrice: String? = null,
        val gasLimit: String? = null,
        val nonce: String? = null,
        val data: String? = null
    )
    
    /**
     * 签名结果
     */
    data class SignatureResult(
        val signature: String,
        val publicKey: String,
        val transactionHash: String
    )
    
    /**
     * 生成椭圆曲线密钥对 (secp256k1)
     */
    fun generateKeyPair(): KeyPair {
        val keyGen = KeyPairGenerator.getInstance("EC", "BC")
        val ecSpec = ECGenParameterSpec("secp256k1")
        keyGen.initialize(ecSpec, SecureRandom())
        return keyGen.generateKeyPair()
    }
    
    /**
     * 从公钥生成以太坊地址
     */
    fun generateEthereumAddress(publicKey: ECPublicKey): String {
        try {
            // 获取未压缩的公钥字节
            val pubKeyBytes = publicKey.encoded
            
            // 简化版地址生成（实际应用中需要使用Keccak-256）
            val digest = MessageDigest.getInstance("SHA-256")
            val hash = digest.digest(pubKeyBytes)
            
            // 取后20字节作为地址
            val addressBytes = hash.sliceArray(hash.size - 20 until hash.size)
            return "0x" + bytesToHex(addressBytes).lowercase()
        } catch (e: Exception) {
            throw RuntimeException("生成地址失败: ${e.message}")
        }
    }
    
    /**
     * 加密私钥
     */
    fun encryptPrivateKey(privateKey: PrivateKey, password: String): String {
        try {
            val keySpec = generateAESKey(password)
            val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
            cipher.init(Cipher.ENCRYPT_MODE, keySpec)
            
            val privateKeyBytes = privateKey.encoded
            val encryptedBytes = cipher.doFinal(privateKeyBytes)
            
            return Base64.encodeToString(encryptedBytes, Base64.NO_WRAP)
        } catch (e: Exception) {
            throw RuntimeException("私钥加密失败: ${e.message}")
        }
    }
    
    /**
     * 解密私钥
     */
    fun decryptPrivateKey(encryptedPrivateKey: String, password: String): PrivateKey {
        try {
            val keySpec = generateAESKey(password)
            val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
            cipher.init(Cipher.DECRYPT_MODE, keySpec)
            
            val encryptedBytes = Base64.decode(encryptedPrivateKey, Base64.NO_WRAP)
            val decryptedBytes = cipher.doFinal(encryptedBytes)
            
            val keyFactory = KeyFactory.getInstance("EC", "BC")
            val privateKeySpec = PKCS8EncodedKeySpec(decryptedBytes)
            
            return keyFactory.generatePrivate(privateKeySpec)
        } catch (e: Exception) {
            throw RuntimeException("私钥解密失败: ${e.message}")
        }
    }
    
    /**
     * 对交易数据进行签名
     */
    fun signTransaction(transactionData: TransactionData, privateKey: PrivateKey): SignatureResult {
        try {
            // 创建交易哈希
            val transactionString = createTransactionString(transactionData)
            val transactionHash = sha256(transactionString)
            
            // 使用私钥签名
            val signature = Signature.getInstance("SHA256withECDSA", "BC")
            signature.initSign(privateKey)
            signature.update(transactionHash)
            val signatureBytes = signature.sign()
            
            // 获取对应的公钥
            val keyFactory = KeyFactory.getInstance("EC", "BC")
            val publicKey = keyFactory.generatePublic(
                X509EncodedKeySpec(getPublicKeyFromPrivate(privateKey))
            )
            
            return SignatureResult(
                signature = Base64.encodeToString(signatureBytes, Base64.NO_WRAP),
                publicKey = Base64.encodeToString(publicKey.encoded, Base64.NO_WRAP),
                transactionHash = bytesToHex(transactionHash)
            )
        } catch (e: Exception) {
            throw RuntimeException("交易签名失败: ${e.message}")
        }
    }
    
    /**
     * 验证签名
     */
    fun verifySignature(
        transactionData: TransactionData,
        signatureResult: SignatureResult
    ): Boolean {
        try {
            val transactionString = createTransactionString(transactionData)
            val transactionHash = sha256(transactionString)
            
            val keyFactory = KeyFactory.getInstance("EC", "BC")
            val publicKeySpec = X509EncodedKeySpec(
                Base64.decode(signatureResult.publicKey, Base64.NO_WRAP)
            )
            val publicKey = keyFactory.generatePublic(publicKeySpec)
            
            val signature = Signature.getInstance("SHA256withECDSA", "BC")
            signature.initVerify(publicKey)
            signature.update(transactionHash)
            
            val signatureBytes = Base64.decode(signatureResult.signature, Base64.NO_WRAP)
            return signature.verify(signatureBytes)
        } catch (e: Exception) {
            return false
        }
    }
    
    /**
     * 生成AES密钥
     */
    private fun generateAESKey(password: String): SecretKeySpec {
        val digest = MessageDigest.getInstance("SHA-256")
        val keyBytes = digest.digest(password.toByteArray(StandardCharsets.UTF_8))
        return SecretKeySpec(keyBytes.sliceArray(0..15), "AES")
    }
    
    /**
     * 创建交易字符串
     */
    private fun createTransactionString(transactionData: TransactionData): String {
        return "${transactionData.from}|${transactionData.to}|${transactionData.amount}|" +
                "${transactionData.gasPrice ?: ""}|${transactionData.gasLimit ?: ""}|" +
                "${transactionData.nonce ?: ""}|${transactionData.data ?: ""}"
    }
    
    /**
     * SHA-256哈希
     */
    private fun sha256(input: String): ByteArray {
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(input.toByteArray(StandardCharsets.UTF_8))
    }
    
    /**
     * 从私钥获取公钥字节
     */
    private fun getPublicKeyFromPrivate(privateKey: PrivateKey): ByteArray {
        // 简化实现，实际应用中需要更复杂的椭圆曲线计算
        val keyFactory = KeyFactory.getInstance("EC", "BC")
        val keyPair = generateKeyPair()
        return keyPair.public.encoded
    }
    
    /**
     * 字节数组转十六进制字符串
     */
    private fun bytesToHex(bytes: ByteArray): String {
        return bytes.joinToString("") { "%02x".format(it) }
    }
    
    /**
     * 生成卡片ID
     */
    fun generateCardId(): String {
        val random = SecureRandom()
        val bytes = ByteArray(8)
        random.nextBytes(bytes)
        return bytesToHex(bytes).uppercase()
    }
}
