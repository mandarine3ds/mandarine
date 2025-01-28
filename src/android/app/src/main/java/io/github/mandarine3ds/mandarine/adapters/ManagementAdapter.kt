// SPDX-FileCopyrightText: 2023 yuzu Emulator Project
// SPDX-License-Identifier: GPL-2.0-or-later
// Copyright 2025 Mandarine Project

package io.github.mandarine3ds.mandarine.adapters


import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import io.github.mandarine3ds.mandarine.databinding.CardManagementBinding
import io.github.mandarine3ds.mandarine.model.ManagementItems
import io.github.mandarine3ds.mandarine.utils.ViewUtils.setVisible
import com.google.android.material.divider.MaterialDivider
import io.github.mandarine3ds.mandarine.R

class ManagementAdapter<T>(items: List<T>) :
    RecyclerView.Adapter<ManagementAdapter<T>.ManagementViewHolder>() {

    private val items = items.toMutableList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ManagementViewHolder {
        val binding = CardManagementBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ManagementViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ManagementViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount() = items.size

    // Used to update the search location folder items
    fun updateItem(position: Int, item: T) {
        if (position < items.size) {
            items[position] = item
            notifyItemChanged(position)
        }
    }

    inner class ManagementViewHolder(private val binding: CardManagementBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: T) {
            when (item) {
                is ManagementItems -> {
                    binding.title.setText(item.titleId)
                    binding.description.setText(item.descriptionId)
                    binding.details.text = item.details
                    binding.details.setVisible(item.details != null)

                    binding.buttonSet.setVisible(item.set != null)
                    binding.buttonSet.setOnClickListener { item.set?.invoke() }

                    binding.buttonInstall.setVisible(item.install != null)
                    binding.buttonInstall.setOnClickListener { item.install?.invoke() }

                    binding.directoriesContainer.removeAllViews()


                    val hasDirectories = item.directories?.isNotEmpty() == true
                    binding.directoriesDivider.setVisible(hasDirectories)
                    binding.directoriesContainer.setVisible(hasDirectories)

                    if (hasDirectories) {
                        // We need to set padding programmatically because the to avoid all cards getting extra padding
                        binding.directoriesContainer.setPadding(
                            binding.root.resources.getDimensionPixelSize(R.dimen.spacing_xlarge), // 24dp
                            binding.root.resources.getDimensionPixelSize(R.dimen.spacing_large), // 16dp
                            binding.root.resources.getDimensionPixelSize(R.dimen.spacing_xlarge), // 24dp
                            binding.root.resources.getDimensionPixelSize(R.dimen.spacing_large)  // 16dp
                        )

                        item.directories?.forEach { uri ->
                            val directoryView = LayoutInflater.from(binding.root.context)
                                .inflate(R.layout.card_search_directory, binding.directoriesContainer, false)

                            directoryView.findViewById<TextView>(R.id.directory_name).text =
                                uri.lastPathSegment?.substringAfterLast('/')
                            directoryView.findViewById<TextView>(R.id.directory_path).text =
                                uri.path?.replace("%3A", ":")?.replace("%2F", "/")

                            directoryView.findViewById<ImageView>(R.id.delete_button).setOnClickListener {
                                item.onDeleteDirectory?.invoke(uri)
                            }

                            binding.directoriesContainer.addView(directoryView)

                            if (uri != item.directories?.last()) {
                                val divider = MaterialDivider(binding.root.context)
                                val params = LinearLayout.LayoutParams(
                                    LinearLayout.LayoutParams.MATCH_PARENT,
                                    LinearLayout.LayoutParams.WRAP_CONTENT
                                )
                                params.topMargin = binding.root.resources.getDimensionPixelSize(R.dimen.spacing_med)
                                params.bottomMargin = binding.root.resources.getDimensionPixelSize(R.dimen.spacing_med)
                                divider.layoutParams = params
                                binding.directoriesContainer.addView(divider)
                            }
                        }
                    }
                }
            }
        }
    }
}
