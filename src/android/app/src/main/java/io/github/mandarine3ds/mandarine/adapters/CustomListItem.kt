
package io.github.mandarine3ds.mandarine.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding

/**
 * A generic ViewHolder to handle different types of ViewBinding.
 */
class CustomViewHolder<out V : ViewBinding>(val binding: V) : RecyclerView.ViewHolder(binding.root)

/**
 * Extension function to get the LayoutInflater from a View.
 */
fun View.getInflater() = LayoutInflater.from(context)!!

/**
 * Interface for creating ViewBinding instances.
 */
interface BindingFactory {
    fun createBinding(parent: ViewGroup): ViewBinding
}

/**
 * Abstract base class for list items in a RecyclerView.
 * Each item must define a BindingFactory and binding logic.
 */
abstract class CustomListItem<V : ViewBinding> {
    var adapter: MultiTypeAdapter? = null

    /**
     * Provides the factory for creating the ViewBinding associated with this item.
     */
    abstract fun getBindingFactory(): BindingFactory

    /**
     * Binds data to the provided ViewHolder.
     */
    abstract fun bind(holder: CustomViewHolder<V>, position: Int)

    /**
     * Returns a key for filtering purposes. Override to provide custom keys.
     */
    open fun getFilterKey(): String = ""

    /**
     * Determines whether two items are the same. Override for custom logic.
     */
    open fun isSameItem(other: CustomListItem<V>): Boolean = this == other

    /**
     * Determines whether the content of two items is the same. Override for custom logic.
     */
    open fun isSameContent(other: CustomListItem<V>): Boolean = this == other

    /**
     * Indicates whether the item should span the full width. Default is false.
     */
    open val isFullSpan: Boolean = false
}

/**
 * Abstract base class for list items in a selectable adapter.
 */
abstract class SelectableCustomListItem<V : ViewBinding> : CustomListItem<V>() {
    val selectableAdapter get() = super.adapter as? SelectableAdapter
}
