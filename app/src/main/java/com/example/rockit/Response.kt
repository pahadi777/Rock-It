package com.example.rockit

data class Response (val data : SearchedSongs)

data class SearchedSongs(val results : MutableList<Songs> , val songs : MutableList<Songs>)

data class Songs(val name : String , val primaryArtists : String , val image : MutableList<SongMetaData> , val downloadUrl : MutableList<SongMetaData>)

data class SongMetaData(val quality : String , val link : String)