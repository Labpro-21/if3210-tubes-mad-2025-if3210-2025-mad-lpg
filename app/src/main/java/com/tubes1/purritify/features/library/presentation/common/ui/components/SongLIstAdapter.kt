package com.tubes1.purritify.features.library.presentation.common.ui.components

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.tubes1.purritify.R
import com.tubes1.purritify.core.data.model.Song

class SongListAdapter(
    private val onItemClick: (Song) -> Unit
) : RecyclerView.Adapter<SongListAdapter.SongViewHolder>() {

    private val differ = AsyncListDiffer(this, SongDiffCallback())

    fun submitList(list: List<Song>) {
        differ.submitList(list)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SongViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.song_list_item_layout, parent, false)
        return SongViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: SongViewHolder, position: Int) {
        val currentSong = differ.currentList[position]
        holder.bind(currentSong, onItemClick)
    }

    override fun getItemCount(): Int {
        return differ.currentList.size
    }

    class SongViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val titleTextView: TextView = itemView.findViewById(R.id.songTitleTextView)
        private val artistTextView: TextView = itemView.findViewById(R.id.songArtistTextView)
        private val albumArtImageView: ImageView = itemView.findViewById(R.id.albumArtImageView)

        fun bind(song: Song, onItemClick: (Song) -> Unit) {
            titleTextView.text = song.title ?: "Unknown Title"
            artistTextView.text = song.artist ?: "Unknown Artist"
            Glide.with(itemView.context)
                .load(song.songArtUri ?: R.drawable.dummy_song_art)
                .placeholder(R.drawable.dummy_song_art)
                .into(albumArtImageView)

            itemView.setOnClickListener { onItemClick(song) }
        }
    }
}

class SongDiffCallback : DiffUtil.ItemCallback<Song>() {
    override fun areItemsTheSame(oldItem: Song, newItem: Song): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: Song, newItem: Song): Boolean {
        return oldItem == newItem
    }
}