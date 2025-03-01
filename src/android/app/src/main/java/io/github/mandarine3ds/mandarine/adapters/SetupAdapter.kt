// Copyright 2025 Citra / Mandarine Project
// Licensed under GPLv2 or any later version
// Refer to the license.txt file included.

package io.github.mandarine3ds.mandarine.adapters

import android.content.res.ColorStateList
import android.text.Html
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import io.github.mandarine3ds.mandarine.R
import io.github.mandarine3ds.mandarine.databinding.PageSetupBinding
import io.github.mandarine3ds.mandarine.model.ButtonState
import io.github.mandarine3ds.mandarine.model.SetupCallback
import io.github.mandarine3ds.mandarine.model.SetupPage
import io.github.mandarine3ds.mandarine.model.PageState
import io.github.mandarine3ds.mandarine.utils.ViewUtils

class SetupAdapter(val activity: AppCompatActivity, val pages: List<SetupPage>) :
    RecyclerView.Adapter<SetupAdapter.SetupPageViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SetupPageViewHolder {
        val binding = PageSetupBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return SetupPageViewHolder(binding)
    }

    override fun getItemCount(): Int = pages.size

    override fun onBindViewHolder(holder: SetupPageViewHolder, position: Int) =
        holder.bind(pages[position])

    inner class SetupPageViewHolder(val binding: PageSetupBinding) :
        RecyclerView.ViewHolder(binding.root), SetupCallback {
        lateinit var page: SetupPage

        init {
            itemView.tag = this
        }

        fun bind(page: SetupPage) {
            this.page = page


            if (page.pageSteps.invoke() == PageState.PAGE_STEPS_COMPLETE) {
                onStepCompleted(0, pageFullyCompleted = true)
            }

            if (page.pageButton != null && page.pageSteps.invoke() != PageState.PAGE_STEPS_COMPLETE) {
                for (pageButton in page.pageButton) {
                    val pageButtonView = LayoutInflater.from(activity)
                        .inflate(
                            R.layout.page_button,
                            binding.pegeButtonContainer,
                            false
                        ) as MaterialButton

                    pageButtonView.apply {
                        id = pageButton.titleId
                        icon = ResourcesCompat.getDrawable(
                            activity.resources,
                            pageButton.iconId,
                            activity.theme
                        )
                        text = activity.resources.getString(pageButton.titleId)
                    }

                    pageButtonView.setOnClickListener {
                        pageButton.buttonAction.invoke(this@SetupPageViewHolder)
                    }

                    binding.pegeButtonContainer.addView(pageButtonView)

                    // Disable buton on add if its already completed
                    if (pageButton.buttonState.invoke() == ButtonState.BUTTON_ACTION_COMPLETE) {
                        onStepCompleted(pageButton.titleId, pageFullyCompleted = false)
                    }
                }
            }

            binding.icon.setImageDrawable(
                ResourcesCompat.getDrawable(
                    activity.resources,
                    page.iconId,
                    activity.theme
                )
            )
            binding.textTitle.text = activity.resources.getString(page.titleId)
            binding.textDescription.text =
                Html.fromHtml(activity.resources.getString(page.descriptionId), 0)
            binding.textDescription.movementMethod = LinkMovementMethod.getInstance()
        }

        override fun onStepCompleted(pageButtonId: Int, pageFullyCompleted: Boolean) {
            val button = binding.pegeButtonContainer.findViewById<MaterialButton>( pageButtonId)

            if (pageFullyCompleted) {
                ViewUtils.hideView(binding.pegeButtonContainer, 200)
                ViewUtils.showView(binding.textConfirmation, 200)
            }

            if (button != null) {
                button.isEnabled = false
                button.animate()
                    .alpha(0.38f)
                    .setDuration(200)
                    .start()
                button.setTextColor(button.context.getColor(com.google.android.material.R.color.material_on_surface_disabled))
                button.iconTint = ColorStateList.valueOf(button.context.getColor(com.google.android.material.R.color.material_on_surface_disabled))
            }


        }
    }
}
