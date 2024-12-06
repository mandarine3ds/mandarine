
package io.github.mandarine3ds.mandarine.adapters

import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import java.util.*
import info.debatty.java.stringsimilarity.Cosine
import info.debatty.java.stringsimilarity.JaroWinkler

typealias OnFilterCompleteListener = () -> Unit

/**
 * Adapter supporting multiple view types, utilizing items implementing [CustomListItem].
 * View bindings are determined by [CustomListItem.getBindingFactory].
 */
open class MultiTypeAdapter : RecyclerView.Adapter<CustomViewHolder<ViewBinding>>(), Filterable {

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<CustomListItem<ViewBinding>>() {
            override fun areItemsTheSame(oldItem: CustomListItem<ViewBinding>, newItem: CustomListItem<ViewBinding>) =
                oldItem.isSameItem(newItem)

            override fun areContentsTheSame(oldItem: CustomListItem<ViewBinding>, newItem: CustomListItem<ViewBinding>) =
                oldItem.isSameContent(newItem)
        }
    }

    private val listDiffer = AsyncListDiffer(this, DIFF_CALLBACK)
    private val completeItemList = mutableListOf<CustomListItem<out ViewBinding>>()
    val filteredItemList: List<CustomListItem<in ViewBinding>> get() = listDiffer.currentList

    private val bindingTypeMap = mutableMapOf<BindingFactory, Int>()
    private var onFilterCompleteListener: OnFilterCompleteListener? = null

    var searchQuery: String = ""

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CustomViewHolder<ViewBinding> {
        val bindingFactory = bindingTypeMap.filterValues { it == viewType }.keys.single()
        return CustomViewHolder(bindingFactory.createBinding(parent))
    }

    override fun onBindViewHolder(holder: CustomViewHolder<ViewBinding>, position: Int) {
        filteredItemList[position].apply {
            adapter = this@MultiTypeAdapter
            bind(holder, position)
        }
    }

    override fun getItemCount() = filteredItemList.size

    fun registerBindingFactory(factory: BindingFactory): Int =
        bindingTypeMap.getOrPut(factory) { bindingTypeMap.size }

    override fun getItemViewType(position: Int) =
        bindingTypeMap.getOrPut(filteredItemList[position].getBindingFactory()) { bindingTypeMap.size }

    fun updateItems(items: List<CustomListItem<*>>) {
        completeItemList.clear()
        completeItemList.addAll(items)
        filter.filter(searchQuery)
    }

    open fun removeItemAt(position: Int) {
        completeItemList.removeAt(position)
        filter.filter(searchQuery)
    }

    open fun addItemAt(position: Int, item: CustomListItem<out ViewBinding>) {
        completeItemList.add(position, item)
        filter.filter(searchQuery)
    }

    fun setOnFilterCompleteListener(listener: OnFilterCompleteListener) {
        onFilterCompleteListener = listener
    }

    override fun getFilter() = object : Filter() {
        private val jaroWinkler = JaroWinkler()
        private val cosine = Cosine()

        inner class ItemScore(val score: Double, val item: CustomListItem<*>)

        private fun getSortedMatches(): List<ItemScore> {
            return completeItemList.mapNotNull { item ->
                val itemKey = item.key().lowercase(Locale.getDefault())
                val similarity = (jaroWinkler.similarity(searchQuery, itemKey) + cosine.similarity(searchQuery, itemKey)) / 2
                if (similarity > 0) ItemScore(similarity, item) else null
            }.sortedByDescending { it.score }
        }

        override fun performFiltering(query: CharSequence): FilterResults {
            val lowerCaseQuery = query.toString().lowercase(Locale.getDefault())
            searchQuery = lowerCaseQuery

            val results = if (lowerCaseQuery.isEmpty()) {
                completeItemList
            } else {
                val matches = getSortedMatches()
                val averageScore = matches.sumOf { it.score } / matches.size
                matches.filter { it.score >= averageScore }.map { it.item }
            }

            return FilterResults().apply {
                values = results
                count = results.size
            }
        }

        override fun publishResults(constraint: CharSequence, results: FilterResults) {
            @Suppress("UNCHECKED_CAST")
            listDiffer.submitList(results.values as List<CustomListItem<ViewBinding>>)
            onFilterCompleteListener?.invoke()
        }
    }
}

/**
 * An adapter extension that allows selecting an item with automatic notifications.
 */
class SelectableAdapter(private val initialPosition: Int) : MultiTypeAdapter() {
    var selectedPosition: Int = initialPosition

    fun selectAndNotify(position: Int) {
        val previousSelection = selectedPosition
        selectedPosition = position
        notifyItemChanged(previousSelection)
        notifyItemChanged(position)
    }

    override fun removeItemAt(position: Int) {
        super.removeItemAt(position)
        when {
            position < selectedPosition -> selectedPosition--
            position == selectedPosition -> selectAndNotify(initialPosition)
        }
    }

    override fun addItemAt(position: Int, item: CustomListItem<out ViewBinding>) {
        super.addItemAt(position, item)
        if (position <= selectedPosition) selectedPosition++
    }
}
