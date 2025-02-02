// Copyright 2025 Citra Project / Mandarine Project
// Licensed under GPLv2 or any later version
// Refer to the license.txt file included.

package io.github.mandarine3ds.mandarine.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.core.widget.doOnTextChanged
import androidx.documentfile.provider.DocumentFile
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.transition.MaterialSharedAxis
import io.github.mandarine3ds.mandarine.MandarineApplication
import io.github.mandarine3ds.mandarine.HomeNavigationDirections
import io.github.mandarine3ds.mandarine.R
import io.github.mandarine3ds.mandarine.adapters.HomeSettingAdapter
import io.github.mandarine3ds.mandarine.databinding.DialogSoftwareKeyboardBinding
import io.github.mandarine3ds.mandarine.databinding.FragmentHomeSettingsBinding
import io.github.mandarine3ds.mandarine.features.settings.model.Settings
import io.github.mandarine3ds.mandarine.features.settings.ui.SettingsActivity
import io.github.mandarine3ds.mandarine.features.settings.utils.SettingsFile
import io.github.mandarine3ds.mandarine.model.Game
import io.github.mandarine3ds.mandarine.model.HomeSetting
import io.github.mandarine3ds.mandarine.ui.main.MainActivity
import io.github.mandarine3ds.mandarine.utils.GpuDriverHelper
import io.github.mandarine3ds.mandarine.utils.SearchLocationHelper
import io.github.mandarine3ds.mandarine.utils.Log
import io.github.mandarine3ds.mandarine.utils.PermissionsHandler
import io.github.mandarine3ds.mandarine.viewmodel.DriverViewModel
import io.github.mandarine3ds.mandarine.viewmodel.HomeViewModel

class HomeSettingsFragment : Fragment() {
    private var _binding: FragmentHomeSettingsBinding? = null
    private val binding get() = _binding!!

    private lateinit var mainActivity: MainActivity

    private val homeViewModel: HomeViewModel by activityViewModels()
    private val driverViewModel: DriverViewModel by activityViewModels()

    private val preferences get() =
        PreferenceManager.getDefaultSharedPreferences(MandarineApplication.appContext)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        reenterTransition = MaterialSharedAxis(MaterialSharedAxis.X, false)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeSettingsBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        mainActivity = requireActivity() as MainActivity
        SearchLocationHelper.getSearchLocations(requireContext())

        val optionsList = listOf(
            HomeSetting(
                R.string.grid_menu_core_settings,
                R.string.settings_description,
                R.drawable.ic_settings,
                { SettingsActivity.launch(requireContext(), SettingsFile.FILE_NAME_CONFIG, "") }
            ),
            HomeSetting(
                R.string.artic_base_connect,
                R.string.artic_base_connect_description,
                R.drawable.ic_network,
                {
                    val inflater = LayoutInflater.from(context)
                    val inputBinding = DialogSoftwareKeyboardBinding.inflate(inflater)
                    var textInputValue: String = preferences.getString("lastArticBaseAddr", "")!!

                    inputBinding.editTextInput.setText(textInputValue)
                    inputBinding.editTextInput.doOnTextChanged { text, _, _, _ ->
                        textInputValue = text.toString()
                    }

                    context?.let {
                        MaterialAlertDialogBuilder(it)
                            .setView(inputBinding.root)
                            .setTitle(getString(R.string.artic_base_enter_address))
                            .setPositiveButton(android.R.string.ok) { _, _ ->
                                if (textInputValue.isNotEmpty()) {
                                    preferences.edit()
                                        .putString("lastArticBaseAddr", textInputValue)
                                        .apply()
                                    val menu = Game(
                                        title = getString(R.string.artic_base),
                                        path = "articbase://$textInputValue",
                                        filename = ""
                                    )
                                    val action =
                                        HomeNavigationDirections.actionGlobalEmulationActivity(menu)
                                    binding.root.findNavController().navigate(action)
                                }
                            }
                            .setNegativeButton(android.R.string.cancel) {_, _ -> }
                            .show()
                    }
                }
            ),
            HomeSetting(
                R.string.system_files,
                R.string.system_files_description,
                R.drawable.ic_system_update,
                {
                    exitTransition = MaterialSharedAxis(MaterialSharedAxis.X, true)
                    parentFragmentManager.primaryNavigationFragment?.findNavController()
                        ?.navigate(R.id.action_homeSettingsFragment_to_systemFilesFragment)
                }
            ),
            HomeSetting(
                R.string.manage_emulator_data_title,
                R.string.manage_emulator_data_description,
                R.drawable.ic_folder_data,
                {                     exitTransition = MaterialSharedAxis(MaterialSharedAxis.X, true)
                    parentFragmentManager.primaryNavigationFragment?.findNavController()
                        ?.navigate(R.id.action_homeSettingsFragment_to_managmentFragment) }
            ),
            HomeSetting(
                R.string.multiplayer,
                R.string.multiplayer_description,
                R.drawable.ic_multiplayer,
                { mainActivity.displayMultiplayerDialog() }
            ),
            HomeSetting(
                R.string.share_log,
                R.string.share_log_description,
                R.drawable.ic_share,
                { shareLog() }
            ),
            HomeSetting(
                R.string.gpu_driver_manager,
                R.string.install_gpu_driver_description,
                R.drawable.ic_install_driver,
                {
                    binding.root.findNavController()
                        .navigate(R.id.action_homeSettingsFragment_to_driverManagerFragment)
                },
                { true },
                R.string.custom_driver_not_supported,
                R.string.custom_driver_not_supported_description,
                driverViewModel.selectedDriverMetadata
            ),
            HomeSetting(
                R.string.preferences_theme,
                R.string.theme_and_color_description,
                R.drawable.ic_palette,
                { SettingsActivity.launch(requireContext(), Settings.SECTION_THEME, "") }
            ),
            HomeSetting(
                R.string.about,
                R.string.about_description,
                R.drawable.ic_info_outline,
                {
                    exitTransition = MaterialSharedAxis(MaterialSharedAxis.X, true)
                    parentFragmentManager.primaryNavigationFragment?.findNavController()
                        ?.navigate(R.id.action_homeSettingsFragment_to_aboutFragment)
                }
            )
        )

        binding.homeSettingsList.apply {
            layoutManager = GridLayoutManager(
                requireContext(),
                resources.getInteger(R.integer.game_grid_columns)
            )
            adapter = HomeSettingAdapter(
                requireActivity() as AppCompatActivity,
                viewLifecycleOwner,
                optionsList
            )
        }

        setInsets()
    }

    override fun onStart() {
        super.onStart()
        exitTransition = null
        homeViewModel.setStatusBarShadeVisibility(visible = true)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun shareLog() {
        val logDirectory = DocumentFile.fromTreeUri(
            requireContext(),
            PermissionsHandler.mandarineDirectory
        )?.findFile("log")
        val currentLog = logDirectory?.findFile("mandarine_log.txt")
        val oldLog = logDirectory?.findFile("mandarine_log.txt.old.txt")

        val intent = Intent().apply {
            action = Intent.ACTION_SEND
            type = "text/plain"
        }
        if (!Log.gameLaunched && oldLog?.exists() == true) {
            intent.putExtra(Intent.EXTRA_STREAM, oldLog.uri)
            startActivity(Intent.createChooser(intent, getText(R.string.share_log)))
        } else if (currentLog?.exists() == true) {
            intent.putExtra(Intent.EXTRA_STREAM, currentLog.uri)
            startActivity(Intent.createChooser(intent, getText(R.string.share_log)))
        } else {
            Toast.makeText(
                requireContext(),
                getText(R.string.share_log_not_found),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun setInsets() =
        ViewCompat.setOnApplyWindowInsetsListener(
            binding.root
        ) { view: View, windowInsets: WindowInsetsCompat ->
            val barInsets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            val cutoutInsets = windowInsets.getInsets(WindowInsetsCompat.Type.displayCutout())
            val spacingNavigation = resources.getDimensionPixelSize(R.dimen.spacing_navigation)
            val spacingNavigationRail =
                resources.getDimensionPixelSize(R.dimen.spacing_navigation_rail)

            val leftInsets = barInsets.left + cutoutInsets.left
            val rightInsets = barInsets.right + cutoutInsets.right

            binding.scrollViewSettings.updatePadding(
                top = barInsets.top,
                bottom = barInsets.bottom
            )

            val mlpScrollSettings = binding.scrollViewSettings.layoutParams as MarginLayoutParams
            mlpScrollSettings.leftMargin = leftInsets
            mlpScrollSettings.rightMargin = rightInsets
            binding.scrollViewSettings.layoutParams = mlpScrollSettings

            binding.linearLayoutSettings.updatePadding(bottom = spacingNavigation)

            if (ViewCompat.getLayoutDirection(view) == ViewCompat.LAYOUT_DIRECTION_LTR) {
                binding.linearLayoutSettings.updatePadding(left = spacingNavigationRail)
            } else {
                binding.linearLayoutSettings.updatePadding(right = spacingNavigationRail)
            }

            windowInsets
        }
}
