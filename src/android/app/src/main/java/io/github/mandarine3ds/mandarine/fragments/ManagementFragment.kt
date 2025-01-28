// SPDX-FileCopyrightText: 2023 yuzu Emulator Project
// SPDX-License-Identifier: GPL-2.0-or-later
// Copyright 2025 Mandarine Project

package io.github.mandarine3ds.mandarine.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.transition.MaterialSharedAxis
import io.github.mandarine3ds.mandarine.R
import io.github.mandarine3ds.mandarine.adapters.ManagementAdapter
import io.github.mandarine3ds.mandarine.databinding.FragmentManagmentBinding
import io.github.mandarine3ds.mandarine.model.ManagementItems
import io.github.mandarine3ds.mandarine.ui.main.MainActivity
import io.github.mandarine3ds.mandarine.utils.SearchLocationHelper
import io.github.mandarine3ds.mandarine.utils.SearchLocationResult
import io.github.mandarine3ds.mandarine.viewmodel.GamesViewModel
import io.github.mandarine3ds.mandarine.viewmodel.HomeViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ManagementFragment : Fragment() {
    private var _binding: FragmentManagmentBinding? = null
    private val binding get() = _binding!!
    private val homeViewModel: HomeViewModel by activityViewModels()
    private val mainActivity get() = requireActivity() as MainActivity
    private val gamesViewModel: GamesViewModel by activityViewModels()

    override fun onCreate(
        savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enterTransition = MaterialSharedAxis(MaterialSharedAxis.X, true)
        returnTransition = MaterialSharedAxis(MaterialSharedAxis.X, false)
        reenterTransition = MaterialSharedAxis(MaterialSharedAxis.X, false)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentManagmentBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.toolbarInstallables.setNavigationOnClickListener {
            binding.root.findNavController().popBackStack()
        }

        val items = mutableListOf(
            ManagementItems(
                R.string.install_game_content,
                R.string.install_game_content_description,
                install = { mainActivity.ciaFileInstaller.launch(true) }
            ),
            ManagementItems(
                R.string.select_mandarine_user_folder,
                R.string.select_mandarine_user_folder_home_description,
                set = { mainActivity.openMandarineDirectory.launch(null) },
                details = homeViewModel.userDir.value
            ),
            ManagementItems(
                R.string.search_location,
                R.string.search_location_description,
                set = { documentPicker.launch(null) },
                directories = SearchLocationHelper.getSearchLocations(requireContext()),
                onDeleteDirectory = { uri ->
                    SearchLocationHelper.deleteLocation(requireContext(), uri)
                    gamesViewModel.reloadGames(false)
                    updateSearchLocationItem()
                }
            )
        )

        binding.listInstallables.apply {
            layoutManager = GridLayoutManager(
                requireContext(),
                resources.getInteger(R.integer.game_grid_columns)
            )
            adapter = ManagementAdapter(items)
        }

        // Update userDir on change
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                homeViewModel.userDir.collectLatest { newUserDir ->
                    (binding.listInstallables.adapter as ManagementAdapter<ManagementItems>).updateItem(
                        1,
                        ManagementItems(
                            R.string.select_mandarine_user_folder,
                            R.string.select_mandarine_user_folder_home_description,
                            set = { mainActivity.openMandarineDirectory.launch(null) },
                            details = newUserDir
                        )
                    )
                }
            }
        }

        setInsets()
    }

    private fun setInsets() =
        ViewCompat.setOnApplyWindowInsetsListener(
            binding.root
        ) { _: View, windowInsets: WindowInsetsCompat ->
            val barInsets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            windowInsets.getInsets(WindowInsetsCompat.Type.displayCutout())

            homeViewModel.setNavigationVisibility(visible = false, animated = true)
            homeViewModel.setStatusBarShadeVisibility(visible = false)

            binding.listInstallables.updatePadding(bottom = barInsets.bottom)

            windowInsets
        }

    // Workaround for updating the search location item
    private fun updateSearchLocationItem() {
        (binding.listInstallables.adapter as ManagementAdapter<ManagementItems>).updateItem(
            2,
            ManagementItems(
                R.string.search_location,
                R.string.search_location_description,
                set = { documentPicker.launch(null) },
                directories = SearchLocationHelper.getSearchLocations(requireContext()),
                onDeleteDirectory = { uri ->
                    SearchLocationHelper.deleteLocation(requireContext(), uri)
                    gamesViewModel.reloadGames(false)
                    updateSearchLocationItem()
                }
            )
        )
    }

    // Document picker for search location
    private val documentPicker =
        registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
            uri?.let {
                requireContext().contentResolver.takePersistableUriPermission(
                    it,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )

                // Show a toast message if we're adding the same directory again
                val existingLocations = SearchLocationHelper.getSearchLocations(requireContext())
                if (existingLocations.any { existing -> existing.path == it.path }) {
                    Toast.makeText(
                        requireContext(),
                        R.string.directory_already_added,
                        Toast.LENGTH_SHORT
                    ).show()
                    return@let
                }

                val result = SearchLocationHelper.addLocation(requireContext(), it)
                if (result == SearchLocationResult.Success) {
                    updateSearchLocationItem()
                    gamesViewModel.reloadGames(false)
                }
            }
        }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}