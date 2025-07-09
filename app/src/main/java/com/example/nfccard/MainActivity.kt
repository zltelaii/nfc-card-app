package com.example.nfccard

import android.app.PendingIntent
import android.content.Intent
import android.content.IntentFilter
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.Ndef
import android.nfc.tech.NdefFormatable
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.nfccard.databinding.ActivityMainBinding
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var nfcAdapter: NfcAdapter? = null
    private var pendingIntent: PendingIntent? = null
    private var intentFiltersArray: Array<IntentFilter>? = null
    private var techListsArray: Array<Array<String>>? = null

    private var currentTag: Tag? = null
    private var currentCardInfo: TangemCardManager.CardInfo? = null
    private val tangemManager = TangemCardManager()
    private val gson = Gson()

    // 当前操作模式
    private enum class OperationMode {
        SCAN_CARD,
        INITIALIZE_CARD,
        SIGN_TRANSACTION,
        VIEW_WALLET
    }

    private var currentMode = OperationMode.SCAN_CARD
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        initNFC()
        setupUI()
        handleIntent(intent)
    }
    
    private fun initNFC() {
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        
        if (nfcAdapter == null) {
            showToast("此设备不支持NFC")
            finish()
            return
        }
        
        if (!nfcAdapter!!.isEnabled) {
            showToast("请在设置中启用NFC")
        }
        
        // 创建PendingIntent
        pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_MUTABLE
        )
        
        // 创建IntentFilter
        val ndef = IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED)
        try {
            ndef.addDataType("*/*")
        } catch (e: IntentFilter.MalformedMimeTypeException) {
            throw RuntimeException("fail", e)
        }
        
        intentFiltersArray = arrayOf(ndef)
        techListsArray = arrayOf(
            arrayOf(Ndef::class.java.name),
            arrayOf(NdefFormatable::class.java.name)
        )
    }
    
    private fun setupUI() {
        // 初始化卡片按钮
        binding.btnInitializeCard.setOnClickListener {
            val password = binding.etCardPassword.text.toString()
            if (password.length >= 6) {
                currentMode = OperationMode.INITIALIZE_CARD
                showToast("请将新卡片靠近手机进行初始化...")
            } else {
                showToast("密码至少需要6位字符")
            }
        }

        // 查看钱包按钮
        binding.btnViewWallet.setOnClickListener {
            currentMode = OperationMode.VIEW_WALLET
            showToast("请将卡片靠近手机...")
        }

        // 签名交易按钮
        binding.btnSignTransaction.setOnClickListener {
            val toAddress = binding.etToAddress.text.toString()
            val amount = binding.etAmount.text.toString()
            val password = binding.etTransactionPassword.text.toString()

            if (toAddress.isNotEmpty() && amount.isNotEmpty() && password.isNotEmpty()) {
                currentMode = OperationMode.SIGN_TRANSACTION
                showToast("请将卡片靠近手机进行签名...")
            } else {
                showToast("请填写完整的交易信息")
            }
        }

        // 清除按钮
        binding.btnClear.setOnClickListener {
            clearAllFields()
            resetState()
        }
    }
    
    override fun onResume() {
        super.onResume()
        nfcAdapter?.enableForegroundDispatch(this, pendingIntent, intentFiltersArray, techListsArray)
    }
    
    override fun onPause() {
        super.onPause()
        nfcAdapter?.disableForegroundDispatch(this)
    }
    
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }
    
    private fun handleIntent(intent: Intent) {
        val action = intent.action
        if (NfcAdapter.ACTION_NDEF_DISCOVERED == action ||
            NfcAdapter.ACTION_TAG_DISCOVERED == action ||
            NfcAdapter.ACTION_TECH_DISCOVERED == action) {

            val tag = intent.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG)
            if (tag != null) {
                currentTag = tag
                processNFCCard(tag)
            }
        }
    }

    private fun processNFCCard(tag: Tag) {
        when (currentMode) {
            OperationMode.INITIALIZE_CARD -> initializeNewCard(tag)
            OperationMode.VIEW_WALLET -> viewWalletInfo(tag)
            OperationMode.SIGN_TRANSACTION -> signTransaction(tag)
            OperationMode.SCAN_CARD -> scanExistingCard(tag)
        }
    }
    
    /**
     * 初始化新卡片
     */
    private fun initializeNewCard(tag: Tag) {
        val password = binding.etCardPassword.text.toString()

        showToast("正在初始化卡片...")

        try {
            val response = tangemManager.initializeCard(tag, password)

            if (response.success && response.data != null) {
                val cardInfo = gson.fromJson(response.data, TangemCardManager.CardInfo::class.java)
                currentCardInfo = cardInfo

                binding.tvCardStatus.text = "卡片状态: 已初始化"
                binding.tvCardId.text = "卡片ID: ${cardInfo.cardId}"
                binding.tvWalletAddress.text = "钱包地址: ${cardInfo.walletAddress}"
                binding.tvPublicKey.text = "公钥: ${cardInfo.publicKey?.take(32)}..."

                showToast("卡片初始化成功！")
                clearAllFields()
                currentMode = OperationMode.SCAN_CARD
            } else {
                showToast("初始化失败: ${response.error}")
            }
        } catch (e: Exception) {
            showToast("初始化错误: ${e.message}")
        }
    }

    /**
     * 查看钱包信息
     */
    private fun viewWalletInfo(tag: Tag) {
        try {
            val command = TangemCardManager.CardCommand(
                type = TangemCardManager.CommandType.GET_CARD_INFO
            )

            val response = tangemManager.processCommand(tag, command)

            if (response.success && response.data != null) {
                val cardInfo = gson.fromJson(response.data, TangemCardManager.CardInfo::class.java)
                currentCardInfo = cardInfo

                binding.tvCardStatus.text = "卡片状态: ${cardInfo.status}"
                binding.tvCardId.text = "卡片ID: ${cardInfo.cardId}"
                binding.tvWalletAddress.text = "钱包地址: ${cardInfo.walletAddress}"
                binding.tvPublicKey.text = "公钥: ${cardInfo.publicKey?.take(32)}..."
                binding.tvLastUsed.text = "最后使用: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(cardInfo.lastUsed)}"

                showToast("钱包信息读取成功")
            } else {
                showToast("读取失败: ${response.error}")
            }
        } catch (e: Exception) {
            showToast("读取错误: ${e.message}")
        }

        currentMode = OperationMode.SCAN_CARD
    }
    
    /**
     * 签名交易
     */
    private fun signTransaction(tag: Tag) {
        val toAddress = binding.etToAddress.text.toString()
        val amount = binding.etAmount.text.toString()
        val password = binding.etTransactionPassword.text.toString()

        try {
            // 创建交易数据
            val transactionData = CryptoUtils.TransactionData(
                from = currentCardInfo?.walletAddress ?: "",
                to = toAddress,
                amount = amount,
                gasPrice = "20000000000", // 20 Gwei
                gasLimit = "21000",
                nonce = System.currentTimeMillis().toString()
            )

            val command = TangemCardManager.CardCommand(
                type = TangemCardManager.CommandType.SIGN_TRANSACTION,
                data = gson.toJson(transactionData),
                password = password
            )

            showToast("正在签名交易...")

            val response = tangemManager.processCommand(tag, command)

            if (response.success && response.data != null) {
                val signatureResult = gson.fromJson(response.data, CryptoUtils.SignatureResult::class.java)

                binding.tvSignatureResult.text = "签名结果:\n" +
                        "交易哈希: ${signatureResult.transactionHash}\n" +
                        "签名: ${signatureResult.signature.take(32)}...\n" +
                        "公钥: ${signatureResult.publicKey.take(32)}..."

                showToast("交易签名成功！")
                clearTransactionFields()
            } else {
                showToast("签名失败: ${response.error}")
            }
        } catch (e: Exception) {
            showToast("签名错误: ${e.message}")
        }

        currentMode = OperationMode.SCAN_CARD
    }

    /**
     * 扫描现有卡片
     */
    private fun scanExistingCard(tag: Tag) {
        try {
            val command = TangemCardManager.CardCommand(
                type = TangemCardManager.CommandType.GET_CARD_INFO
            )

            val response = tangemManager.processCommand(tag, command)

            if (response.success && response.data != null) {
                val cardInfo = gson.fromJson(response.data, TangemCardManager.CardInfo::class.java)
                currentCardInfo = cardInfo

                binding.tvNfcStatus.text = "检测到已初始化的卡片"
                binding.tvCardStatus.text = "卡片状态: ${cardInfo.status}"
                binding.tvCardId.text = "卡片ID: ${cardInfo.cardId}"
                binding.tvWalletAddress.text = "钱包地址: ${cardInfo.walletAddress}"

                showToast("卡片扫描成功")
            } else {
                binding.tvNfcStatus.text = "检测到未初始化的卡片"
                showToast("检测到空白卡片，可以进行初始化")
            }
        } catch (e: Exception) {
            binding.tvNfcStatus.text = "卡片读取失败"
            showToast("卡片读取错误: ${e.message}")
        }
    }

    /**
     * 清除所有输入字段
     */
    private fun clearAllFields() {
        binding.etCardPassword.text.clear()
        binding.etToAddress.text.clear()
        binding.etAmount.text.clear()
        binding.etTransactionPassword.text.clear()
    }

    /**
     * 清除交易相关字段
     */
    private fun clearTransactionFields() {
        binding.etToAddress.text.clear()
        binding.etAmount.text.clear()
        binding.etTransactionPassword.text.clear()
    }

    /**
     * 重置状态
     */
    private fun resetState() {
        currentTag = null
        currentCardInfo = null
        currentMode = OperationMode.SCAN_CARD
        binding.tvNfcStatus.text = "请将NFC卡靠近手机..."
        binding.tvCardStatus.text = "卡片状态: 未检测到"
        binding.tvCardId.text = "卡片ID: -"
        binding.tvWalletAddress.text = "钱包地址: -"
        binding.tvPublicKey.text = "公钥: -"
        binding.tvLastUsed.text = "最后使用: -"
        binding.tvSignatureResult.text = "签名结果: 暂无"
    }


    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
