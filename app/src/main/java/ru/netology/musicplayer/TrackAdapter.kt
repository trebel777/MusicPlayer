package ru.netology.musicplayer

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView

class TrackAdapter(private val tracks: List<Track>) : RecyclerView.Adapter<TrackAdapter.TrackViewHolder>() {

    private var onItemClickListener: OnItemClickListener? = null
    private var currentTrackIndex: Int = -1

    private var isPlaying: Boolean = false

    interface OnItemClickListener {
        fun onItemClick(position: Int)
    }

    fun setOnItemClickListener(listener: OnItemClickListener) {
        onItemClickListener = listener
    }

    fun setCurrentTrackIndex(index: Int) {
        currentTrackIndex = index
        notifyDataSetChanged()
    }

    fun setTrackActive(index: Int, isActive: Boolean) {
        // Если индекс активного трека уже совпадает с переданным индексом, нет необходимости обновлять список
        if (currentTrackIndex == index) return

        // Очищаем цвет фона предыдущего активного трека
        if (currentTrackIndex >= 0) {
            notifyItemChanged(currentTrackIndex)
        }

        // Устанавливаем новый активный трек
        currentTrackIndex = index

        // Обновляем цвет фона нового активного трека
        if (currentTrackIndex >= 0) {
            notifyItemChanged(currentTrackIndex)
        }
    }


    fun setPlayingStatus(playing: Boolean) {
        isPlaying = playing
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TrackViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.item_track, parent, false)

        return TrackViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: TrackViewHolder, position: Int) {
        val track = tracks[position]
        holder.bind(track)

        if (position == currentTrackIndex) {
            holder.itemView.setBackgroundColor(ContextCompat.getColor(holder.itemView.context, R.color.lightGreen))
        } else {
            holder.itemView.setBackgroundColor(ContextCompat.getColor(holder.itemView.context, R.color.white))
        }

        holder.itemView.setOnClickListener {
            onItemClickListener?.onItemClick(position)
        }
    }


    override fun getItemCount(): Int {
        return tracks.size
    }

    fun getItem(position: Int): Track {
        return tracks[position]
    }

    inner class TrackViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val trackTitleTextView: TextView = itemView.findViewById(R.id.trackTitleTextView)
        private val trackFileTextView: TextView = itemView.findViewById(R.id.trackFileTextView)

        fun bind(track: Track) {
            trackTitleTextView.text = track.title
            trackFileTextView.text = track.file
        }
    }
}

