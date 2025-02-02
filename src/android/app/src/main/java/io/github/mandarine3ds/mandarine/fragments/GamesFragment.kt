// Copyright 2025 Citra Project / Mandarine Project
// Licensed under GPLv2 or any later version
// Refer to the license.txt file included.

package io.github.mandarine3ds.mandarine.fragments

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import android.view.inputmethod.InputMethodManager
import android.widget.PopupMenu
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.color.MaterialColors
import com.google.android.material.transition.MaterialFadeThrough
import info.debatty.java.stringsimilarity.Jaccard
import info.debatty.java.stringsimilarity.JaroWinkler
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import io.github.mandarine3ds.mandarine.MandarineApplication
import io.github.mandarine3ds.mandarine.R
import io.github.mandarine3ds.mandarine.adapters.GameAdapter
import io.github.mandarine3ds.mandarine.databinding.FragmentGamesBinding
import io.github.mandarine3ds.mandarine.features.settings.model.Settings
import io.github.mandarine3ds.mandarine.model.Game
import io.github.mandarine3ds.mandarine.utils.SearchLocationHelper
import io.github.mandarine3ds.mandarine.utils.SearchLocationResult
import io.github.mandarine3ds.mandarine.viewmodel.GamesViewModel
import io.github.mandarine3ds.mandarine.viewmodel.HomeViewModel
import java.time.temporal.ChronoField
import java.util.*

class GamesFragment : Fragment() {
    private var _binding: FragmentGamesBinding? = null
    private val binding get() = _binding!!

    companion object {
        private const val SEARCH_TEXT = "SearchText"
        private const val PREF_VIEW_TYPE = "GamesViewType"
        private const val PREF_SORT_TYPE = "GamesSortType"
    }

    private val gamesViewModel: GamesViewModel by activityViewModels()
    private val homeViewModel: HomeViewModel by activityViewModels()

    private val openImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        adapter.handleImageResult(uri)
    }

    private lateinit var preferences: SharedPreferences

    private lateinit var adapter: GameAdapter

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
                    gamesViewModel.reloadGames(false)
                }
            }
        }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enterTransition = MaterialFadeThrough()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGamesBinding.inflate(inflater)
        return binding.root
    }

    // This is using the correct scope, lint is just acting up
    @SuppressLint("UnsafeRepeatOnLifecycleDetector")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        homeViewModel.setStatusBarShadeVisibility(visible = true)

        preferences = PreferenceManager.getDefaultSharedPreferences(MandarineApplication.appContext)

        val inflater = LayoutInflater.from(requireContext())

        adapter = GameAdapter(
            requireActivity() as AppCompatActivity,
            inflater,
            openImageLauncher
        )

        binding.gridGames.apply {
            val gameAdapter = GameAdapter(requireActivity() as AppCompatActivity, inflater, openImageLauncher)
            val savedViewType = preferences.getInt(PREF_VIEW_TYPE, GameAdapter.VIEW_TYPE_LIST)
            gameAdapter.setViewType(savedViewType)
            currentFilter = preferences.getInt(PREF_SORT_TYPE, View.NO_ID)
            adapter = gameAdapter

            fun updateLayoutManager() {
                val columnResId = if (gameAdapter.getViewType() == GameAdapter.VIEW_TYPE_GRID) {
                    R.integer.game_grid_columns_big
                } else {
                    R.integer.game_grid_columns
                }
                layoutManager = GridLayoutManager(requireContext(), resources.getInteger(columnResId))
            }

            updateLayoutManager()
            // Check for layout changes
            gameAdapter.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
                override fun onChanged() {
                    updateLayoutManager()
                }
            })
        }

        binding.swipeRefresh.apply {
            // Add swipe down to refresh gesture
            setOnRefreshListener {
                gamesViewModel.reloadGames(false)
            }

            // Set theme color to the refresh animation's background
            setProgressBackgroundColorSchemeColor(
                MaterialColors.getColor(
                    binding.swipeRefresh,
                    com.google.android.material.R.attr.colorPrimary
                )
            )
            setColorSchemeColors(
                MaterialColors.getColor(
                    binding.swipeRefresh,
                    com.google.android.material.R.attr.colorOnPrimary
                )
            )

            setProgressViewOffset(
                false,
                0,
                resources.getDimensionPixelSize(R.dimen.spacing_refresh_end)
            )

        }

        if (savedInstanceState != null) {
            binding.searchText.setText(savedInstanceState.getString(SEARCH_TEXT))
        }

        viewLifecycleOwner.lifecycleScope.apply {
            launch {
                repeatOnLifecycle(Lifecycle.State.RESUMED) {
                    gamesViewModel.isReloading.collect { isReloading ->
                        binding.swipeRefresh.isRefreshing = isReloading
                        if (gamesViewModel.games.value.isEmpty() && !isReloading) {
                            binding.noticeText.visibility = View.VISIBLE
                        } else {
                            binding.noticeText.visibility = View.INVISIBLE
                        }
                    }
                }
            }
            launch {
                repeatOnLifecycle(Lifecycle.State.RESUMED) {
                    gamesViewModel.games.collectLatest { setAdapter(it) }
                }
            }
            launch {
                repeatOnLifecycle(Lifecycle.State.RESUMED) {
                    gamesViewModel.shouldSwapData.collect {
                        if (it) {
                            setAdapter(gamesViewModel.games.value)
                            gamesViewModel.setShouldSwapData(false)
                        }
                    }
                }
            }
            launch {
                repeatOnLifecycle(Lifecycle.State.RESUMED) {
                    gamesViewModel.shouldScrollToTop.collect {
                        if (it) {
                            scrollToTop()
                            gamesViewModel.setShouldScrollToTop(false)
                        }
                    }
                }
            }
        }

        setInsets()
        setupTopView()

        binding.addDirectory.setOnClickListener {
            documentPicker.launch(null)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (_binding != null) {
            outState.putString(SEARCH_TEXT, binding.searchText.text.toString())
        }
    }

    private fun setAdapter(games: List<Game>) {
        val preferences = PreferenceManager.getDefaultSharedPreferences(MandarineApplication.appContext)
        val currentSearchText = binding.searchText.text.toString()
        val currentFilter = binding.filterButton.id

        val baseList = if (preferences.getBoolean(Settings.PREF_SHOW_HOME_APPS, false)) {
            games
        } else {
            games.filter { !it.isSystemTitle }
        }

        if (currentSearchText.isNotEmpty() || currentFilter != View.NO_ID) {
            filterAndSearch(baseList)
        } else {
            (binding.gridGames.adapter as GameAdapter).submitList(baseList)
            gamesViewModel.setFilteredGames(baseList)
        }
    }

    private fun scrollToTop() {
        if (_binding != null) {
            binding.gridGames.smoothScrollToPosition(0)
        }
    }

    private fun setInsets() =
        ViewCompat.setOnApplyWindowInsetsListener(
            binding.root
        ) { view: View, windowInsets: WindowInsetsCompat ->
            val barInsets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            val cutoutInsets = windowInsets.getInsets(WindowInsetsCompat.Type.displayCutout())
            val extraListSpacing = resources.getDimensionPixelSize(R.dimen.spacing_large)
            val spacingNavigation = resources.getDimensionPixelSize(R.dimen.spacing_navigation)
            val spacingNavigationRail =
                resources.getDimensionPixelSize(R.dimen.spacing_navigation_rail)
            val chipSpacing = resources.getDimensionPixelSize(R.dimen.spacing_chip)
            val isLandscape = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

            binding.swipeRefresh.setProgressViewEndTarget(
                false,
                barInsets.top + resources.getDimensionPixelSize(R.dimen.spacing_refresh_end)
            )

            val leftInsets = barInsets.left + cutoutInsets.left
            val rightInsets = barInsets.right + cutoutInsets.right
            val mlpSwipe = binding.swipeRefresh.layoutParams as MarginLayoutParams
            if (ViewCompat.getLayoutDirection(view) == ViewCompat.LAYOUT_DIRECTION_LTR) {
                mlpSwipe.leftMargin = leftInsets
                mlpSwipe.rightMargin = rightInsets
            } else {
                mlpSwipe.leftMargin = leftInsets
                mlpSwipe.rightMargin = rightInsets
            }
            binding.swipeRefresh.layoutParams = mlpSwipe

            binding.noticeText.updatePadding(bottom = spacingNavigation)
            if (ViewCompat.getLayoutDirection(view) == ViewCompat.LAYOUT_DIRECTION_LTR) {
                binding.header.updatePadding(top = cutoutInsets.top + if (isLandscape) barInsets.top else 0)
            } else {
                binding.header.updatePadding(top = cutoutInsets.top + if (isLandscape) barInsets.top else 0)
            }

            val mlpFab = binding.addDirectory.layoutParams as MarginLayoutParams
            val fabPadding = resources.getDimensionPixelSize(R.dimen.spacing_large)
            mlpFab.leftMargin = leftInsets + fabPadding
            mlpFab.bottomMargin = barInsets.bottom + fabPadding
            mlpFab.rightMargin = rightInsets + fabPadding
            binding.addDirectory.layoutParams = mlpFab

            windowInsets
        }

    private fun setupTopView() {
        binding.searchText.doOnTextChanged { text: CharSequence?, _: Int, _: Int, _: Int ->
            if (text.toString().isNotEmpty()) {
                binding.clearButton.visibility = View.VISIBLE
            } else {
                binding.clearButton.visibility = View.INVISIBLE
            }
            filterAndSearch()
        }

        binding.clearButton.setOnClickListener { binding.searchText.setText("") }
        binding.searchBackground.setOnClickListener { focusSearch() }

        // Setup filter button
        binding.filterButton.setOnClickListener { showFilterMenu(it) }

        // Setup view button
        binding.viewButton.setOnClickListener { showViewMenu(it) }

        // Setup settings button
        binding.settingsButton.setOnClickListener { navigateToSettings() }
    }

    private fun navigateToSettings() {
        val navController = findNavController()
        navController.navigate(R.id.action_gamesFragment_to_homeSettingsFragment)}

    private fun showFilterMenu(anchor: View) {
        val popup = PopupMenu(requireContext(), anchor)
        popup.menuInflater.inflate(R.menu.menu_game_filters, popup.menu)

        // Set checked state based on current filter
        when (currentFilter) {
            R.id.alphabetical -> popup.menu.findItem(R.id.alphabetical).isChecked = true
            R.id.filter_recently_played -> popup.menu.findItem(R.id.filter_recently_played).isChecked = true
            R.id.filter_recently_added -> popup.menu.findItem(R.id.filter_recently_added).isChecked = true
            R.id.filter_installed -> popup.menu.findItem(R.id.filter_installed).isChecked = true
        }

        popup.setOnMenuItemClickListener { item ->
            currentFilter = item.itemId
            preferences.edit().putInt(PREF_SORT_TYPE, currentFilter).apply()
            filterAndSearch()
            true
        }

        popup.show()
    }

    private fun showViewMenu(anchor: View) {
        val popup = PopupMenu(requireContext(), anchor)
        popup.menuInflater.inflate(R.menu.menu_game_views, popup.menu)

        val currentViewType = (binding.gridGames.adapter as GameAdapter).getViewType()
        when (currentViewType) {
            GameAdapter.VIEW_TYPE_LIST -> popup.menu.findItem(R.id.view_list).isChecked = true
            GameAdapter.VIEW_TYPE_GRID -> popup.menu.findItem(R.id.view_grid).isChecked = true
        }

        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.view_list -> {
                    (binding.gridGames.adapter as GameAdapter).setViewType(GameAdapter.VIEW_TYPE_LIST)
                    preferences.edit().putInt(PREF_VIEW_TYPE, GameAdapter.VIEW_TYPE_LIST).apply()
                    item.isChecked = true
                    true
                }
                R.id.view_grid -> {
                    (binding.gridGames.adapter as GameAdapter).setViewType(GameAdapter.VIEW_TYPE_GRID)
                    preferences.edit().putInt(PREF_VIEW_TYPE, GameAdapter.VIEW_TYPE_GRID).apply()
                    item.isChecked = true
                    true
                }
                else -> false
            }
        }

        popup.show()
    }

    // Track current filter
    private var currentFilter = View.NO_ID

    private fun filterAndSearch(baseList: List<Game> = gamesViewModel.games.value) {
        val filteredList: List<Game> = when (currentFilter) {
            R.id.alphabetical -> baseList.sortedBy { it.title }
            R.id.filter_recently_played -> {
                baseList.filter {
                    val lastPlayedTime = preferences.getLong(it.keyLastPlayedTime, 0L)
                    lastPlayedTime > (System.currentTimeMillis() - ChronoField.MILLI_OF_DAY.range().maximum)
                }
            }
            R.id.filter_recently_added -> {
                baseList.filter {
                    val addedTime = preferences.getLong(it.keyAddedToLibraryTime, 0L)
                    addedTime > (System.currentTimeMillis() - ChronoField.MILLI_OF_DAY.range().maximum)
                }
            }
            R.id.filter_installed -> baseList.filter { it.isInstalled }
            else -> baseList
        }

        val searchTerm = binding.searchText.text.toString().lowercase(Locale.getDefault())
        if (searchTerm.isEmpty()) {
            (binding.gridGames.adapter as GameAdapter).submitList(filteredList)
            gamesViewModel.setFilteredGames(filteredList)
            return
        }

        val searchAlgorithm = if (searchTerm.length > 1) Jaccard(2) else JaroWinkler()
        val sortedList = filteredList.mapNotNull { game ->
            val title = game.title.lowercase(Locale.getDefault())
            val score = searchAlgorithm.similarity(searchTerm, title)
            if (score > 0.03) {
                ScoredGame(score, game)
            } else {
                null
            }
        }.sortedByDescending { it.score }.map { it.item }

        (binding.gridGames.adapter as GameAdapter).submitList(sortedList)
        gamesViewModel.setFilteredGames(sortedList)
    }

    private inner class ScoredGame(val score: Double, val item: Game)

    private fun focusSearch() {
        binding.searchText.requestFocus()
        val imm = requireActivity()
            .getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager?
        imm?.showSoftInput(binding.searchText, InputMethodManager.SHOW_IMPLICIT)
    }
}
