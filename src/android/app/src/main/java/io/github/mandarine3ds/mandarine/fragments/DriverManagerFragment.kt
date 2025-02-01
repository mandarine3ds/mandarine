// Copyright 2025 Citra Project / Mandarine Project
// Licensed under GPLv2 or any later version
// Refer to the license.txt file included.

package io.github.mandarine3ds.mandarine.fragments

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.core.widget.doOnTextChanged
import androidx.documentfile.provider.DocumentFile
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.transition.MaterialSharedAxis
import io.github.mandarine3ds.mandarine.MandarineApplication
import io.github.mandarine3ds.mandarine.R
import io.github.mandarine3ds.mandarine.adapters.DriverAdapter
import io.github.mandarine3ds.mandarine.databinding.DialogSoftwareKeyboardBinding
import io.github.mandarine3ds.mandarine.databinding.FragmentDriverManagerBinding
import io.github.mandarine3ds.mandarine.utils.DirectoryInitialization
import io.github.mandarine3ds.mandarine.utils.DirectoryInitialization.userDirectory
import io.github.mandarine3ds.mandarine.utils.DriversFetcher
import io.github.mandarine3ds.mandarine.utils.DriversFetcher.DownloadResult
import io.github.mandarine3ds.mandarine.utils.FileUtil
import io.github.mandarine3ds.mandarine.utils.FileUtil.inputStream
import io.github.mandarine3ds.mandarine.utils.GpuDriverHelper
import io.github.mandarine3ds.mandarine.viewmodel.DriverViewModel
import io.github.mandarine3ds.mandarine.viewmodel.HomeViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.IOException

class DriverManagerFragment : Fragment() {
    private var _binding: FragmentDriverManagerBinding? = null
    private val binding get() = _binding!!

    private val homeViewModel: HomeViewModel by activityViewModels()
    private val driverViewModel: DriverViewModel by activityViewModels()
    private val tempDriverZipFile: DocumentFile
        get() {
            val root = DocumentFile.fromTreeUri(
                MandarineApplication.appContext,
                Uri.parse(DirectoryInitialization.userPath)
            )!!
            var driverDirectory = root.findFile("cache")
            if (driverDirectory == null) {
                driverDirectory = FileUtil.createDir(root.uri.toString(), "cache")
            }
            return driverDirectory!!
        }


    override fun onCreate(savedInstanceState: Bundle?) {
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
        _binding = FragmentDriverManagerBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        homeViewModel.setNavigationVisibility(visible = false, animated = true)
        homeViewModel.setStatusBarShadeVisibility(visible = false)

        if (!driverViewModel.isInteractionAllowed) {
            DriversLoadingDialogFragment().show(
                childFragmentManager,
                DriversLoadingDialogFragment.TAG
            )
        }

        if (!GpuDriverHelper.supportsCustomDriverLoading()) {
            binding.buttonInstall.visibility = View.GONE
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Unsupported")
                .setMessage(R.string.custom_driver_not_supported_description)
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    driverViewModel.setSelectedDriverIndex(0)
                }
                .show()
        }

        binding.toolbarDrivers.setNavigationOnClickListener {
            binding.root.findNavController().popBackStack()
        }

        binding.buttonInstall.setOnClickListener {
            val items = arrayOf("Import", "Install")
            var checkedItem = 0
            var selectedItem: String? = items[0]

            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Choose")
                .setSingleChoiceItems(items, checkedItem) { dialog, which ->
                    selectedItem = items[which]
                }
                .setPositiveButton(android.R.string.ok) { dialog, _ ->
                    if (selectedItem == "Install") {
                        getDriver.launch(arrayOf("application/zip"))
                    } else {
                        handleGpuDriverImport()
                    }
                }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
        }

        binding.listDrivers.apply {
            layoutManager = GridLayoutManager(
                requireContext(),
                resources.getInteger(R.integer.game_grid_columns)
            )
            adapter = DriverAdapter(driverViewModel)
        }

        viewLifecycleOwner.lifecycleScope.apply {
            launch {
                driverViewModel.driverList.collectLatest {
                    (binding.listDrivers.adapter as DriverAdapter).submitList(it)
                }
            }
            launch {
                driverViewModel.newDriverInstalled.collect {
                    if (_binding != null && it) {
                        (binding.listDrivers.adapter as DriverAdapter).apply {
                            notifyItemChanged(driverViewModel.previouslySelectedDriver)
                            notifyItemChanged(driverViewModel.selectedDriver)
                            driverViewModel.setNewDriverInstalled(false)
                        }
                    }
                }
            }
        }

        setInsets()
    }

    // Start installing requested driver
    override fun onStop() {
        super.onStop()
        driverViewModel.onCloseDriverManager()
    }

    private fun handleGpuDriverImport() {
        val inflater = LayoutInflater.from(requireContext())
        val inputBinding = DialogSoftwareKeyboardBinding.inflate(inflater)
        var textInputValue: String = "https://github.com/K11MCH1/AdrenoToolsDrivers"

        inputBinding.editTextInput.setText(textInputValue)
        inputBinding.editTextInput.doOnTextChanged { text, _, _, _ ->
            textInputValue = text.toString()
        }

        MaterialAlertDialogBuilder(requireContext())
            .setView(inputBinding.root)
            .setTitle("Enter repo url")
            .setPositiveButton("Fetch") { _, _ ->
                if (textInputValue.isNotEmpty()) {
                     fetchAndShowDrivers(textInputValue)
                }
            }
            .setNegativeButton(android.R.string.cancel) {_, _ -> }
            .show()
    }

    private fun fetchAndShowDrivers(repoUrl: String) {
        lifecycleScope.launch(Dispatchers.Main) {
            val releases = DriversFetcher.fetchReleases(repoUrl)
            if (releases.isEmpty()) {
                Snackbar.make(binding.root, "Failed to fetch ${repoUrl}: validation failed or check your internet connection", Snackbar.LENGTH_SHORT).show()
                return@launch
            }

            val releaseNames = releases.map { it.first }
            val releaseUrls = releases.map { it.second }
            var chosenUrl: String? = releaseUrls[0]
            var chosenName: String? = releaseNames[0]

            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Drivers")
                .setSingleChoiceItems(releaseNames.toTypedArray(), 0) { _, which ->
                    chosenUrl = releaseUrls[which]
                    chosenName = releaseNames[which]
                }
                .setPositiveButton("Import") { _, _ ->
                    downloadDriver(chosenUrl!!, chosenName!!)
                }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
        }
    }

    private fun downloadDriver(chosenUrl: String, chosenName: String) {
        GlobalScope.launch(Dispatchers.Main) {
            val createZipFile = tempDriverZipFile.createFile("application/zip", chosenName)
            val driverFile: DocumentFile

            val progressDialog = MaterialAlertDialogBuilder(requireContext())
                .setTitle("Downloading Driver")
                .setView(R.layout.dialog_progress)
                .setCancelable(false)
                .create()
            progressDialog.show()

            val result = DriversFetcher.downloadAsset(requireContext(), chosenUrl, createZipFile!!.uri)
            progressDialog.dismiss()

            when (result) {
                is DownloadResult.Success -> {
                    try {
                        driverFile =
                            GpuDriverHelper.copyDriverToExternalStorage(createZipFile.uri)
                                ?: throw IOException("Driver failed validation!")
                    } catch (_: IOException) {
                        Snackbar.make(
                            binding.root,
                            "Failed to import ${chosenName}: ${"Driver failed validation!"}",
                            Snackbar.LENGTH_SHORT
                        ).show()
                        return@launch
                    }
                    IndeterminateProgressDialogFragment.newInstance(
                        requireActivity(),
                        R.string.installing_driver,
                        false
                    ) {
                        val driverData =
                            GpuDriverHelper.getMetadataFromZip(driverFile.inputStream())
                        val driverInList =
                            driverViewModel.driverList.value.firstOrNull { it.second == driverData }
                        if (driverInList != null) {
                            driverFile.delete()
                            Snackbar.make(
                                binding.root,
                                "Driver $chosenName already installed",
                                Snackbar.LENGTH_SHORT
                            ).show()
                        } else {
                            driverViewModel.addDriver(Pair(driverFile.uri, driverData))
                            driverViewModel.setNewDriverInstalled(true)
                        }
                        return@newInstance Any()
                    }.show(childFragmentManager, IndeterminateProgressDialogFragment.TAG)

                    tempDriverZipFile.delete()
                }

                is DownloadResult.Error -> Snackbar.make(
                    binding.root,
                    "Failed to import ${chosenName}: ${result.message}",
                    Snackbar.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun setInsets() =
        ViewCompat.setOnApplyWindowInsetsListener(
            binding.root
        ) { _: View, windowInsets: WindowInsetsCompat ->
            val barInsets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            val cutoutInsets = windowInsets.getInsets(WindowInsetsCompat.Type.displayCutout())

            val leftInsets = barInsets.left + cutoutInsets.left
            val rightInsets = barInsets.right + cutoutInsets.right

            val mlpAppBar = binding.toolbarDrivers.layoutParams as ViewGroup.MarginLayoutParams
            mlpAppBar.leftMargin = leftInsets
            mlpAppBar.rightMargin = rightInsets
            binding.toolbarDrivers.layoutParams = mlpAppBar

            val mlplistDrivers = binding.listDrivers.layoutParams as ViewGroup.MarginLayoutParams
            mlplistDrivers.leftMargin = leftInsets
            mlplistDrivers.rightMargin = rightInsets
            binding.listDrivers.layoutParams = mlplistDrivers

            val fabSpacing = resources.getDimensionPixelSize(R.dimen.spacing_fab)
            val mlpFab =
                binding.buttonInstall.layoutParams as ViewGroup.MarginLayoutParams
            mlpFab.leftMargin = leftInsets + fabSpacing
            mlpFab.rightMargin = rightInsets + fabSpacing
            mlpFab.bottomMargin = barInsets.bottom + fabSpacing
            binding.buttonInstall.layoutParams = mlpFab

            binding.listDrivers.updatePadding(
                bottom = barInsets.bottom +
                        resources.getDimensionPixelSize(R.dimen.spacing_bottom_list_fab)
            )

            windowInsets
        }

    private val getDriver =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { result ->
            if (result == null) {
                return@registerForActivityResult
            }

            IndeterminateProgressDialogFragment.newInstance(
                requireActivity(),
                R.string.installing_driver,
                false
            ) {
                // Ignore file exceptions when a user selects an invalid zip
                val driverFile: DocumentFile
                try {
                    driverFile = GpuDriverHelper.copyDriverToExternalStorage(result)
                        ?: throw IOException("Driver failed validation!")
                } catch (_: IOException) {
                    return@newInstance getString(R.string.select_gpu_driver_error)
                }

                val driverData = GpuDriverHelper.getMetadataFromZip(driverFile.inputStream())
                val driverInList =
                    driverViewModel.driverList.value.firstOrNull { it.second == driverData }
                if (driverInList != null) {
                    driverFile.delete()
                    return@newInstance getString(R.string.driver_already_installed)
                } else {
                    driverViewModel.addDriver(Pair(driverFile.uri, driverData))
                    driverViewModel.setNewDriverInstalled(true)
                }
                return@newInstance Any()
            }.show(childFragmentManager, IndeterminateProgressDialogFragment.TAG)
        }
}
