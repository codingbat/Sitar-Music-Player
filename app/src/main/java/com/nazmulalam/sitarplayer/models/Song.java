package com.nazmulalam.sitarplayer.models;

/*
 * http://code.tutsplus.com/tutorials/create-a-music-player-on-android-project-setup--mobile-22764
 * Sue Smith - February 2014
 */

public class Song {

  private long id;
  private String title;
  private String artist;

  public Song(long songID, String songTitle, String songArtist) {
    id = songID;
    title = songTitle;
    artist = songArtist;
  }

  public long getID() {
    return id;
  }

  public String getTitle() {
    return title;
  }

  public String getArtist() {
    return artist;
  }
}
