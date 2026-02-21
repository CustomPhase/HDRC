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

        private val title: TextView = itemView.findViewById(R.id.titleTextView)
        //private val description: TextView = itemView.findViewById(R.id.descriptionTextView)
        private val poster: ImageView = itemView.findViewById(R.id.posterImageView)

        init {
            itemView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    // здесь надо передать объект, но мы используем колбэк, поэтому передадим в bind
                }
            }
        }

        fun bind(result: SearchResult) {
            title.text = result.title
            //description.text = result.description ?: "Описание отсутствует"

            // Загрузка постера (пока без Glide, можно просто показать заглушку)
            if (result.posterUrl != null) {
                // Здесь можно использовать Glide или Picasso, но для простоты оставим пустым
                // Например:
                // Glide.with(itemView.context).load(result.posterUrl).into(poster)
            } else {
                poster.setImageResource(android.R.drawable.ic_menu_report_image)
            }

            itemView.setOnClickListener {
                onItemClick(result)
            }
        }
    }
}