// Jury screen.

package com.dialectek.nimbus;

import android.app.AlertDialog;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class Jury extends Fragment {

    private View m_view;
    private Context m_context;
    private String m_dataDirectory;
    private Spinner m_jurorSpinner;
    private ArrayAdapter m_adapterForJurorSpinner;
    private String juror_hint = "<add juror>";
    private EditText m_jurorNameText;
    private EditText m_jurorStatusText;
    private Button m_addJurorButton;
    private Button m_updateJurorStatusButton;
    private Button m_removeJurorButton;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        m_view = inflater.inflate(R.layout.jury, container, false);
        m_context = m_view.getContext();
        String rootDir = m_context.getFilesDir().getAbsolutePath();
        try {
            rootDir = new File(rootDir).getCanonicalPath();
        } catch (IOException ioe) {
        }
        m_dataDirectory = rootDir + "/content";
        return m_view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        TextView juryCaseText = (TextView)m_view.findViewById(R.id.jury_case_text);
        juryCaseText.setText("Jury for " + Cases.CaseName + ":");
        m_jurorSpinner = (Spinner)m_view.findViewById(R.id.juror_spinner);
        m_jurorSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                int c = m_jurorSpinner.getCount();
                if (c > 0) {
                    String selected = m_jurorSpinner.getSelectedItem().toString();
                    if (selected != null && !selected.equals("") && !selected.equals(juror_hint)) {
                        String[] jurorParts = selected.split("/", 2);
                        if (jurorParts != null && jurorParts.length == 2) {
                            m_jurorNameText.setText(jurorParts[0]);
                            m_jurorStatusText.setText(jurorParts[1]);
                        }
                    }
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
            }

        });
        m_adapterForJurorSpinner = new ArrayAdapter(m_view.getContext(), android.R.layout.simple_spinner_item);
        m_adapterForJurorSpinner.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        m_jurorSpinner.setAdapter(m_adapterForJurorSpinner);
        m_jurorNameText = (EditText)m_view.findViewById(R.id.juror_name_text);
        m_jurorStatusText = (EditText)m_view.findViewById(R.id.juror_status_text);
        m_addJurorButton = (Button)m_view.findViewById(R.id.add_juror_button);
        m_updateJurorStatusButton = (Button)m_view.findViewById(R.id.update_juror_status_button);
        m_updateJurorStatusButton.setEnabled(false);
        m_removeJurorButton = (Button)m_view.findViewById(R.id.remove_juror_button);
        m_removeJurorButton.setEnabled(false);

        m_addJurorButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v) {
                String jurorName = m_jurorNameText.getText().toString();
                String jurorStatus = m_jurorStatusText.getText().toString();
                if (jurorName != null && !jurorName.isEmpty() &&
                    jurorStatus != null && !jurorStatus.isEmpty()) {
                        addJuror(jurorName, jurorStatus);
                }
            }
        });

        m_updateJurorStatusButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v) {
                String jurorName = m_jurorNameText.getText().toString();
                String jurorStatus = m_jurorStatusText.getText().toString();
                if (jurorName != null && !jurorName.isEmpty() &&
                        jurorStatus != null && !jurorStatus.isEmpty()) {
                    updateJurorStatus(jurorName, jurorStatus);
                }
            }
        });

        m_removeJurorButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v) {
                String jurorName = m_jurorNameText.getText().toString();
                if (jurorName != null && !jurorName.isEmpty()) {
                    removeJuror(jurorName);
                }
            }
        });

        // Get current jury.
        getJury();
    }

    // Get jury from server.
    private void getJury() {
        String s = Cases.Server + "/EvenTheOdds/rest/service/get_jury/" + Cases.CaseName;
        if (!s.startsWith("http"))
        {
            s = "http://" + s;
        }
        final String URLname = s;
        new Thread(new Runnable(){
            @Override
            public void run() {
                Handler handler = new Handler(Looper.getMainLooper());
                HTTPget http = new HTTPget(URLname);
                try {
                    final int status = http.get();
                    if (status == HttpURLConnection.HTTP_OK) {
                        BufferedReader rd = new BufferedReader(new InputStreamReader(
                                http.httpConn.getInputStream()));
                        String s = "";
                        String line;
                        while ((line = rd.readLine()) != null) {
                            s += line;
                        }
                        rd.close();
                        final String response = s;
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                m_adapterForJurorSpinner.clear();
                                if (response.equals("[]")) {
                                    Toast toast = Toast.makeText(m_context, "No jurors for case "
                                            + Cases.CaseName, Toast.LENGTH_LONG);
                                    toast.setGravity(Gravity.CENTER, 0, 0);
                                    toast.show();
                                    CharSequence textHolder = juror_hint;
                                    m_adapterForJurorSpinner.add(textHolder);
                                    m_updateJurorStatusButton.setEnabled(false);
                                    m_removeJurorButton.setEnabled(false);
                                    m_jurorNameText.setText("");
                                    m_jurorStatusText.setText("");
                                } else {
                                    String[] jurors = response.split("\\[");
                                    jurors = jurors[1].split("\\]");
                                    jurors = jurors[0].split(",");
                                    boolean first = true;
                                    for (String jurorInfo : jurors) {
                                        if (first) {
                                            first = false;
                                            String[] jurorParts = jurorInfo.split("/", 2);
                                            if (jurorParts != null && jurorParts.length == 2) {
                                                m_jurorNameText.setText(jurorParts[0]);
                                                m_jurorStatusText.setText(jurorParts[1]);
                                            }
                                        }
                                        CharSequence textHolder = jurorInfo.trim();
                                        m_adapterForJurorSpinner.add(textHolder);
                                    }
                                    m_updateJurorStatusButton.setEnabled(true);
                                    m_removeJurorButton.setEnabled(true);
                                }
                            }
                        });
                    } else {
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                Toast toast;
                                if (status == -1) {
                                    toast = Toast.makeText(m_context, "Cannot get jury",
                                            Toast.LENGTH_LONG);
                                } else {
                                    toast = Toast.makeText(m_context, "Cannot get jury: status="
                                            + status, Toast.LENGTH_LONG);
                                }
                                toast.setGravity(Gravity.CENTER, 0, 0);
                                toast.show();
                            }
                        });
                    }
                }
                catch (final Exception e) {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            Toast toast = Toast.makeText(m_context, "Cannot get jury: exception=" +
                                    e.getMessage(), Toast.LENGTH_LONG);
                            toast.setGravity(Gravity.CENTER, 0, 0);
                            toast.show();
                        }
                    });
                } finally {
                    http.close();
                }
            }
        }).start();
    }

    // Add juror.
    private void addJuror(String jurorName, String jurorStatus) {
        String s = Cases.Server + "/EvenTheOdds/rest/service/add_juror/" +
                Cases.CaseName + "/" + jurorName + "/" + jurorStatus;
        if (!s.startsWith("http"))
        {
            s = "http://" + s;
        }
        final String URLname = s;
        new Thread(new Runnable(){
            @Override
            public void run() {
                Handler handler = new Handler(Looper.getMainLooper());
                HTTPget http = new HTTPget(URLname);
                try {
                    final int status = http.get();
                    if (status == HttpURLConnection.HTTP_OK) {
                        getJury();
                    } else {
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                Toast toast;
                                if (status == -1) {
                                    toast = Toast.makeText(m_context, "Cannot add juror",
                                            Toast.LENGTH_LONG);
                                } else {
                                    toast = Toast.makeText(m_context, "Cannot add juror: status="
                                            + status, Toast.LENGTH_LONG);
                                }
                                toast.setGravity(Gravity.CENTER, 0, 0);
                                toast.show();
                            }
                        });
                    }
                }
                catch (final Exception e) {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            Toast toast = Toast.makeText(m_context, "Cannot add juror: exception=" +
                                    e.getMessage(), Toast.LENGTH_LONG);
                            toast.setGravity(Gravity.CENTER, 0, 0);
                            toast.show();
                        }
                    });
                } finally {
                    http.close();
                }
            }
        }).start();
    }

    // Update juror status.
    private void updateJurorStatus(String jurorName, String jurorStatus) {
        String s = Cases.Server + "/EvenTheOdds/rest/service/update_juror_status/" +
                Cases.CaseName + "/" + jurorName + "/" + jurorStatus;
        if (!s.startsWith("http"))
        {
            s = "http://" + s;
        }
        final String URLname = s;
        new Thread(new Runnable(){
            @Override
            public void run() {
                Handler handler = new Handler(Looper.getMainLooper());
                HTTPget http = new HTTPget(URLname);
                try {
                    final int status = http.get();
                    if (status == HttpURLConnection.HTTP_OK) {
                        getJury();
                    } else {
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                Toast toast;
                                if (status == -1) {
                                    toast = Toast.makeText(m_context, "Cannot update juror",
                                            Toast.LENGTH_LONG);
                                } else {
                                    toast = Toast.makeText(m_context, "Cannot update juror: status="
                                            + status, Toast.LENGTH_LONG);
                                }
                                toast.setGravity(Gravity.CENTER, 0, 0);
                                toast.show();
                            }
                        });
                    }
                }
                catch (final Exception e) {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            Toast toast = Toast.makeText(m_context, "Cannot update juror: exception=" +
                                    e.getMessage(), Toast.LENGTH_LONG);
                            toast.setGravity(Gravity.CENTER, 0, 0);
                            toast.show();
                        }
                    });
                } finally {
                    http.close();
                }
            }
        }).start();
    }

    // Remove juror.
    private void removeJuror(String jurorName) {
        String s = Cases.Server + "/EvenTheOdds/rest/service/remove_juror/" +
                Cases.CaseName + "/" + jurorName;
        if (!s.startsWith("http"))
        {
            s = "http://" + s;
        }
        final String URLname = s;
        new Thread(new Runnable(){
            @Override
            public void run() {
                Handler handler = new Handler(Looper.getMainLooper());
                HTTPget http = new HTTPget(URLname);
                try {
                    final int status = http.get();
                    if (status == HttpURLConnection.HTTP_OK) {
                        getJury();
                    } else {
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                Toast toast;
                                if (status == -1) {
                                    toast = Toast.makeText(m_context, "Cannot remove juror",
                                            Toast.LENGTH_LONG);
                                } else {
                                    toast = Toast.makeText(m_context, "Cannot remove juror: status="
                                            + status, Toast.LENGTH_LONG);
                                }
                                toast.setGravity(Gravity.CENTER, 0, 0);
                                toast.show();
                            }
                        });
                    }
                }
                catch (final Exception e) {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            Toast toast = Toast.makeText(m_context, "Cannot remove juror: exception=" +
                                    e.getMessage(), Toast.LENGTH_LONG);
                            toast.setGravity(Gravity.CENTER, 0, 0);
                            toast.show();
                        }
                    });
                } finally {
                    http.close();
                }
            }
        }).start();
    }
}
