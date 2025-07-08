package com.example.nfccard

import android.app.PendingIntent
import android.content.Intent
import android.content.IntentFilter
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.Ndef
import android.nfc.tech.NdefFormatable
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.nfccard.databinding.ActivityMainBinding
import java.nio.charset.Charset

class MainActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityMainBinding
    private var nfcAdapter: NfcAdapter? = null
    private var pendingIntent: PendingIntent? = null
    private var intentFiltersArray: Array<IntentFilter>? = null
    private var techListsArray: Array<Array<String>>? = null
    
    private var currentTag: Tag? = null
    private var isPasswordVerified = false
    private val correctPassword = "123456" // 默认密码，实际应用中应该加密存储
    
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
        binding.btnConfirmPassword.setOnClickListener {
            val inputPassword = binding.etPassword.text.toString()
            if (inputPassword == correctPassword) {
                isPasswordVerified = true
                showDataSection()
                readNFCData()
                showToast("密码验证成功")
            } else {
                showToast("密码错误，请重试")
                binding.etPassword.text.clear()
            }
        }
        
        binding.btnCancelPassword.setOnClickListener {
            hidePasswordSection()
            resetState()
        }
        
        binding.btnWriteData.setOnClickListener {
            val dataToWrite = binding.etNewData.text.toString()
            if (dataToWrite.isNotEmpty()) {
                val shouldEncrypt = binding.cbEncrypt.isChecked
                writeNFCData(dataToWrite, shouldEncrypt)
            } else {
                showToast("请输入要写入的数据")
            }
        }
        
        binding.btnReadData.setOnClickListener {
            readNFCData()
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
                binding.tvNfcStatus.text = "检测到NFC卡，请输入密码"
                showPasswordSection()
            }
        }
    }
    
    private fun showPasswordSection() {
        binding.cardPassword.visibility = View.VISIBLE
        binding.cardData.visibility = View.GONE
        binding.etPassword.text.clear()
        isPasswordVerified = false
    }
    
    private fun hidePasswordSection() {
        binding.cardPassword.visibility = View.GONE
    }
    
    private fun showDataSection() {
        binding.cardPassword.visibility = View.GONE
        binding.cardData.visibility = View.VISIBLE
    }
    
    private fun resetState() {
        currentTag = null
        isPasswordVerified = false
        binding.tvNfcStatus.text = "请将NFC卡靠近手机..."
        binding.cardPassword.visibility = View.GONE
        binding.cardData.visibility = View.GONE
    }
    
    private fun readNFCData() {
        currentTag?.let { tag ->
            val ndef = Ndef.get(tag)
            if (ndef != null) {
                try {
                    ndef.connect()
                    val ndefMessage = ndef.ndefMessage
                    if (ndefMessage != null && ndefMessage.records.isNotEmpty()) {
                        val record = ndefMessage.records[0]
                        val payload = record.payload

                        if (payload.isNotEmpty()) {
                            // 检查是否是文本记录
                            // 尝试解析为文本记录
                            val text = NFCUtils.parseTextRecord(record)
                            if (text != null) {
                                // 检查是否为加密数据
                                if (NFCUtils.isEncryptedText(text)) {
                                    try {
                                        val decryptedText = NFCUtils.decryptText(text, correctPassword)
                                        binding.tvCurrentData.text = "当前数据: $decryptedText (已解密)"
                                        showToast("加密数据读取成功")
                                    } catch (e: Exception) {
                                        binding.tvCurrentData.text = "当前数据: [加密数据，解密失败]"
                                        showToast("解密失败，可能密码不正确")
                                    }
                                } else {
                                    binding.tvCurrentData.text = "当前数据: $text"
                                    showToast("数据读取成功")
                                }
                            } else {
                                // 尝试直接解析为文本
                                val rawText = String(payload, Charset.forName("UTF-8"))
                                binding.tvCurrentData.text = "当前数据: $rawText"
                                showToast("数据读取成功（原始格式）")
                            }
                        } else {
                            binding.tvCurrentData.text = "当前数据: 无数据"
                            showToast("NFC卡为空")
                        }
                    } else {
                        binding.tvCurrentData.text = "当前数据: 无数据"
                        showToast("NFC卡为空")
                    }
                    ndef.close()
                } catch (e: Exception) {
                    showToast("读取失败: ${e.message}")
                    binding.tvCurrentData.text = "当前数据: 读取失败"
                }
            } else {
                // 尝试其他技术类型
                tryReadWithOtherTech(tag)
            }
        }
    }

    private fun tryReadWithOtherTech(tag: Tag) {
        try {
            // 获取卡片信息
            val tagInfo = NFCUtils.getTagInfo(tag)
            val infoText = tagInfo.entries.joinToString("\n") { "${it.key}: ${it.value}" }

            binding.tvCurrentData.text = "卡片信息:\n$infoText"
            showToast("检测到NFC卡，但无法读取NDEF数据")
        } catch (e: Exception) {
            showToast("无法识别的NFC卡类型")
            binding.tvCurrentData.text = "当前数据: 无法识别"
        }
    }


    
    private fun writeNFCData(data: String, encrypt: Boolean = false) {
        currentTag?.let { tag ->
            val ndef = Ndef.get(tag)
            if (ndef != null) {
                try {
                    ndef.connect()

                    // 处理数据（加密或清理）
                    val processedData = if (encrypt) {
                        try {
                            NFCUtils.encryptText(data, correctPassword)
                        } catch (e: Exception) {
                            showToast("加密失败: ${e.message}")
                            return
                        }
                    } else {
                        NFCUtils.sanitizeInput(data)
                    }

                    // 验证数据有效性
                    if (!encrypt && !NFCUtils.isValidText(processedData)) {
                        showToast("输入的数据无效，请输入有效的字母或数字")
                        return
                    }

                    // 创建NDEF记录
                    val textRecord = NFCUtils.createTextRecord(processedData)
                    val ndefMessage = NdefMessage(arrayOf(textRecord))
                    
                    // 检查容量
                    if (ndefMessage.toByteArray().size > ndef.maxSize) {
                        showToast("数据太大，无法写入")
                        return
                    }
                    
                    // 写入数据
                    ndef.writeNdefMessage(ndefMessage)
                    ndef.close()

                    val displayData = if (encrypt) "$data (已加密)" else data
                    binding.tvCurrentData.text = "当前数据: $displayData"
                    binding.etNewData.text.clear()
                    binding.cbEncrypt.isChecked = false
                    showToast("数据写入成功")
                    
                } catch (e: Exception) {
                    showToast("写入失败: ${e.message}")
                }
            } else {
                // 尝试格式化卡片
                val ndefFormatable = NdefFormatable.get(tag)
                if (ndefFormatable != null) {
                    try {
                        ndefFormatable.connect()

                        // 处理数据（与上面相同的逻辑）
                        val processedData = if (encrypt) {
                            try {
                                NFCUtils.encryptText(data, correctPassword)
                            } catch (e: Exception) {
                                showToast("加密失败: ${e.message}")
                                return
                            }
                        } else {
                            NFCUtils.sanitizeInput(data)
                        }

                        val textRecord = NFCUtils.createTextRecord(processedData)
                        val ndefMessage = NdefMessage(arrayOf(textRecord))
                        ndefFormatable.format(ndefMessage)
                        ndefFormatable.close()

                        val displayData = if (encrypt) "$data (已加密)" else data
                        binding.tvCurrentData.text = "当前数据: $displayData"
                        binding.etNewData.text.clear()
                        binding.cbEncrypt.isChecked = false
                        showToast("卡片格式化并写入成功")
                        
                    } catch (e: Exception) {
                        showToast("格式化失败: ${e.message}")
                    }
                } else {
                    showToast("不支持的NFC卡类型")
                }
            }
        }
    }
    

    
    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
