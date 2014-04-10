package com.kure.musicplayer.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.MediaController.MediaPlayerControl;

import com.kure.musicplayer.MusicController;
import com.kure.musicplayer.R;
import com.kure.musicplayer.kMP;
import com.kure.musicplayer.adapters.AdapterSong;


/**
 * It is the "Now Playing Queue".
 *
 * Tasks:
 *
 * - List all currently playing songs.
 * - Has a MediaController, little widgets with
 *   buttons to play, pause, skip, etc.
 * - Lets the user append songs to it at any time.
 * - Allows the user to select any song inside it to
 *   start playing right away.
 *
 * Interface:
 *
 * If you want to play a set of musics, set the
 * ArrayList<Song> on `kmP.nowPlayingList` with all
 * the songs you want.
 *
 * Then, send an Extra called "song" that contains
 * the global ID of the Song you want to start
 * playing.
 *
 * - If we don't find that ID on the list, we start
 *   playing from the beginning.
 * - The Extra is optional: if you don't provide it
 *   it does nothing.
 */
public class ActivityNowPlaying extends ActivityMaster
	implements MediaPlayerControl,
	           OnItemClickListener {

	/**
	 * List that will display all the songs.
	 */
	private ListView songListView;

	private boolean paused = false;
	private boolean playbackPaused = false;

	private MusicController musicController;

	@Override
	protected void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_now_playing);

		songListView = (ListView)findViewById(R.id.activity_now_playing_song_list);

		// We'll play this pre-defined list
		kMP.musicService.setList(kMP.nowPlayingList);

		// Connects the song list to an adapter
		// (thing that creates several Layouts from the song list)
		AdapterSong songAdapter = new AdapterSong(this, kMP.nowPlayingList);
		songListView.setAdapter(songAdapter);

		// Looking for an optional extra with the song ID
		// to start playing.
		Intent intent = getIntent();
		Bundle bundle = intent.getExtras();

		if (bundle != null) {

			// If we received an extra with the song position
			// inside the now playing list, start playing it

			int songToPlayIndex = (int)bundle.get("song");

			// Prepare the music service to play the song.
			// `setSong` does limit-checking
			kMP.musicService.setSong(songToPlayIndex);
			kMP.musicService.playSong();
		}

		// Scroll the list view to the current song.
		songListView.setSelection(kMP.musicService.currentSongPosition);

		// We'll get warned when the user clicks on an item.
		songListView.setOnItemClickListener(this);

		setMusicController();

		if (playbackPaused) {
			setMusicController();
			playbackPaused = false;
		}

		// When we're playing music, we'll leave a shortcut on
		// the main screen to us.
		ActivityMenuMain.nowPlaying(this, true);
	}

	/**
	 * Icon that will show on the top menu showing if
	 * `shuffle` is on/off and allowing the user to change it.
	 */
	private MenuItem shuffleItem;

	/**
	 * Icon that will show on the top menu showing if
	 * `repeat` is on/off and allowing the user to change it.
	 */
	private MenuItem repeatItem;

	/**
	 * Let's create the ActionBar (menu on the top).
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.activity_now_playing_action_bar, menu);

		shuffleItem = menu.findItem(R.id.action_bar_shuffle);

		repeatItem = menu.findItem(R.id.action_bar_repeat);

		refreshActionBarItems();

		return super.onCreateOptionsMenu(menu);
	}

	/**
	 * Refreshes the icons on the Action Bar based on the
	 * status of `shuffle` and `repeat`.
	 *
	 * Source:
	 * http://stackoverflow.com/a/11006878
	 */
	private void refreshActionBarItems() {

		shuffleItem.setIcon((kMP.musicService.isShuffle())?
				             R.drawable.ic_menu_shuffle_on:
		                     R.drawable.ic_menu_shuffle_off);

		repeatItem.setIcon((kMP.musicService.isRepeat())?
		                    R.drawable.ic_menu_repeat_on:
		                    R.drawable.ic_menu_repeat_off);
	}

	/**
	 * This method gets called whenever the user clicks an
	 * item on the ActionBar.
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		switch (item.getItemId()) {

		case R.id.action_bar_shuffle:
			kMP.musicService.toggleShuffleMode();
			refreshActionBarItems();
			return true;

		case R.id.action_bar_repeat:
			kMP.musicService.toggleRepeatMode();
			refreshActionBarItems();
			return true;
		}

		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void onStart() {

		super.onStart();

		// WHY CANT I SET THE MUSIC CONTROLLER HERE AND LET IT BE
		// FOREVER?
	//	if (!this.isFinishing()) {
	//	    musicController.show(5000);
	//	}
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {

		if(event.getAction() == KeyEvent.ACTION_DOWN)
			if (keyCode == KeyEvent.KEYCODE_MENU)
				musicController.show();

		return super.onKeyDown(keyCode, event);
	}

	/**
	 * Another Activity is taking focus.
	 * (either from user going to another Activity or home)
	 */
	@Override
	protected void onPause() {
		super.onPause();

		paused = true;
		playbackPaused = true;
	}

	/**
	 * Activity has become visible.
	 *
	 * @see onPause()
	 */
	@Override
	protected void onResume() {
		super.onResume();
		if (paused) {
			// Ensure that the controller
			// is shown when the user returns to the app
			setMusicController();
			paused = false;
		}
	}

	/**
	 * Activity is no longer visible.
	 */
	@Override
	protected void onStop() {
		musicController.hide();
		super.onStop();
	}

	/**
	 * (Re)Starts the musicController.
	 */
	private void setMusicController() {
		musicController = new MusicController(ActivityNowPlaying.this);

		// What will happen when the user presses the
		// next/previous buttons?
		musicController.setPrevNextListeners(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				// Calling method defined on MainActivity
				playNext();
			}
		}, new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				// Calling method defined on MainActivity
				playPrevious();
			}
		});

		// Binding to our media player
		musicController.setMediaPlayer(this);
		musicController.setAnchorView(findViewById(R.id.activity_now_playing_song_list));
		musicController.setEnabled(true);
	}

	@Override
	public void start() {
		kMP.musicService.start();
	}

	@Override
	public void pause() {
		kMP.musicService.pausePlayer();
	}

	@Override
	public int getDuration() {
		if (kMP.musicService != null && kMP.musicService.musicBound && kMP.musicService.isPlaying())
			return kMP.musicService.getDuration();
		else
			return 0;
	}

	@Override
	public int getCurrentPosition() {
		if (kMP.musicService != null && kMP.musicService.musicBound && kMP.musicService.isPlaying())
			return kMP.musicService.getPosition();
		else
			return 0;
	}

	@Override
	public void seekTo(int position) {
		kMP.musicService.seekTo(position);
	}

	@Override
	public boolean isPlaying() {
		if (kMP.musicService != null && kMP.musicService.musicBound)
			return kMP.musicService.isPlaying();

		return false;
	}

	@Override
	public int getBufferPercentage() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public boolean canPause() {
		return true;
	}

	@Override
	public boolean canSeekBackward() {
		return true;
	}

	@Override
	public boolean canSeekForward() {
		return true;
	}

	@Override
	public int getAudioSessionId() {
		// TODO Auto-generated method stub
		return 0;
	}

	// Back to the normal methods

	/**
	 * Jumps to the next song and starts playing it right now.
	 */
	private void playNext() {
		kMP.musicService.next();

		// To prevent the MusicPlayer from behaving
		// unexpectedly when we pause the song playback.
		if (playbackPaused) {
			setMusicController();
			playbackPaused = false;
		}

		musicController.show();
	}

	/**
	 * Jumps to the previous song and starts playing it right now.
	 */
	private void playPrevious() {
		kMP.musicService.previous();

		// To prevent the MusicPlayer from behaving
		// unexpectedly when we pause the song playback.
		if (playbackPaused) {
			setMusicController();
			playbackPaused = false;
		}

		musicController.show();
	}

	/**
	 * When the user selects a music inside the "Now Playing List",
	 * we'll start playing it right away.
	 */
	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

		// Prepare the music service to play the song.
		kMP.musicService.setSong(position);

		// Scroll the list view to the current song.
		songListView.setSelection(position);

		kMP.musicService.playSong();

		if (playbackPaused) {
			setMusicController();
			playbackPaused = false;
		}
	}
}

