package com.example.nfccard

import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.Tag
import android.nfc.tech.Ndef
import android.nfc.tech.NdefFormatable
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import java.security.KeyPair
import java.security.interfaces.ECPrivateKey
import java.security.interfaces.ECPublicKey

/**
 * Tangem风格的NFC卡片管理器
 * 模拟安全芯片的功能
 */
class TangemCardManager {
    
    private val gson = Gson()
    
    /**
     * 卡片状态
     */
    enum class CardStatus {
        UNINITIALIZED,  // 未初始化
        INITIALIZED,    // 已初始化
        LOCKED,         // 已锁定
        ERROR           // 错误状态
    }
    
    /**
     * 卡片命令类型
     */
    enum class CommandType {
        INITIALIZE_CARD,    // 初始化卡片
        GET_PUBLIC_KEY,     // 获取公钥
        SIGN_TRANSACTION,   // 签名交易
        GET_CARD_INFO,      // 获取卡片信息
        BACKUP_WALLET       // 备份钱包
    }
    
    /**
     * 卡片命令
     */
    data class CardCommand(
        val type: CommandType,
        val data: String? = null,
        val password: String? = null
    )
    
    /**
     * 卡片响应
     */
    data class CardResponse(
        val success: Boolean,
        val data: String? = null,
        val error: String? = null,
        val cardStatus: CardStatus = CardStatus.UNINITIALIZED
    )
    
    /**
     * 卡片信息
     */
    data class CardInfo(
        val cardId: String,
        val status: CardStatus,
        val walletAddress: String? = null,
        val publicKey: String? = null,
        val createdAt: Long? = null,
        val lastUsed: Long = System.currentTimeMillis()
    )
    
    /**
     * 初始化新卡片
     */
    fun initializeCard(tag: Tag, password: String): CardResponse {
        return try {
            // 生成新的密钥对
            val keyPair = CryptoUtils.generateKeyPair()
            val publicKey = keyPair.public as ECPublicKey
            val privateKey = keyPair.private as ECPrivateKey
            
            // 生成钱包地址
            val address = CryptoUtils.generateEthereumAddress(publicKey)
            
            // 加密私钥
            val encryptedPrivateKey = CryptoUtils.encryptPrivateKey(privateKey, password)
            
            // 创建钱包数据
            val walletData = CryptoUtils.WalletData(
                publicKey = android.util.Base64.encodeToString(publicKey.encoded, android.util.Base64.NO_WRAP),
                address = address,
                encryptedPrivateKey = encryptedPrivateKey,
                cardId = CryptoUtils.generateCardId()
            )
            
            // 创建卡片信息
            val cardInfo = CardInfo(
                cardId = walletData.cardId,
                status = CardStatus.INITIALIZED,
                walletAddress = address,
                publicKey = walletData.publicKey,
                createdAt = walletData.createdAt
            )
            
            // 将数据写入NFC卡
            val success = writeToCard(tag, walletData, cardInfo)
            
            if (success) {
                CardResponse(
                    success = true,
                    data = gson.toJson(cardInfo),
                    cardStatus = CardStatus.INITIALIZED
                )
            } else {
                CardResponse(
                    success = false,
                    error = "写入卡片失败",
                    cardStatus = CardStatus.ERROR
                )
            }
        } catch (e: Exception) {
            CardResponse(
                success = false,
                error = "初始化失败: ${e.message}",
                cardStatus = CardStatus.ERROR
            )
        }
    }
    
    /**
     * 处理卡片命令
     */
    fun processCommand(tag: Tag, command: CardCommand): CardResponse {
        return try {
            val cardData = readFromCard(tag)
            if (cardData == null) {
                return CardResponse(
                    success = false,
                    error = "无法读取卡片数据",
                    cardStatus = CardStatus.ERROR
                )
            }
            
            when (command.type) {
                CommandType.GET_CARD_INFO -> getCardInfo(cardData)
                CommandType.GET_PUBLIC_KEY -> getPublicKey(cardData)
                CommandType.SIGN_TRANSACTION -> signTransaction(cardData, command)
                CommandType.BACKUP_WALLET -> backupWallet(cardData, command)
                else -> CardResponse(
                    success = false,
                    error = "不支持的命令类型",
                    cardStatus = CardStatus.ERROR
                )
            }
        } catch (e: Exception) {
            CardResponse(
                success = false,
                error = "命令处理失败: ${e.message}",
                cardStatus = CardStatus.ERROR
            )
        }
    }
    
    /**
     * 获取卡片信息
     */
    private fun getCardInfo(cardData: Pair<CryptoUtils.WalletData, CardInfo>): CardResponse {
        val (_, cardInfo) = cardData
        return CardResponse(
            success = true,
            data = gson.toJson(cardInfo.copy(lastUsed = System.currentTimeMillis())),
            cardStatus = cardInfo.status
        )
    }
    
    /**
     * 获取公钥
     */
    private fun getPublicKey(cardData: Pair<CryptoUtils.WalletData, CardInfo>): CardResponse {
        val (walletData, cardInfo) = cardData
        return CardResponse(
            success = true,
            data = walletData.publicKey,
            cardStatus = cardInfo.status
        )
    }
    
    /**
     * 签名交易
     */
    private fun signTransaction(
        cardData: Pair<CryptoUtils.WalletData, CardInfo>,
        command: CardCommand
    ): CardResponse {
        val (walletData, cardInfo) = cardData
        
        if (command.password == null || command.data == null) {
            return CardResponse(
                success = false,
                error = "缺少密码或交易数据",
                cardStatus = cardInfo.status
            )
        }
        
        return try {
            // 解密私钥
            val privateKey = CryptoUtils.decryptPrivateKey(walletData.encryptedPrivateKey, command.password)
            
            // 解析交易数据
            val transactionData = gson.fromJson(command.data, CryptoUtils.TransactionData::class.java)
            
            // 签名交易
            val signatureResult = CryptoUtils.signTransaction(transactionData, privateKey)
            
            CardResponse(
                success = true,
                data = gson.toJson(signatureResult),
                cardStatus = cardInfo.status
            )
        } catch (e: Exception) {
            CardResponse(
                success = false,
                error = "签名失败: ${e.message}",
                cardStatus = cardInfo.status
            )
        }
    }
    
    /**
     * 备份钱包
     */
    private fun backupWallet(
        cardData: Pair<CryptoUtils.WalletData, CardInfo>,
        command: CardCommand
    ): CardResponse {
        val (walletData, cardInfo) = cardData
        
        if (command.password == null) {
            return CardResponse(
                success = false,
                error = "缺少密码",
                cardStatus = cardInfo.status
            )
        }
        
        return try {
            // 验证密码（通过尝试解密私钥）
            CryptoUtils.decryptPrivateKey(walletData.encryptedPrivateKey, command.password)
            
            // 返回加密的钱包数据用于备份
            CardResponse(
                success = true,
                data = gson.toJson(walletData),
                cardStatus = cardInfo.status
            )
        } catch (e: Exception) {
            CardResponse(
                success = false,
                error = "密码验证失败",
                cardStatus = cardInfo.status
            )
        }
    }
    
    /**
     * 写入数据到NFC卡
     */
    private fun writeToCard(tag: Tag, walletData: CryptoUtils.WalletData, cardInfo: CardInfo): Boolean {
        return try {
            val combinedData = mapOf(
                "wallet" to walletData,
                "info" to cardInfo
            )
            val jsonData = gson.toJson(combinedData)
            
            val ndef = Ndef.get(tag)
            if (ndef != null) {
                ndef.connect()
                val record = NFCUtils.createTextRecord(jsonData)
                val message = NdefMessage(arrayOf(record))
                ndef.writeNdefMessage(message)
                ndef.close()
                true
            } else {
                val ndefFormatable = NdefFormatable.get(tag)
                if (ndefFormatable != null) {
                    ndefFormatable.connect()
                    val record = NFCUtils.createTextRecord(jsonData)
                    val message = NdefMessage(arrayOf(record))
                    ndefFormatable.format(message)
                    ndefFormatable.close()
                    true
                } else {
                    false
                }
            }
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * 从NFC卡读取数据
     */
    private fun readFromCard(tag: Tag): Pair<CryptoUtils.WalletData, CardInfo>? {
        return try {
            val ndef = Ndef.get(tag) ?: return null
            ndef.connect()
            val message = ndef.ndefMessage
            ndef.close()
            
            if (message != null && message.records.isNotEmpty()) {
                val record = message.records[0]
                val text = NFCUtils.parseTextRecord(record)
                
                if (text != null) {
                    val combinedData = gson.fromJson(text, Map::class.java)
                    val walletJson = gson.toJson(combinedData["wallet"])
                    val infoJson = gson.toJson(combinedData["info"])
                    
                    val walletData = gson.fromJson(walletJson, CryptoUtils.WalletData::class.java)
                    val cardInfo = gson.fromJson(infoJson, CardInfo::class.java)
                    
                    Pair(walletData, cardInfo)
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
}
