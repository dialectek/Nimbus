// Even (the odds) main activity.

package com.dialectek.nimbus;

import android.Manifest;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Color;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Bundle;
import android.os.StatFs;
import android.text.format.Formatter;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public final class MainActivity extends AppCompatActivity {
    public static final String TAG = "EvenTheOdds";

    public static String mRecordingFile;
    public static String mServersFile;
    private ArrayList<String> mPlaylist;
    private int mPlaylistIndex;
    private String m_dataDirectory;
    private Button mRecordButton;
    private Button mPlayButton;
    private Button mPauseButton;
    private Button mNextButton;
    private TextView mTextLog;
    private SeekBar mSeekbarAudio;
    private TextView mTextSeekbarAudio;
    private ScrollView mScrollContainer;
    private PlayerAdapter mPlayerAdapter;
    private boolean mUserIsSeeking = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // EULA.
        Eula.show(this);

        String rootDir = getFilesDir().getAbsolutePath();
        try {
            rootDir = new File(rootDir).getCanonicalPath();
        } catch (IOException ioe) {
        }
        mRecordingFile = rootDir + "/recording.3gp";
        File file = new File(mRecordingFile);
        if (file.exists()) {
            file.delete();
        }
        m_dataDirectory = rootDir + "/content";
        File dir = new File(m_dataDirectory);
        if (!dir.exists()) {
            if (!dir.mkdir()) {
                Log.e(TAG, "onCreate: cannot create data directory " + m_dataDirectory);
            }
        }
        mServersFile = rootDir + "/servers.txt";

        ActivityCompat.requestPermissions(this, permissions, REQUEST_RECORD_AUDIO_PERMISSION);

        setContentView(R.layout.activity_main);
        initializeUI();
        initializeSeekbar();
        initializePlaybackController();

        Log.d(TAG, "onCreate: finished");
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onStart: create MediaPlayer");
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (isChangingConfigurations() && mPlayerAdapter.isPlaying()) {
            Log.d(TAG, "onStop: don't release MediaPlayer as screen is rotating & playing");
        } else {
            mPlayerAdapter.release();
            Log.d(TAG, "onStop: release MediaPlayer");
        }
    }

    private void initializeUI() {
        // Record button.
        mRecordButton = (Button) findViewById(R.id.button_record);
        int buttonWidth = (int) ((float) Resources.getSystem().getDisplayMetrics().widthPixels * 0.6f);
        mRecordButton.setWidth(buttonWidth);
        mRecordButton.setOnClickListener(new View.OnClickListener() {
                                             static final int StartColor = Color.BLACK;
                                             static final int StopColor = Color.RED;
                                             boolean mStartRecording = true;
                                             MediaRecorder mRecorder;

                                             @Override
                                             public void onClick(View v) {
                                                 mPlayerAdapter.reset();
                                                 if (mStartRecording) {
                                                     mRecorder = new MediaRecorder();
                                                     mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
                                                     mRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
                                                     mRecorder.setOutputFile(mRecordingFile);
                                                     mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);

                                                     try {
                                                         mRecorder.prepare();
                                                     } catch (IOException e) {
                                                         Toast toast = Toast.makeText(MainActivity.this, "Cannot start recording", Toast.LENGTH_LONG);
                                                         toast.setGravity(Gravity.CENTER, 0, 0);
                                                         toast.show();
                                                         return;
                                                     }

                                                     DateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy z hh:mm:ss aa");
                                                     String timeString = dateFormat.format(new Date()).toString() + ".";
                                                     logMessage("Start recording at: " + timeString);

                                                     mRecorder.start();
                                                     mRecordButton.setTextColor(StopColor);
                                                     mRecordButton.setText("Stop recording");

                                                     Animation anim = new AlphaAnimation(0.35f, 1.0f);
                                                     anim.setDuration(500);
                                                     anim.setStartOffset(20);
                                                     anim.setRepeatMode(Animation.REVERSE);
                                                     anim.setRepeatCount(Animation.INFINITE);
                                                     mRecordButton.startAnimation(anim);
                                                 } else {
                                                     DateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy z hh:mm:ss aa");
                                                     String timeString = dateFormat.format(new Date()).toString() + ".";
                                                     logMessage("Stop recording at: " + timeString);
                                                     mRecordButton.clearAnimation();
                                                     mRecorder.stop();
                                                     mRecorder.release();
                                                     mRecorder = null;
                                                     mRecordButton.setTextColor(StartColor);
                                                     mRecordButton.setText("Start recording");
                                                     mPlaylist = new ArrayList<String>();
                                                     mPlaylist.add(mRecordingFile);
                                                     mPlaylistIndex = 0;
                                                     mPlayButton.setEnabled(false);
                                                     mPauseButton.setEnabled(false);
                                                     mNextButton.setEnabled(false);
                                                     if (new File(mRecordingFile).exists()) {
                                                         if (mPlayerAdapter.setDataSource(mRecordingFile)) {
                                                             mPlayButton.setEnabled(true);
                                                             mPauseButton.setEnabled(true);
                                                             mNextButton.setEnabled(false);
                                                             logMessage("Recording ready to play (duration=" + getTimeFromMS(mPlayerAdapter.getPlayDuration()) + ").");
                                                         } else {
                                                             Toast toast = Toast.makeText(MainActivity.this, "Cannot play recording", Toast.LENGTH_LONG);
                                                             toast.setGravity(Gravity.CENTER, 0, 0);
                                                             toast.show();
                                                         }
                                                     } else {
                                                         Toast toast = Toast.makeText(MainActivity.this, "Cannot access recording", Toast.LENGTH_LONG);
                                                         toast.setGravity(Gravity.CENTER, 0, 0);
                                                         toast.show();
                                                     }
                                                 }

                                                 mStartRecording = !mStartRecording;
                                             }
                                         }
        );

        // Save.
        Button saveButton = (Button) findViewById(R.id.button_save);
        saveButton.setWidth(buttonWidth);
        saveButton.setOnClickListener(new View.OnClickListener() {
                                          String m_saved;

                                          @Override
                                          public void onClick(View v) {
                                              mPlayerAdapter.reset();
                                              if (new File(mRecordingFile).exists()) {
                                                  DateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy z hh:mm:ss aa");
                                                  String fileString = dateFormat.format(new Date()).toString() + ".3gp";
                                                  FileManager.Default_File_Name = fileString;
                                              } else {
                                                  FileManager.Default_File_Name = "";
                                              }
                                              FileManager recordSaver = new FileManager(MainActivity.this, m_dataDirectory, FileManager.SAVE_RECORD,
                                                      new FileManager.Listener() {
                                                          @Override
                                                          public void onSave(String savedFile) {
                                                              m_saved = savedFile;
                                                              logMessage(savedFile.replace(m_dataDirectory, "") + " saved.");
                                                          }

                                                          @Override
                                                          public void onSelect(String selectedFile) {
                                                          }

                                                          @Override
                                                          public void onDelete(String deletedFile) {
                                                          }

                                                          @Override
                                                          public void onStorage() {
                                                              logAvailableStorage();
                                                              logTotalStorage();
                                                          }
                                                      }
                                              );
                                              recordSaver.chooseFile_or_Dir();
                                          }
                                      }
        );

        // Browse.
        Button browseButton = (Button) findViewById(R.id.button_browse);
        browseButton.setWidth(buttonWidth);
        browseButton.setOnClickListener(new View.OnClickListener() {
                                            String m_selected;

                                            @Override
                                            public void onClick(View v) {
                                                mPlayerAdapter.reset();
                                                FileManager.Default_File_Name = "";
                                                FileManager recordBrowser = new FileManager(MainActivity.this, m_dataDirectory, FileManager.BROWSE_RECORDS,
                                                        new FileManager.Listener() {
                                                            @Override
                                                            public void onSave(String savedFile) {
                                                            }

                                                            @Override
                                                            public void onSelect(String selectedFile) {
                                                                m_selected = selectedFile;
                                                                String displayFile = selectedFile.replace(m_dataDirectory, "");
                                                                logMessage(displayFile + " selected.");
                                                                File file = new File(selectedFile);
                                                                if (file.exists()) {
                                                                    if (file.isFile()) {
                                                                        mPlaylist = new ArrayList<String>();
                                                                        mPlaylist.add(selectedFile);
                                                                    } else {
                                                                        mPlaylist = listRecordings(selectedFile);
                                                                    }
                                                                    mPlaylistIndex = 0;
                                                                    if (mPlaylist != null) {
                                                                        selectedFile = mPlaylist.get(0);
                                                                        displayFile = selectedFile.replace(m_dataDirectory, "");
                                                                        mPlayButton.setEnabled(false);
                                                                        mPauseButton.setEnabled(false);
                                                                        if (mPlaylist.size() > 1) {
                                                                            mNextButton.setEnabled(true);
                                                                            logMessage(mPlaylist.size() + " recordings found in folder.");
                                                                        } else {
                                                                            mNextButton.setEnabled(false);
                                                                        }
                                                                        if (!copyFile(selectedFile, mRecordingFile)) {
                                                                            Toast toast = Toast.makeText(MainActivity.this, "Cannot access recording " + displayFile, Toast.LENGTH_LONG);
                                                                            toast.setGravity(Gravity.CENTER, 0, 0);
                                                                            toast.show();
                                                                        } else {
                                                                            if (mPlayerAdapter.setDataSource(mRecordingFile)) {
                                                                                mPlayButton.setEnabled(true);
                                                                                mPauseButton.setEnabled(true);
                                                                                logMessage("Ready to play " + displayFile +
                                                                                        " (" + (mPlaylistIndex + 1) + "/" + mPlaylist.size() +
                                                                                        ", duration=" + getTimeFromMS(mPlayerAdapter.getPlayDuration()) + ").");
                                                                            } else {
                                                                                Toast toast = Toast.makeText(MainActivity.this, "Cannot access recording " + displayFile, Toast.LENGTH_LONG);
                                                                                toast.setGravity(Gravity.CENTER, 0, 0);
                                                                                toast.show();
                                                                            }
                                                                        }
                                                                    } else {
                                                                        Toast toast = Toast.makeText(MainActivity.this, "No recordings in folder " + displayFile, Toast.LENGTH_LONG);
                                                                        toast.setGravity(Gravity.CENTER, 0, 0);
                                                                        toast.show();
                                                                    }
                                                                } else {
                                                                    Toast toast = Toast.makeText(MainActivity.this, displayFile + " does not exist", Toast.LENGTH_LONG);
                                                                    toast.setGravity(Gravity.CENTER, 0, 0);
                                                                    toast.show();
                                                                }
                                                            }

                                                            @Override
                                                            public void onDelete(String deletedFile) {
                                                            }

                                                            @Override
                                                            public void onStorage() {
                                                                logAvailableStorage();
                                                                logTotalStorage();
                                                            }
                                                        }
                                                );
                                                recordBrowser.chooseFile_or_Dir();
                                            }
                                        }
        );

        // Delete.
        Button deleteButton = (Button) findViewById(R.id.button_delete);
        deleteButton.setWidth(buttonWidth);
        deleteButton.setOnClickListener(new View.OnClickListener() {
                                            String m_deleted;

                                            @Override
                                            public void onClick(View v) {
                                                mPlayerAdapter.reset();
                                                FileManager.Default_File_Name = "";
                                                FileManager recordDeleter = new FileManager(MainActivity.this, m_dataDirectory, FileManager.DELETE_RECORD,
                                                        new FileManager.Listener() {
                                                            @Override
                                                            public void onSave(String savedFile) {
                                                            }

                                                            @Override
                                                            public void onSelect(String selectedFile) {
                                                            }

                                                            @Override
                                                            public void onDelete(String deletedFile) {
                                                                m_deleted = deletedFile;
                                                                logMessage(deletedFile.replace(m_dataDirectory, "") + " deleted.");
                                                            }

                                                            @Override
                                                            public void onStorage() {
                                                                logAvailableStorage();
                                                                logTotalStorage();
                                                            }
                                                        }
                                                );
                                                recordDeleter.chooseFile_or_Dir();
                                            }
                                        }
        );

        // Cases screen.
        Button casesButton = (Button) findViewById(R.id.button_cases);
        casesButton.setWidth(buttonWidth);
        casesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FragmentManager fm = getFragmentManager();
                FragmentTransaction fragmentTransaction = fm.beginTransaction();
                fragmentTransaction.replace(R.id.frameLayout, new Cases());
                fragmentTransaction.addToBackStack(null);
                fragmentTransaction.commit();
            }
        });

        mTextLog = (TextView) findViewById(R.id.text_log);
        mPlayButton = (Button) findViewById(R.id.button_play);
        mPlayButton.setEnabled(false);
        mPauseButton = (Button) findViewById(R.id.button_pause);
        mPauseButton.setEnabled(false);
        mNextButton = (Button) findViewById(R.id.button_next);
        mNextButton.setEnabled(false);
        mSeekbarAudio = (SeekBar) findViewById(R.id.seekbar_audio);
        mTextSeekbarAudio = (TextView) findViewById(R.id.text_seekbar_audio);
        mTextSeekbarAudio.setText("0:0");
        setTextSeekbarX(mTextSeekbarAudio, mSeekbarAudio);
        mScrollContainer = (ScrollView) findViewById(R.id.scroll_container);

        mPlayButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        mPlayerAdapter.play();
                    }
                });
        mPauseButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        mPlayerAdapter.pause();
                    }
                });
        mNextButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (mPlaylist != null && mPlaylistIndex < mPlaylist.size() - 1) {
                            mPlaylistIndex++;
                            mPlayButton.setEnabled(false);
                            mPauseButton.setEnabled(false);
                            if (mPlaylistIndex < mPlaylist.size() - 1) {
                                mNextButton.setEnabled(true);
                            } else {
                                mNextButton.setEnabled(false);
                            }
                            String selectedFile = mPlaylist.get(mPlaylistIndex);
                            String displayFile = selectedFile.replace(m_dataDirectory, "");
                            File file = new File(selectedFile);
                            if (file.exists()) {
                                if (!copyFile(selectedFile, mRecordingFile)) {
                                    Toast toast = Toast.makeText(MainActivity.this, "Cannot access recording " + displayFile, Toast.LENGTH_LONG);
                                    toast.setGravity(Gravity.CENTER, 0, 0);
                                    toast.show();
                                } else {
                                    if (mPlayerAdapter.setDataSource(mRecordingFile)) {
                                        mPlayButton.setEnabled(true);
                                        mPauseButton.setEnabled(true);
                                        logMessage("Ready to play " + displayFile +
                                                " (" + (mPlaylistIndex + 1) + "/" + mPlaylist.size() +
                                                ", duration=" + getTimeFromMS(mPlayerAdapter.getPlayDuration()) + ").");
                                    } else {
                                        Toast toast = Toast.makeText(MainActivity.this, "Cannot access recording " + displayFile, Toast.LENGTH_LONG);
                                        toast.setGravity(Gravity.CENTER, 0, 0);
                                        toast.show();
                                    }
                                }
                            } else {
                                Toast toast = Toast.makeText(MainActivity.this, "Cannot access recording " + displayFile, Toast.LENGTH_LONG);
                                toast.setGravity(Gravity.CENTER, 0, 0);
                                toast.show();
                            }
                        }
                    }
                });
    }

    private void initializePlaybackController() {
        MediaPlayerHolder mMediaPlayerHolder = new MediaPlayerHolder(this);
        Log.d(TAG, "initializePlaybackController: created MediaPlayerHolder");
        mMediaPlayerHolder.setPlaybackInfoListener(new PlaybackListener());
        mPlayerAdapter = mMediaPlayerHolder;
        Log.d(TAG, "initializePlaybackController: MediaPlayerHolder progress callback set");
    }

    private void initializeSeekbar() {
        mSeekbarAudio.setOnSeekBarChangeListener(
                new SeekBar.OnSeekBarChangeListener() {
                    int userSelectedPosition = 0;

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {
                        mUserIsSeeking = true;
                    }

                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                        if (fromUser) {
                            userSelectedPosition = progress;
                        }

                        mTextSeekbarAudio.setText(getTimeFromMS(progress));
                        setTextSeekbarX(mTextSeekbarAudio, seekBar);
                    }

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {
                        mUserIsSeeking = false;
                        mPlayerAdapter.seekTo(userSelectedPosition);
                    }
                });
    }

    void setTextSeekbarX(TextView text, SeekBar seekBar) {
        int width = seekBar.getWidth() - seekBar.getPaddingLeft() - seekBar.getPaddingRight();
        int thumbPos = seekBar.getPaddingLeft() + width * seekBar.getProgress() / seekBar.getMax();
        text.measure(0,0);
        int txtW = text.getMeasuredWidth();
        int delta = txtW / 2;
        text.setX(seekBar.getX()+thumbPos -delta);
    }

    public class PlaybackListener extends PlaybackInfoListener
    {
        @Override
        public void onDurationChanged(int duration)
        {
            mSeekbarAudio.setMax(duration);
            Log.d(TAG, String.format("setPlaybackDuration: setMax(%d)", duration));
        }

        @RequiresApi(api = Build.VERSION_CODES.N)
        @Override
        public void onPositionChanged(int position)
        {
            if (!mUserIsSeeking) {
                mSeekbarAudio.setProgress(position, true);
                Log.d(TAG, String.format("setPlaybackPosition: setProgress(%d)", position));
            }
        }

        @Override
        public void onStateChanged(@State int state)
        {
            String stateToString = PlaybackInfoListener.convertStateToString(state);
            onLogUpdated(String.format("onStateChanged(%s)", stateToString));
        }

        @Override
        public void onPlaybackCompleted()
        {
        }

        @Override
        public void onLogUpdated(String message)
        {
            //logMessage(message);
        }
    }

    // Log message.
    public void logMessage(String message)
    {
        if (mTextLog != null) {
            mTextLog.append(message);
            mTextLog.append("\n");
            // Moves the scrollContainer focus to the end.
            mScrollContainer.post(
                    new Runnable() {
                        @Override
                        public void run() {
                            mScrollContainer.fullScroll(ScrollView.FOCUS_DOWN);
                        }
                    });
        }
    }

    // Requesting permission to RECORD_AUDIO
    private static final int    REQUEST_RECORD_AUDIO_PERMISSION = 200;
    private boolean permissionToRecordAccepted = false;
    private         String [] permissions = { Manifest.permission.RECORD_AUDIO };

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults)
    {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode)
        {
            case REQUEST_RECORD_AUDIO_PERMISSION:
                permissionToRecordAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                break;
        }
        if (!permissionToRecordAccepted) { finish(); }
    }

    // Copy file.
    public static boolean copyFile(String fromFile, String toFile)
    {
        File sourceLocation = new File(fromFile);
        File targetLocation = new File(toFile);

        InputStream in = null;
        OutputStream out = null;
        boolean result = false;
        try
        {
            in = new FileInputStream(sourceLocation);
            out = new FileOutputStream(targetLocation);
            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0)
            {
                out.write(buf, 0, len);
            }
            result = true;
        } catch (Exception e) {
        } finally
        {
            try
            {
                if (in != null) in.close();
                if (out != null) out.close();
            } catch (Exception e) {
                result = false;
            }
        }

        return(result);
    }

    // Recursively list recordings in directory.
    public ArrayList<String> listRecordings(String dir) {
        ArrayList<String> results = new ArrayList<String>();
        File[] files = new File(dir).listFiles();
        for(File f:files) {
            String name = f.getName();
            if (f.isDirectory()) {
                ArrayList<String> subResults = listRecordings(f.getPath());
                if (subResults != null) {
                    for (String path : subResults)
                    {
                        results.add(path);
                    }
                }
            } else {
                try {
                    results.add(f.getCanonicalPath());
                } catch (Exception e) {
                    Toast toast = Toast.makeText(MainActivity.this, "Cannot get path", Toast.LENGTH_LONG);
                    toast.setGravity(Gravity.CENTER, 0, 0);
                    toast.show();
                    return null;
                }
            }
        }
        if (results.size() > 0)
        {
            return results;
        } else {
            return null;
        }
    }

    // Convert time from ms to mins:secs
    public String getTimeFromMS(int ms)
    {
        int seconds = ms / 1000;
        int minutes = seconds / 60;
        seconds -= (minutes * 60);
        return minutes + ":" + seconds;
    }

    public long getAvailableStorage() {
        File path = getFilesDir();
        StatFs stat = new StatFs(path.getPath());
        long blockSize = stat.getBlockSize();
        long availableBlocks = stat.getAvailableBlocks();
        long size = availableBlocks * blockSize;
        return size;
    }

    public long getTotalStorage() {
        File path = getFilesDir();
        StatFs stat = new StatFs(path.getPath());
        long blockSize = stat.getBlockSize();
        long totalBlocks = stat.getBlockCount();
        long size = totalBlocks * blockSize;
        return size;
    }

    public void logAvailableStorage() {
        logMessage("Available storage=" + Formatter.formatFileSize(this, getAvailableStorage()) + ".");
    }

    public void logTotalStorage() {
        logMessage("Total storage=" + Formatter.formatFileSize(this, getTotalStorage()) + ".");
    }

    public static boolean isAlphanumeric(String str)
    {
        char[] charArray = str.toCharArray();
        for(char c:charArray)
        {
            if (!Character.isLetterOrDigit(c) && c != ' ')
                return false;
        }
        return true;
    }
}

