
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
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.viewbinding.ViewBinding
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.transition.MaterialSharedAxis
import dagger.hilt.android.AndroidEntryPoint
import io.github.mandarine3ds.mandarine.R
import io.github.mandarine3ds.mandarine.adapters.CustomListItem
import io.github.mandarine3ds.mandarine.adapters.SearchLocationViewItem
import io.github.mandarine3ds.mandarine.adapters.SelectableAdapter
import io.github.mandarine3ds.mandarine.adapters.SpacingItemDecoration
import io.github.mandarine3ds.mandarine.databinding.FragmentSearchLocationBinding
import io.github.mandarine3ds.mandarine.utils.SearchLocationHelper
import io.github.mandarine3ds.mandarine.utils.SearchLocationResult
import io.github.mandarine3ds.mandarine.utils.serializable

/**
 * This fragment is used to manage the selected search locations to use.
 */
@AndroidEntryPoint
class SearchLocationFragment : Fragment() {
    private var _binding: FragmentSearchLocationBinding? = null
    private val binding get() = _binding!!

    private val adapter = SelectableAdapter(0)

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

        adapter.selectAndNotify(items)
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

            val mlpScrollAbout = binding.locationsList.layoutParams as MarginLayoutParams
            mlpScrollAbout.leftMargin = leftInsets
            mlpScrollAbout.rightMargin = rightInsets
            binding.locationsList.layoutParams = mlpScrollAbout

            binding.root.updatePadding(bottom = barInsets.bottom)

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
