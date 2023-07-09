package ru.netology.musicplayer

import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.ImageButton
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.netology.musicplayer.RetrofitClient.BASE_URL


class MainActivity : AppCompatActivity(), TrackAdapter.OnItemClickListener {

    private lateinit var trackRecyclerView: RecyclerView
    private lateinit var trackAdapter: TrackAdapter

    private lateinit var mediaPlayer: MediaPlayer
    private var currentTrackIndex: Int = -1
    private var isPlaying: Boolean = false
    private var isUserSeeking: Boolean = false

    private val handler = Handler(Looper.getMainLooper())
    private lateinit var runnable: Runnable

    private lateinit var albumTitleTextView: TextView
    private lateinit var albumSubtitleTextView: TextView
    private lateinit var albumArtistTextView: TextView
    private lateinit var albumPublishedTextView: TextView
    private lateinit var albumGenreTextView: TextView

    private lateinit var trackSeekBar: SeekBar
    private lateinit var playButton: ImageButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        trackRecyclerView = findViewById(R.id.trackRecyclerView)
        trackRecyclerView.layoutManager = LinearLayoutManager(this)

        albumTitleTextView = findViewById(R.id.albumTitleTextView)
        albumSubtitleTextView = findViewById(R.id.albumSubtitleTextView)
        albumArtistTextView = findViewById(R.id.albumArtistTextView)
        albumPublishedTextView = findViewById(R.id.albumPublishedTextView)
        albumGenreTextView = findViewById(R.id.albumGenreTextView)

        CoroutineScope(Dispatchers.Main).launch {
            try {
                val album: Album = withContext(Dispatchers.IO) {
                    RetrofitClient.albumApi.getAlbumData()
                }

                val tracks = album.tracks
                trackAdapter = TrackAdapter(tracks)
                trackAdapter.setOnItemClickListener(this@MainActivity)
                trackRecyclerView.adapter = trackAdapter

                // Отображение информации об альбоме
                albumTitleTextView.text = album.title
                albumSubtitleTextView.text = album.subtitle
                albumArtistTextView.text = album.artist
                albumPublishedTextView.text = album.published
                albumGenreTextView.text = album.genre

            } catch (e: Exception) {
                // Произошла ошибка при загрузке данных
                Toast.makeText(this@MainActivity, "Ошибка загрузки данных", Toast.LENGTH_SHORT).show()
            }
        }

        // Создание экземпляра MediaPlayer
        mediaPlayer = MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            setOnCompletionListener {
                // Воспроизведение следующего трека после завершения текущего
                playNextTrack()
            }
            setOnPreparedListener {
                trackSeekBar.max = mediaPlayer.duration // Установка максимального значения SeekBar после получения длительности трека
                updateSeekBar()
            }
        }

        playButton = findViewById(R.id.playButton)
        playButton.setOnClickListener {
            if (isPlaying) {
                pausePlayback()
            } else {
                if (currentTrackIndex == -1) {
                    playTrack(0)
                }
            }
            updatePlayButton()
        }



        trackSeekBar = findViewById(R.id.trackSeekBar)
        trackSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    // Обновляем позицию проигрывания в соответствии с измененным прогрессом
                    if (isUserSeeking) {
                        val duration = mediaPlayer.duration
                        val newPosition = duration * progress / 100
                        mediaPlayer.seekTo(newPosition)
                    }
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                isUserSeeking = true
                handler.removeCallbacks(runnable) // Останавливаем обновление позиции SeekBar во время перемотки
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                if (isUserSeeking) {
                    // При окончании перемотки обновляем позицию проигрывания и возобновляем обновление SeekBar
                    val progress = seekBar?.progress ?: 0
                    val duration = mediaPlayer.duration
                    val newPosition = duration * progress / 100
                    mediaPlayer.seekTo(newPosition)
                    isUserSeeking = false

                    handler.postDelayed(runnable, 1000) // Возобновляем обновление позиции SeekBar
                }
            }
        })

        runnable = object : Runnable {
            override fun run() {
                updateSeekBar()
                handler.postDelayed(this, 1000)
            }
        }
        handler.postDelayed(runnable, 1000)
    }

    override fun onItemClick(position: Int) {
        // Воспроизведение выбранного трека
        playTrack(position)
    }

    private fun playTrack(position: Int) {
        if (currentTrackIndex != position) {
            currentTrackIndex = position
            trackAdapter.setCurrentTrackIndex(currentTrackIndex)
            trackAdapter.notifyDataSetChanged()
        }

        val currentTrack = trackAdapter.getItem(currentTrackIndex)
        val trackUrl = getTrackUrl(currentTrack)
        mediaPlayer.reset()
        mediaPlayer.setDataSource(trackUrl)
        mediaPlayer.prepareAsync()

        mediaPlayer.setOnPreparedListener {
            mediaPlayer.start()
            isPlaying = true
            updatePlayButton()
            updateSeekBar()

            trackAdapter.setTrackActive(currentTrackIndex, true)
        }
    }

    private fun playNextTrack() {
        currentTrackIndex = (currentTrackIndex + 1) % trackAdapter.itemCount
        playTrack(currentTrackIndex)
    }

    private fun resumePlayback() {
        if (!mediaPlayer.isPlaying) {
            mediaPlayer.start()
            isPlaying = true
        }
    }

    private fun pausePlayback() {
        if (mediaPlayer.isPlaying) {
            mediaPlayer.pause()
            isPlaying = false
        }
    }

    private fun updatePlayButton() {
        val playButtonImage = if (isPlaying) R.drawable.pause_button else R.drawable.play_button
        playButton.setImageResource(playButtonImage)
    }

    private fun updateSeekBar() {
        val currentPosition = mediaPlayer.currentPosition
        val totalDuration = mediaPlayer.duration
        val progress = if (totalDuration != 0) {
            (currentPosition * 100) / totalDuration
        } else {
            0 // или другое значение по умолчанию
        }
        trackSeekBar.progress = progress
    }

    private fun getTrackUrl(track: Track): String {
        return BASE_URL + track.file
    }

    override fun onDestroy() {
        mediaPlayer.release()
        handler.removeCallbacks(runnable) // Удаляем обновление позиции SeekBar
        super.onDestroy()
    }
}
