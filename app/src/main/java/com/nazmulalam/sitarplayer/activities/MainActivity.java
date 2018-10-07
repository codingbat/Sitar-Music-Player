package com.nazmulalam.sitarplayer.activities;

import android.Manifest;
import android.Manifest.permission;
import android.app.Activity;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.MediaController.MediaPlayerControl;
import com.crashlytics.android.Crashlytics;
import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.CustomEvent;
import com.nazmulalam.sitarplayer.BuildConfig;
import com.nazmulalam.sitarplayer.controllers.MusicController;
import com.nazmulalam.sitarplayer.services.MusicService;
import com.nazmulalam.sitarplayer.services.MusicService.MusicBinder;
import com.nazmulalam.sitarplayer.R;
import com.nazmulalam.sitarplayer.models.Song;
import com.nazmulalam.sitarplayer.adapters.SongAdapter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;


public class MainActivity extends Activity implements MediaPlayerControl {
  private static final String TAG = "MainActivity";
  private static final int MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 123;

  // song list variables
  private ArrayList<Song> songList;
  private ListView songView;

  // service
  private MusicService musicSrv;
  private Intent playIntent;
  // binding
  private boolean musicBound = false;

  // controller
  private MusicController controller;

  // activity and playback pause flags
  private boolean paused = false, playbackPaused = false;
  private boolean playerVisible = false;
  // connect to the service
  private ServiceConnection musicConnection =
      new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
          MusicBinder binder = (MusicBinder) service;
          // get service
          musicSrv = binder.getService();
          // pass list
          musicSrv.setList(songList);
          musicBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
          musicBound = false;
        }
      };

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    // retrieve list view
    songView = (ListView) findViewById(R.id.song_list);
    // instantiate list
    songList = new ArrayList<Song>();
    // get songs from device
    if (checkPermission()) {
      getSongList();
    }
    // sort alphabetically by title
    Collections.sort(
        songList,
        new Comparator<Song>() {
          public int compare(Song a, Song b) {
            return a.getTitle().compareTo(b.getTitle());
          }
        });
    // create and set adapter
    SongAdapter songAdt = new SongAdapter(this, songList);
    songView.setAdapter(songAdt);

    // setup controller
    setController();
  }

  public void onKeyMetric(String event, String category, String categoryType) {
    if (BuildConfig.USE_CRASHLYTICS) {
      Answers.getInstance().logCustom(new CustomEvent(event)
          .putCustomAttribute(category, categoryType));
    }
  }

  @Override
  public void onBackPressed() {
    onKeyMetric("Exit", "System", "onBackPressed");
    close();
    super.onBackPressed();
  }

  private boolean checkPermission() {
    if (ContextCompat.checkSelfPermission(this, permission.READ_EXTERNAL_STORAGE)
        != PackageManager.PERMISSION_GRANTED) {
      // Permission is not granted
      Log.d(TAG, "Permission not granted");
      if (BuildConfig.USE_CRASHLYTICS) {
        Crashlytics.logException(new Exception("Permission not granted."));
      }
        ActivityCompat.requestPermissions(
            this,
            new String[] {Manifest.permission.READ_EXTERNAL_STORAGE},
            MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE);
    }
    return true;
  }

  // start and bind the service when the activity starts
  @Override
  protected void onStart() {
    super.onStart();
    if (playIntent == null) {
      playIntent = new Intent(this, MusicService.class);
      bindService(playIntent, musicConnection, Context.BIND_AUTO_CREATE);
      startService(playIntent);
    }
  }

  // user song select
  public void songPicked(View view) {
    musicSrv.setSong(Integer.parseInt(view.getTag().toString()));
    musicSrv.playSong();
    if (playbackPaused) {
      setController();
      playbackPaused = false;
      playerVisible = true;
    }
    controller.show(0);
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    // Inflate the menu; this adds items to the action bar if it is present.
    getMenuInflater().inflate(R.menu.main, menu);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    // menu item selected
    switch (item.getItemId()) {
      case R.id.action_shuffle:
        musicSrv.setShuffle();
        break;
      case R.id.action_end:
        close();
        break;
    }
    return super.onOptionsItemSelected(item);
  }

  private void close() {
    stopService(playIntent);
    musicSrv = null;
    System.exit(0);
  }

  // method to retrieve song info from device
  public void getSongList() {
    // query external audio
    ContentResolver musicResolver = getContentResolver();
    Uri musicUri = android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
    Cursor musicCursor = musicResolver.query(musicUri, null, null, null, null);
    // iterate over results if valid
    if (musicCursor != null && musicCursor.moveToFirst()) {
      // get columns
      int titleColumn = musicCursor.getColumnIndex(android.provider.MediaStore.Audio.Media.TITLE);
      int idColumn = musicCursor.getColumnIndex(android.provider.MediaStore.Audio.Media._ID);
      int artistColumn = musicCursor.getColumnIndex(android.provider.MediaStore.Audio.Media.ARTIST);
      // add songs to list
      do {
        long thisId = musicCursor.getLong(idColumn);
        String thisTitle = musicCursor.getString(titleColumn);
        String thisArtist = musicCursor.getString(artistColumn);
        songList.add(new Song(thisId, thisTitle, thisArtist));
      } while (musicCursor.moveToNext());
    }
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
    return 0;
  }

  @Override
  public int getBufferPercentage() {
    return 0;
  }

  @Override
  public int getCurrentPosition() {
    if (musicSrv != null && musicBound && musicSrv.isPng()) return musicSrv.getPosn();
    else return 0;
  }

  @Override
  public int getDuration() {
    if (musicSrv != null && musicBound && musicSrv.isPng()) return musicSrv.getDur();
    else return 0;
  }

  @Override
  public boolean isPlaying() {
    if (musicSrv != null && musicBound) return musicSrv.isPng();
    return false;
  }

  @Override
  public void pause() {
    playbackPaused = true;
    musicSrv.pausePlayer();
  }

  @Override
  public void seekTo(int pos) {
    musicSrv.seek(pos);
  }

  @Override
  public void start() {
    musicSrv.go();
  }

  // set the controller up
  private void setController() {
    if (!playerVisible) {
      controller = new MusicController(this);
      // set previous and next button listeners
      controller.setPrevNextListeners(
          new View.OnClickListener() {
            @Override
            public void onClick(View v) {
              playNext();
              // start();
            }
          },
          new View.OnClickListener() {
            @Override
            public void onClick(View v) {
              playPrev();
              // start();
            }
          });
      // set and show
      controller.setMediaPlayer(this);
      controller.setAnchorView(findViewById(R.id.song_list));
      controller.setEnabled(true);
    }
  }

  private void playNext() {
    musicSrv.playNext();
    if (playbackPaused) {
      setController();
      playbackPaused = false;
    }
    controller.show(0);
  }

  private void playPrev() {
    musicSrv.playPrev();
    if (playbackPaused) {
      setController();
      playbackPaused = false;
    }
    controller.show(0);
  }

  @Override
  protected void onPause() {
    super.onPause();
    paused = true;
  }

  @Override
  protected void onResume() {
    super.onResume();
    if (paused) {
      setController();
      paused = false;
    }
  }

  @Override
  protected void onStop() {
    controller.hide();
    super.onStop();
  }

  @Override
  protected void onDestroy() {
    stopService(playIntent);
    musicSrv = null;
    super.onDestroy();
  }
}
