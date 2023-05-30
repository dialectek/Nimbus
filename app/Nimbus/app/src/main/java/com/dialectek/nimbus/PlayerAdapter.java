package com.dialectek.nimbus;

import java.util.ArrayList;

/**
 * Allows {@link MainActivity} to control media playback of {@link MediaPlayerHolder}.
 */
public interface PlayerAdapter {

    boolean setDataSource(String filename);

    boolean loadMedia(int resourceId);

    void release();

    boolean isPlaying();

    void play();

    void reset();

    void pause();

    void initializeProgressCallback();

    void seekTo(int position);

    int getPlayDuration();
}
