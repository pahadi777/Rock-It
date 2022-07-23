package com.example.rockit

import android.app.AlertDialog
import android.app.DownloadManager
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.rockit.databinding.ActivityMainBinding
import com.squareup.picasso.Picasso
import retrofit2.Call
import retrofit2.Callback
import java.util.concurrent.TimeUnit


class MainActivity : AppCompatActivity(),MusicAdapter.OnItemClick,PopupMenu.OnMenuItemClickListener {

    private lateinit var binding: ActivityMainBinding
    private var topsongs = mutableListOf<Songs>()
    private var searchsongs = mutableListOf<Songs>()
    private var currentplaylist = mutableListOf<Songs>()
    private lateinit var topsongsadapter : MusicAdapter
    private lateinit var searchadapter : MusicAdapter
    private val mediaPlayer = MediaPlayer()
    lateinit var runnable: Runnable
    var handler = Handler()
    private val layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL,false)
    private var position = 0
    private val NOT_AVAILABLE : Byte = -1
    private val PAUSED : Byte = 0
    private val PLAYING : Byte = 1
    private var state : Byte = -1
//    val lessweight = LinearLayout.LayoutParams( LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT, 3.5f )
//    val moreweight = LinearLayout.LayoutParams( LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT, 4f )
    private lateinit var popupmenu : PopupMenu
    private lateinit var sharedPreferences : SharedPreferences
    private var quality : Int = 4
    private lateinit var alertDialog : AlertDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.hide()

        sharedPreferences = getSharedPreferences("mydatabase", MODE_PRIVATE)

        if(!sharedPreferences.contains("quality")) sharedPreferences.edit().putInt("quality",4).apply()
        else quality = sharedPreferences.getInt("quality",4)

        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC)
        binding.song.isSelected = true
        binding.artists.isSelected = true

        startshimmer()

        topsongsadapter = MusicAdapter(topsongs,this)
        searchadapter = MusicAdapter(searchsongs,this)
        binding.recyclerview.adapter = topsongsadapter
        binding.recyclerview.layoutManager = layoutManager

        getsongs()

        binding.search.addTextChangedListener(object : TextWatcher{
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int){}

            override fun afterTextChanged(p0: Editable?){
                val text = p0.toString().trim()
                if(TextUtils.isEmpty(text))
                {
                    binding.recyclerview.adapter = topsongsadapter
                    binding.details.visibility = View.VISIBLE
                    binding.nosong.visibility = View.GONE
//                    binding.bottom.layoutParams = lessweight
                    binding.recyclerview.visibility = View.VISIBLE
                }
                else
                {
                    startshimmer()
                    searchsongs(text,50)
//                    binding.bottom.layoutParams = moreweight
                }
            }
        })

        binding.playpause.setOnClickListener {

            if(state == PLAYING)
            {
                state = PAUSED
                mediaPlayer.pause()
                binding.playpause.setImageResource(R.drawable.play)
            }
            else if(state == PAUSED)
            {
                state = PLAYING
                binding.seekBar.max = mediaPlayer.duration
                mediaPlayer.start()
                binding.playpause.setImageResource(R.drawable.pause)
            }

        }

        binding.seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener{
            override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean)
            {
                if(p2 && state!=NOT_AVAILABLE)
                {
                    mediaPlayer.seekTo(p1)
                    binding.playpause.setImageResource(R.drawable.ic_loadingsong)
                    setseekbar(p1)
                }
                else if(state==NOT_AVAILABLE) setseekbar(0)
            }

            override fun onStartTrackingTouch(p0: SeekBar?)
            {
            }

            override fun onStopTrackingTouch(p0: SeekBar?)
            {

            }
        })

        mediaPlayer.setOnSeekCompleteListener {
            if(state == PLAYING) binding.playpause.setImageResource(R.drawable.pause)
            else binding.playpause.setImageResource(R.drawable.play)
        }

        runnable = Runnable {
            if(state == PLAYING) setseekbar(mediaPlayer.currentPosition)
            val text = binding.search.text.toString().trim()
            if(TextUtils.isEmpty(text) && binding.shimmerlayout.visibility==View.GONE && binding.details.visibility==View.GONE)
            {
                binding.recyclerview.adapter = topsongsadapter
                binding.details.visibility = View.VISIBLE
                binding.nosong.visibility = View.GONE
//                binding.bottom.layoutParams = lessweight
                binding.recyclerview.visibility = View.VISIBLE
            }
            handler.postDelayed(runnable,100)
        }

        handler.postDelayed(runnable,100)

        mediaPlayer.setOnCompletionListener {
            binding.seekBar.max = 0
            binding.seekBar.progress = 0
            setseekbar(0)
            onclick(getNextPos(position,currentplaylist),currentplaylist,false)
        }

        binding.previous.setOnClickListener {
            if(state!=NOT_AVAILABLE) onclick(getPreviousPos(position, currentplaylist), currentplaylist,false)
        }

        binding.next.setOnClickListener {
            if(state!=NOT_AVAILABLE) onclick(getNextPos(position, currentplaylist), currentplaylist,false)
        }

    }

    private fun setseekbar(duration : Int) {
        binding.seekBar.progress = duration
        binding.current.text = getduration(duration.toLong())
    }

    private fun getduration(duration : Long): String
    {
        val minutes: Long = TimeUnit.MINUTES.convert(duration, TimeUnit.MILLISECONDS)
        val seconds: Long = (TimeUnit.SECONDS.convert(duration, TimeUnit.MILLISECONDS)
                - minutes * TimeUnit.SECONDS.convert(1, TimeUnit.MINUTES))
        return String.format("%02d:%02d", minutes, seconds)
    }

    private fun getsongs()
    {
        val result = SongService.SongInstance.getsongs()
        result.enqueue(object : Callback<Response> {
            override fun onResponse(call: Call<Response>, r : retrofit2.Response<Response>)
            {
                val response = r.body()
                if(response!=null)
                {
                    topsongs.addAll(response.results.songs)
                    topsongsadapter.notifyDataSetChanged()
                    stopshimmer()
                    binding.details.visibility = View.VISIBLE
                }
            }

            override fun onFailure(call: Call<Response>, t: Throwable)
            {
//                Toast.makeText(this@MainActivity,t.message, Toast.LENGTH_SHORT).show()
                stopshimmer()
                binding.details.visibility = View.VISIBLE
            }
        })
    }

    private fun searchsongs(query : String , limit : Int)
    {
        Log.d("calling","true")
        val result = SongService.SongInstance.searchsongs(query,limit)
        result.enqueue(object : Callback<Searchsongs> {
            override fun onResponse(call: Call<Searchsongs>, r : retrofit2.Response<Searchsongs>)
            {
                val response = r.body()
                if(response!=null)
                {
                    val tempsongs = response.results
                    stopshimmer()
                    binding.details.visibility = View.GONE
                    searchsongs.clear()
                    if(tempsongs.size!=0)
                    {
                        binding.nosong.visibility = View.GONE
                        binding.recyclerview.visibility = View.VISIBLE
                        searchsongs.addAll(tempsongs)
                        binding.recyclerview.adapter = searchadapter
                        searchadapter.notifyDataSetChanged()
                    }
                    else
                    {
                        binding.recyclerview.visibility = View.GONE
                        binding.nosong.visibility = View.VISIBLE
                    }
                }
            }

            override fun onFailure(call: Call<Searchsongs>, t: Throwable)
            {
//                Toast.makeText(this@MainActivity,""+t.message,Toast.LENGTH_LONG).show()
                stopshimmer()
                binding.details.visibility = View.GONE
            }
        })
    }

    private fun startshimmer()
    {
        binding.shimmerlayout.startShimmer()
        binding.shimmerlayout.visibility = View.VISIBLE
        binding.recyclerview.visibility = View.GONE
        binding.nosong.visibility = View.GONE
        binding.details.visibility = View.GONE
    }

    private fun stopshimmer() {
        binding.shimmerlayout.stopShimmer()
        binding.shimmerlayout.visibility = View.GONE
        binding.recyclerview.visibility = View.VISIBLE
    }

    override fun onclick(pos: Int , playlist : MutableList<Songs> , isclicked : Boolean)
    {
        if(isclicked)
        {
            currentplaylist.clear()
            currentplaylist.addAll(playlist)
        }
        val song = playlist[pos]
        state = NOT_AVAILABLE
        position = pos
        mediaPlayer.reset()
        binding.playpause.setImageResource(R.drawable.ic_loadingsong)
        binding.song.text = song.name
        binding.artists.text = song.primaryArtists
        Picasso.get().load(song.image[1].link).placeholder(R.drawable.loading).into(binding.songphoto)
        binding.current.text = "00:00"
        binding.total.text = "00:00"
        binding.seekBar.progress = 0
//        Toast.makeText(this,""+quality,Toast.LENGTH_SHORT).show()
        mediaPlayer.setDataSource(song.downloadUrl[quality].link)
        mediaPlayer.prepareAsync()
        mediaPlayer.setOnPreparedListener {
            binding.seekBar.max = it.duration
            it?.start()
            state = PLAYING
            binding.playpause.setImageResource(R.drawable.pause)
            binding.total.text = getduration(it.duration.toLong())
        }
    }

    override fun onlongclick(pos: Int, playlist: MutableList<Songs>, isclicked: Boolean)
    {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
        {
            if(checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED )
            {
                requestPermissions(arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE),1)
            }
            else showdialog(playlist[pos])
        }
        else showdialog(playlist[pos])
    }

    private fun showdialog(song : Songs)
    {
        var filename = song.name.replace(" ","_") + "_" + song.primaryArtists.replace(", ","_").replace(" ","_") + "_"
        if(quality==0) filename+="12_KBPS.mp3"
        else if(quality==1) filename+="48_KBPS.mp3"
        else if(quality==2) filename+="96_KBPS.mp3"
        else if(quality==3) filename+="160_KBPS.mp3"
        else filename+="320_KBPS.mp3"

        alertDialog = AlertDialog.Builder(this)
            .setTitle("Download")
            .setMessage("Are you sure you want to download :\n$filename")
            .setPositiveButton("Yes") { dialog, which ->
                Toast.makeText(this,"Downloading ... $filename",Toast.LENGTH_SHORT).show()
                startdownloading(song.downloadUrl[quality].link , filename)
                alertDialog.dismiss()
            }
            .setIcon(R.drawable.download)
            .setNegativeButton("No"){ dialog , which ->
                alertDialog.dismiss()
            }
            .show()
    }

    private fun startdownloading(link: String , filename : String)
    {
        val request = DownloadManager.Request(Uri.parse(link))
        request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI or DownloadManager.Request.NETWORK_MOBILE)
        request.setTitle(filename)
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS , filename)
        val manager = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        manager.enqueue(request)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if(requestCode==1 && grantResults.isNotEmpty() && grantResults[0]==PackageManager.PERMISSION_DENIED)
        {
            Toast.makeText(this,"Permission Denied",Toast.LENGTH_SHORT).show()
        }
    }

    private fun getNextPos(pos: Int, playlist: MutableList<Songs>) = if (pos==playlist.size-1) 0 else pos+1

    private fun getPreviousPos(pos: Int, playlist: MutableList<Songs>) = if (pos==0) playlist.size-1 else pos-1

    fun showPopUp(view : View)
    {
        popupmenu = PopupMenu(this,view)
        popupmenu.setOnMenuItemClickListener(this)
        popupmenu.inflate(R.menu.menu)
        popupmenu.show()
        checkMenuItem()
    }

    private fun checkMenuItem()
    {
        if(quality==0)
            popupmenu.menu.findItem(R.id.first).setChecked(true)
        else if(quality==1)
            popupmenu.menu.findItem(R.id.second).setChecked(true)
        else if(quality==2)
            popupmenu.menu.findItem(R.id.third).setChecked(true)
        else if(quality==3)
            popupmenu.menu.findItem(R.id.fourth).setChecked(true)
        else
            popupmenu.menu.findItem(R.id.fifth).setChecked(true)
    }

    override fun onMenuItemClick(item: MenuItem?): Boolean
    {
        if(item!=null)
        {
            when(item.itemId)
            {
                R.id.first ->{
                    popupmenu.menu.findItem(R.id.first).isChecked = true
                    quality = 0
                    Toast.makeText(this,"Streaming quality set to 12 KBPS",Toast.LENGTH_SHORT).show()
                    sharedPreferences.edit().putInt("quality",0).apply()
                }
                R.id.second ->{
                    popupmenu.menu.findItem(R.id.second).isChecked = true
                    quality = 1
                    Toast.makeText(this,"Streaming quality set to 48 KBPS",Toast.LENGTH_SHORT).show()
                    sharedPreferences.edit().putInt("quality",1).apply()
                }
                R.id.third ->{
                    popupmenu.menu.findItem(R.id.third).isChecked = true
                    quality = 2
                    Toast.makeText(this,"Streaming quality set to 96 KBPS",Toast.LENGTH_SHORT).show()
                    sharedPreferences.edit().putInt("quality",2).apply()
                }
                R.id.fourth ->{
                    popupmenu.menu.findItem(R.id.fourth).isChecked = true
                    quality = 3
                    Toast.makeText(this,"Streaming quality set to 160 KBPS",Toast.LENGTH_SHORT).show()
                    sharedPreferences.edit().putInt("quality",3).apply()
                }
                R.id.fifth ->{
                    popupmenu.menu.findItem(R.id.fifth).isChecked = true
                    quality = 4
                    Toast.makeText(this,"Streaming quality set to 320 KBPS",Toast.LENGTH_SHORT).show()
                    sharedPreferences.edit().putInt("quality",4).apply()
                }
            }
        }
        return false
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer.release()
    }

}