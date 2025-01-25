
package io.github.mandarine3ds.mandarine.fragments

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewTreeObserver
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import androidx.activity.result.contract.ActivityResultContracts
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.WindowCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.findNavController
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.viewbinding.ViewBinding
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.transition.MaterialSharedAxis
import io.github.mandarine3ds.mandarine.R
import io.github.mandarine3ds.mandarine.adapters.CustomListItem
import io.github.mandarine3ds.mandarine.adapters.SearchLocationViewItem
import io.github.mandarine3ds.mandarine.adapters.SelectableAdapter
import io.github.mandarine3ds.mandarine.adapters.SpacingItemDecoration
import io.github.mandarine3ds.mandarine.databinding.FragmentSearchLocationBinding
import io.github.mandarine3ds.mandarine.utils.SearchLocationHelper
import io.github.mandarine3ds.mandarine.utils.SearchLocationResult
import io.github.mandarine3ds.mandarine.viewmodel.HomeViewModel
import io.github.mandarine3ds.mandarine.viewmodel.GamesViewModel

/**
 * This fragment is used to manage the selected search locations to use.
 */
class SearchLocationFragment : Fragment() {
    private var _binding: FragmentSearchLocationBinding? = null
    private val binding get() = _binding!!

    private val adapter = SelectableAdapter(0)
    private val homeViewModel: HomeViewModel by activityViewModels()
    private val gamesViewModel: GamesViewModel by activityViewModels()

    private val documentPicker =
        registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
            uri?.let {
                Log.i("Uri selected", it.toString())
                requireContext().contentResolver.takePersistableUriPermission(
                    it,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
                val result = SearchLocationHelper.addLocation(requireContext(), it)
                Snackbar.make(binding.root, resolveActionResultString(result), Snackbar.LENGTH_LONG)
                    .show()
                if (result == SearchLocationResult.Success) {
                    populateAdapter()
                    gamesViewModel.reloadGames(false)
                }
            }
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
        _binding = FragmentSearchLocationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.toolbar.setTitle(getString(R.string.search_location))
        
        binding.toolbar.setNavigationOnClickListener {
            binding.root.findNavController().popBackStack()
        }
        
        homeViewModel.setNavigationVisibility(visible = false, animated = true)
        homeViewModel.setStatusBarShadeVisibility(visible = false)

        val layoutManager = LinearLayoutManager(requireContext())
        binding.locationsList.layoutManager = layoutManager
        binding.locationsList.adapter = adapter

        binding.locationsList.addItemDecoration(
            SpacingItemDecoration(resources.getDimensionPixelSize(R.dimen.spacing_med))
        )

        binding.addLocationButton.setOnClickListener {
            documentPicker.launch(null)
        }

        populateAdapter()
        setInsets()
    }

    private fun populateAdapter() {
        val items: MutableList<CustomListItem<out ViewBinding>> = ArrayList()

        SearchLocationHelper.getSearchLocations(requireContext()).onEach { uri ->
            items.add(SearchLocationViewItem(uri).apply {
                onDelete = { position ->
                    Snackbar.make(
                        binding.root,
                        getString(R.string.search_location_delete_success),
                        Snackbar.LENGTH_LONG
                    ).setAction(R.string.undo) {
                        adapter?.run { addItemAt(position, this@apply) }
                        populateAdapter()
                    }.addCallback(object : Snackbar.Callback() {
                        override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
                            if (event != DISMISS_EVENT_ACTION) {
                                SearchLocationHelper.deleteLocation(requireContext(), uri!!)
                                populateAdapter()
                                gamesViewModel.reloadGames(false)
                            }
                        }
                    }).show()
                }
            })
        }

        adapter.updateItems(items)
    }

    private fun setInsets() =
        ViewCompat.setOnApplyWindowInsetsListener(
            binding.root
        ) { _: View, windowInsets: WindowInsetsCompat ->
            val barInsets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            val cutoutInsets = windowInsets.getInsets(WindowInsetsCompat.Type.displayCutout())

            val leftInsets = barInsets.left + cutoutInsets.left
            val rightInsets = barInsets.right + cutoutInsets.right

            val mlpAppBar = binding.toolbar.layoutParams as MarginLayoutParams
            mlpAppBar.leftMargin = leftInsets
            mlpAppBar.rightMargin = rightInsets
            binding.toolbar.layoutParams = mlpAppBar

            binding.locationsList.updatePadding(
                left = leftInsets,
                right = rightInsets,
                bottom = barInsets.bottom +
                    requireActivity().resources.getDimensionPixelSize(R.dimen.spacing_fab_list)
            )
            
            val mlpFab = binding.addLocationButton.layoutParams as MarginLayoutParams
            val fabPadding = requireActivity().resources.getDimensionPixelSize(R.dimen.spacing_large)
            mlpFab.leftMargin = leftInsets + fabPadding
            mlpFab.bottomMargin = barInsets.bottom + fabPadding
            mlpFab.rightMargin = rightInsets + fabPadding
            binding.addLocationButton.layoutParams = mlpFab
            
            binding.locationsList.post {
                binding.locationsList.invalidate()
                binding.locationsList.requestLayout()
            }

            windowInsets
        }

    private fun resolveActionResultString(result: SearchLocationResult) = when (result) {
        SearchLocationResult.Success -> getString(R.string.search_location_add_success)
        SearchLocationResult.Deleted -> getString(R.string.search_location_delete_success)
        SearchLocationResult.AlreadyAdded -> getString(R.string.search_location_duplicated)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
