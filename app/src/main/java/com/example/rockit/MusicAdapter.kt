package com.example.rockit

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.rockit.databinding.ActivityMusicBinding
import com.squareup.picasso.Picasso

class MusicAdapter(val songs: MutableList<Songs> , private val listener : OnItemClick) : RecyclerView.Adapter<MusicAdapter.ViewHolder>()
{

    inner class ViewHolder(val binding: ActivityMusicBinding) : RecyclerView.ViewHolder(binding.root),View.OnClickListener , View.OnLongClickListener {

        init {
            binding.root.setOnClickListener(this)
            binding.root.setOnLongClickListener(this)
        }

        override fun onClick(view: View?) {
            listener.onclick(adapterPosition,songs,true)
        }

        override fun onLongClick(p0: View?): Boolean
        {
            listener.onlongclick(adapterPosition,songs,true)
            return true
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ActivityMusicBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, pos: Int)
    {
        val song = songs[pos]
        holder.binding.name.text = song.name
        holder.binding.artist.text = song.primaryArtists
        Picasso.get().load(song.image[1].link).placeholder(R.drawable.loading).into(holder.binding.songimage)
        holder.binding.name.isSelected = true
        holder.binding.artist.isSelected = true
    }

    override fun getItemCount() = songs.size

    interface OnItemClick
    {
        fun onclick(pos : Int , songs: MutableList<Songs> , isclicked : Boolean)
        fun onlongclick(pos : Int , songs: MutableList<Songs> , isclicked : Boolean)
    }

}