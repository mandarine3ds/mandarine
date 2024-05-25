// Copyright 2023 Citra Emulator Project
// Licensed under GPLv2 or any later version
// Refer to the license.txt file included.

package org.citra.citra_emu.features.settings.ui.viewholder

import android.view.View
import android.widget.CompoundButton
import org.citra.citra_emu.databinding.ListItemSettingSwitchBinding
import org.citra.citra_emu.features.settings.model.view.SettingsItem
import org.citra.citra_emu.features.settings.model.view.SwitchSetting
import org.citra.citra_emu.features.settings.ui.SettingsAdapter
import org.citra.citra_emu.utils.GpuDriverHelper
import org.citra.citra_emu.R

class SwitchSettingViewHolder(val binding: ListItemSettingSwitchBinding, adapter: SettingsAdapter) :
    SettingViewHolder(binding.root, adapter) {

    private lateinit var setting: SwitchSetting
    private lateinit var settingitem: SettingsItem
    
    override fun bind(item: SettingsItem) {
        setting = item as SwitchSetting
        settingitem = item
        binding.textSettingName.setText(item.nameId)
        if (item.descriptionId != 0) {
            binding.textSettingDescription.setText(item.descriptionId)
            binding.textSettingDescription.visibility = View.VISIBLE
        } else {
            binding.textSettingDescription.text = ""
            binding.textSettingDescription.visibility = View.GONE
        }

        binding.switchWidget.setOnCheckedChangeListener(null)
        binding.switchWidget.isChecked = setting.isChecked
        binding.switchWidget.setOnCheckedChangeListener { _: CompoundButton, _: Boolean ->
            adapter.onBooleanClick(item, bindingAdapterPosition, binding.switchWidget.isChecked)
        }

        binding.switchWidget.isEnabled = if (setting.isEditable) {
        isForceMaxGpuClocksClickable()
        } else { 
            setting.isEditable
        }

        if (setting.isEditable) {
            if (!isForceMaxGpuClocksClickable()) {
                binding.textSettingName.alpha = 0.5f
                binding.textSettingDescription.alpha = 0.5f
            } else {
                binding.textSettingName.alpha = 1f
                binding.textSettingDescription.alpha = 1f
            }
        } else {
            binding.textSettingName.alpha = 0.5f
            binding.textSettingDescription.alpha = 0.5f
        }
    }

    override fun onClick(clicked: View) {
        if (setting.isEditable) {
            if (!isForceMaxGpuClocksClickable()) { 
                adapter.onForceMaximumGpuClocksDisabled()
            } else {
                binding.switchWidget.toggle()
            }
        } else {
            adapter.onClickDisabledSetting()
        }
    }

    override fun onLongClick(clicked: View): Boolean {
        if (setting.isEditable) {
            if (!isForceMaxGpuClocksClickable()) { 
                adapter.onForceMaximumGpuClocksDisabled()
                return false
            } else {
                return adapter.onLongClick(setting.setting!!, bindingAdapterPosition)
            }
        } else {
            adapter.onClickDisabledSetting()
        }
        return false
    }

    private fun isForceMaxGpuClocksClickable(): Boolean {
        return if (settingitem.nameId == R.string.force_max_gpu_clocks) {
            GpuDriverHelper.supportsCustomDriverLoading()
        } else {
            true
        }
    }
        
}
