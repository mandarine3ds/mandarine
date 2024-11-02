package io.github.mandarine3ds.mandarine.dialogs

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.github.mandarine3ds.mandarine.R
import io.github.mandarine3ds.mandarine.databinding.DialogChatBinding
import io.github.mandarine3ds.mandarine.databinding.ItemChatMessageBinding
import io.github.mandarine3ds.mandarine.utils.NetPlayManager
import java.text.SimpleDateFormat
import java.util.*

class ChatMessage(
    val nickname: String, // This is the common name youll see on private servers
    val username: String, // Username is the community/forum username
    val message: String,
    val timestamp: String = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
) {
    fun getFormattedMessage(): String {
        val displayName = if (username.isEmpty() || username == nickname) {
            nickname
        } else {
            "$nickname ($username)"
        }
        return "[$timestamp] <$displayName> $message"
    }
}

class ChatDialog(context: Context) : BaseSheetDialog(context) {
    private lateinit var binding: DialogChatBinding
    private lateinit var chatAdapter: ChatAdapter
    private val blockedUsers = mutableSetOf<String>()
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DialogChatBinding.inflate(LayoutInflater.from(context))
        setContentView(binding.root)

        NetPlayManager.setChatOpen(true)
        setupRecyclerView()
        scrollToBottom()

        NetPlayManager.setOnMessageReceivedListener { type, message ->
            handler.post {
                chatAdapter.notifyDataSetChanged()
                scrollToBottom()
            }
        }

        binding.sendButton.setOnClickListener {
            val message = binding.chatInput.text.toString()
            if (message.isNotBlank()) {
                sendMessage(message)
                binding.chatInput.text?.clear()
            }
        }
    }

    override fun dismiss() {
        NetPlayManager.setChatOpen(false)
        super.dismiss()
    }

    private fun sendMessage(message: String) {
        val username = NetPlayManager.getUsername(context)
        NetPlayManager.netPlaySendMessage(message)

        val chatMessage = ChatMessage(
            nickname = username,
            username = "",
            message = message,
            timestamp = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        )

        NetPlayManager.addChatMessage(chatMessage)
        chatAdapter.notifyDataSetChanged()
        scrollToBottom()
    }

    private fun setupRecyclerView() {
        chatAdapter = ChatAdapter(NetPlayManager.getChatMessages())
        binding.chatRecyclerView.layoutManager = LinearLayoutManager(context).apply {
            stackFromEnd = true // This makes it start from the bottom
        }
        binding.chatRecyclerView.adapter = chatAdapter
    }

    private fun scrollToBottom() {
        binding.chatRecyclerView.scrollToPosition(chatAdapter.itemCount - 1)
    }
}

class ChatAdapter(private val messages: List<ChatMessage>) :
    RecyclerView.Adapter<ChatAdapter.ChatViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val binding = ItemChatMessageBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ChatViewHolder(binding)
    }

    override fun getItemCount(): Int = messages.size

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        holder.bind(messages[position])
    }

    inner class ChatViewHolder(private val binding: ItemChatMessageBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(message: ChatMessage) {
            binding.usernameText.text = message.nickname
            binding.messageText.text = message.message
            binding.userIcon.setImageResource(when (message.nickname) {
                "System" -> R.drawable.ic_system
                else -> R.drawable.ic_user
            })
        }
    }
}
