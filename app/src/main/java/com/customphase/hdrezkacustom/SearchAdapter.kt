package com.customphase.hdrezkacustom

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class SearchAdapter(private val onItemClick: (SearchResult) -> Unit) :
    RecyclerView.Adapter<SearchAdapter.SearchViewHolder>() {

    private var items = listOf<SearchResult>()

    fun submitList(newList: List<SearchResult>) {
        items = newList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SearchViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.search_result_item, parent, false)
        return SearchViewHolder(view, onItemClick)
    }

    override fun onBindViewHolder(holder: SearchViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount() = items.size

    class SearchViewHolder(itemView: View, private val onItemClick: (SearchResult) -> Unit) :
        RecyclerView.ViewHolder(itemView) {

        private val image: ImageView = itemView.findViewById(R.id.searchResultItemImage)
        private val title: TextView = itemView.findViewById(R.id.searchResultItemTitle)
        //private val description: TextView = itemView.findViewById(R.id.descriptionTextView)
        private val info: TextView = itemView.findViewById(R.id.searchResultItemInfo)

        fun bind(result: SearchResult) {
            title.text = result.title
            info.text = result.info

            if (result.imageUrl != null) {
                loadImageFromUrlIntoView(image, result.imageUrl)
            }

            itemView.setOnClickListener {
                onItemClick(result)
            }
        }
    }
}