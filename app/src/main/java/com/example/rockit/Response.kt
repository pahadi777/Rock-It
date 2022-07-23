package com.example.rockit

data class Response (val results : MyResult)

data class Searchsongs (val results : MutableList<Songs>)

data class MyResult(val songs : MutableList<Songs>)

data class Songs(val name : String , val primaryArtists : String , val image : MutableList<SongMetaData> , val downloadUrl : MutableList<SongMetaData>)

data class SongMetaData(val quality : String , val link : String)

//class Songs()
//{
//    private lateinit var name : String
//    private lateinit var primaryArtists : String
//    private lateinit var image : MutableList<SongMetaData>
//    private var downloadUrl : MutableList<SongMetaData>? = null
//    private var isAvailable = true
//
//    fun getname() = this.name
//    fun getprimaryartists() = this.primaryArtists
//    fun getimage() = this.image
//    fun getdownloadurl() = this.downloadUrl
//    fun getisavailable() = this.isAvailable
//
//    constructor(name : String , primaryArtists : String , image : MutableList<SongMetaData> , downloadUrl : Boolean) : this() {
//        Log.d("callingfirst" , "true")
//        this.name = name
//        this.primaryArtists = primaryArtists
//        this.image = image
//        this.isAvailable = downloadUrl
//    }
//
//    constructor(name : String , primaryArtists : String , image : MutableList<SongMetaData> , downloadUrl : MutableList<SongMetaData>) : this()
//    {
//        Log.d("callingsecond" , "true")
//        this.name = name
//        this.primaryArtists = primaryArtists
//        this.image = image
//        this.downloadUrl = downloadUrl
//    }
//}

//class DownloadUrl()
//{
//    lateinit var downloadUrl : MutableList<SongMetaData>
//    var isTrue = true
//    constructor(downloadUrl : Boolean) : this() {
//        this.isTrue = downloadUrl
//    }
//
//    constructor(downloadUrl: MutableList<SongMetaData>) : this() {
//        this.downloadUrl = downloadUrl
//    }
//}