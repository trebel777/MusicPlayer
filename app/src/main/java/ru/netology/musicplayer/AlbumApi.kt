package ru.netology.musicplayer

import retrofit2.http.GET

interface AlbumApi {
    @GET("album.json")
    suspend fun getAlbumData(): Album
}