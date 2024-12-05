
package io.github.mandarine3ds.mandarine.fragments

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.WindowCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import io.github.mandarine3ds.mandarine.R
import io.github.mandarine3ds.mandarine.adapters.GenericListItem
import io.github.mandarine3ds.mandarine.adapters.SearchLocationViewItem
import io.github.mandarine3ds.mandarine.adapters.SelectableGenericAdapter
import io.github.mandarine3ds.mandarine.adapters.SpacingItemDecoration
import io.github.mandarine3ds.mandarine.databinding.FragmentSearchLocationBinding
import io.github.mandarine3ds.mandarine.utils.SearchLocationHelper
import io.github.mandarine3ds.mandarine.utils.SearchLocationResult
import io.github.mandarine3ds.mandarine.utils.WindowInsetsHelper
import io.github.mandarine3ds.mandarine.utils.serializable

/**
 * This fragment is used to manage the selected search locations to use.
 */
@AndroidEntryPoint
class SearchLocationFragment : Fragment() {
    private var _binding: FragmentSearchLocationBinding? = null
    private val binding get() = _binding!!

    private val adapter = SelectableGenericAdapter(0)

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
                if (result == SearchLocationResult.Success) populateAdapter()
            }
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

        WindowInsetsHelper.applyToActivity(binding.root, binding.locationsList)
        WindowInsetsHelper.addMargin(binding.addLocationButton, bottom = true)

        binding.titlebar.toolbar.title = getString(R.string.search_location)
        binding.titlebar.toolbar.subtitle = "" // nothing set for now

        val layoutManager = LinearLayoutManager(requireContext())
        binding.locationsList.layoutManager = layoutManager
        binding.locationsList.adapter = adapter

        binding.locationsList.addItemDecoration(
            SpacingItemDecoration(resources.getDimensionPixelSize(R.dimen.grid_padding))
        )

        binding.addLocationButton.setOnClickListener {
            documentPicker.launch(null)
        }

        binding.coordinatorLayout.viewTreeObserver.addOnTouchModeChangeListener { isTouchMode ->
            val layoutUpdate = {
                val params = binding.locationsList.layoutParams as CoordinatorLayout.LayoutParams
                if (!isTouchMode) {
                    binding.titlebar.appBarLayout.setExpanded(true)
                    params.height = binding.coordinatorLayout.height - binding.titlebar.toolbar.height
                } else {
                    params.height = CoordinatorLayout.LayoutParams.MATCH_PARENT
                }
                binding.locationsList.layoutParams = params
                binding.locationsList.requestLayout()
            }

            binding.coordinatorLayout.viewTreeObserver.addOnGlobalLayoutListener {
                layoutUpdate()
            }
        }

        populateAdapter()
    }

    private fun populateAdapter() {
        val items: MutableList<GenericListItem<out ViewBinding>> = ArrayList()

        SearchLocationHelper.getSearchLocations(requireContext()).onEach { uri ->
            items.add(SearchLocationViewItem(uri).apply {
                onDelete = { position ->
                    Snackbar.make(
                        binding.root,
                        getString(R.string.search_location_delete_success),
                        Snackbar.LENGTH_LONG
                    ).setAction(R.string.undo) {
                        adapter.run { addItemAt(position, this@apply) }
                    }.addCallback(object : Snackbar.Callback() {
                        override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
                            if (event != DISMISS_EVENT_ACTION) {
                                SearchLocationHelper.deleteLocation(requireContext(), uri!!)
                            }
                        }
                    }).show()
                }
            })
        }

        adapter.setItems(items)
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
