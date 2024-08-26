// Copyright 2024 Mandarine Project
// Licensed under GPLv2 or any later version
// Refer to the license.txt file included.

package io.github.mandarine3ds.mandarine.dialogs

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.github.mandarine3ds.mandarine.NativeLibrary
import io.github.mandarine3ds.mandarine.R
import io.github.mandarine3ds.mandarine.activities.EmulationActivity
import io.github.mandarine3ds.mandarine.databinding.DialogMultiplayerBinding
import io.github.mandarine3ds.mandarine.databinding.ItemButtonNetplayBinding
import io.github.mandarine3ds.mandarine.databinding.ItemTextNetplayBinding
import io.github.mandarine3ds.mandarine.ui.main.MainActivity
import io.github.mandarine3ds.mandarine.utils.NetPlayManager

class NetPlayDialog(context: Context) : BaseSheetDialog(context) {
    private lateinit var adapter: NetPlayAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = DialogMultiplayerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.listMultiplayer.layoutManager = LinearLayoutManager(context)
        adapter = NetPlayAdapter()
        binding.listMultiplayer.adapter = adapter
        adapter.loadMultiplayerMenu()
    }

    data class NetPlayItems(
        val option: Int,
        val name: String,
        val type: Int,
        var value: Int
    ) {
        companion object {
            // multiplayer
            const val MULTIPLAYER_ROOM_TEXT = 0
            const val MULTIPLAYER_CREATE_ROOM = 1
            const val MULTIPLAYER_JOIN_ROOM = 2
            const val MULTIPLAYER_ROOM_MEMBER = 3
            const val MULTIPLAYER_EXIT_ROOM = 4

            // view type
            const val TYPE_BUTTON = 0
            const val TYPE_TEXT = 1
        }
    }

    abstract class NetPlayViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView), View.OnClickListener {
        init {
            itemView.setOnClickListener(this)
            findViews(itemView)
        }

        protected abstract fun findViews(root: View)
        abstract fun bind(item: NetPlayItems)
        abstract override fun onClick(clicked: View)
    }

    inner class TextNetPlayViewHolder(val binding: ItemTextNetplayBinding, itemView: View) : NetPlayViewHolder(itemView) {
        private lateinit var netPlayItem: NetPlayItems

        override fun findViews(root: View) {
            // Views are already initialized in property declaration
        }

        // this code is kinda hacky, should be improved on the future
        override fun onClick(clicked: View) {
            val emulationActivity = NativeLibrary.sEmulationActivity.get()
            val mainActivity = MainActivity.get()

            when (netPlayItem.option) {
                NetPlayItems.MULTIPLAYER_CREATE_ROOM -> {
                    if (mainActivity != null && !EmulationActivity.isRunning()) {
                        NetPlayManager.showCreateRoomDialog(mainActivity)
                    }
                    if (emulationActivity != null) {
                        NetPlayManager.showCreateRoomDialog(emulationActivity)
                    }
                    dismiss()
                }
                NetPlayItems.MULTIPLAYER_JOIN_ROOM -> {
                    if (mainActivity != null && !EmulationActivity.isRunning()) {
                        NetPlayManager.showJoinRoomDialog(mainActivity)
                    }
                    if (emulationActivity != null) {
                        NetPlayManager.showJoinRoomDialog(emulationActivity)
                    }
                    dismiss()
                }
                NetPlayItems.MULTIPLAYER_EXIT_ROOM -> {
                    NetPlayManager.netPlayLeaveRoom()
                    dismiss()
                }
            }
        }

        override fun bind(item: NetPlayItems) {
            netPlayItem = item
            binding.itemTextNetplayName.text = netPlayItem.name
        }
    }

    inner class ButtonNetPlayViewHolder(val binding: ItemButtonNetplayBinding, itemView: View) : NetPlayViewHolder(itemView) {
        private lateinit var netPlayItems: NetPlayItems

        init {
            itemView.setOnClickListener(null)
            binding.itemButtonNetplay.setText(R.string.multiplayer_kick_member)
        }

        override fun findViews(root: View) {
            // Views are already initialized in property declaration
        }

        override fun bind(item: NetPlayItems) {
            netPlayItems = item
            binding.itemButtonNetplayName.text = netPlayItems.name
            binding.itemButtonNetplay.setOnClickListener { onClick(it) }
        }

        override fun onClick(clicked: View) {
            if (netPlayItems.option == NetPlayItems.MULTIPLAYER_ROOM_MEMBER) {
                var text = netPlayItems.name
                val pos = text.indexOf('[')
                if (pos > 0) {
                    text = text.substring(0, pos - 1)
                }
                NetPlayManager.netPlayKickUser(text)
            }
        }
    }

    inner class NetPlayAdapter : RecyclerView.Adapter<NetPlayViewHolder>() {
        private val netPlayItems = mutableListOf<NetPlayItems>()

        fun loadMultiplayerMenu() {
            val infos = NetPlayManager.netPlayRoomInfo()

            if (infos.isNotEmpty()) {
                val roomTitle = context.getString(R.string.multiplayer_room_title, infos[0])
                netPlayItems.add(NetPlayItems(NetPlayItems.MULTIPLAYER_ROOM_TEXT, roomTitle, NetPlayItems.TYPE_TEXT, 0))
                if (NetPlayManager.netPlayIsHostedRoom()) {
                    for (i in 1 until infos.size) {
                        netPlayItems.add(NetPlayItems(NetPlayItems.MULTIPLAYER_ROOM_MEMBER, infos[i], NetPlayItems.TYPE_BUTTON, 0))
                    }
                } else {
                    for (i in 1 until infos.size) {
                        netPlayItems.add(NetPlayItems(NetPlayItems.MULTIPLAYER_ROOM_MEMBER, infos[i], NetPlayItems.TYPE_TEXT, 0))
                    }
                }
                netPlayItems.add(NetPlayItems(NetPlayItems.MULTIPLAYER_EXIT_ROOM, context.getString(R.string.multiplayer_exit_room), NetPlayItems.TYPE_TEXT, 0))
            } else {
                val consoleTitle = context.getString(R.string.multiplayer_console_id, NetPlayManager.netPlayGetConsoleId())
                netPlayItems.add(NetPlayItems(NetPlayItems.MULTIPLAYER_ROOM_TEXT, consoleTitle, NetPlayItems.TYPE_TEXT, 0))
                netPlayItems.add(NetPlayItems(NetPlayItems.MULTIPLAYER_CREATE_ROOM, context.getString(R.string.multiplayer_create_room), NetPlayItems.TYPE_TEXT, 0))
                netPlayItems.add(NetPlayItems(NetPlayItems.MULTIPLAYER_JOIN_ROOM, context.getString(R.string.multiplayer_join_room), NetPlayItems.TYPE_TEXT, 0))
            }
        }

        override fun getItemViewType(position: Int): Int {
            return netPlayItems[position].type
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NetPlayViewHolder {
            val inflater = LayoutInflater.from(parent.context)
            val textBinding = ItemTextNetplayBinding.inflate(inflater, parent, false)
            val buttonBinding = ItemButtonNetplayBinding.inflate(inflater, parent, false)
            return when (viewType) {
                NetPlayItems.TYPE_TEXT -> TextNetPlayViewHolder(textBinding, textBinding.root)
                NetPlayItems.TYPE_BUTTON -> ButtonNetPlayViewHolder(buttonBinding, buttonBinding.root)
                else -> throw IllegalStateException("Unsupported view type")
            }
        }

        override fun onBindViewHolder(holder: NetPlayViewHolder, position: Int) {
            holder.bind(netPlayItems[position])
        }

        override fun getItemCount(): Int {
            return netPlayItems.size
        }
    }
}
