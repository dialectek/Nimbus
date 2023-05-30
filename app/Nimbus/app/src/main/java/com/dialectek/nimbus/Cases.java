// Cases screen.

package com.dialectek.nimbus;

import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
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

public class Cases extends Fragment {

    private View m_view;
    private Context m_context;
    private String m_dataDirectory;
    private String m_tmpDirectory;
    private Spinner m_serverSpinner;
    private ArrayAdapter m_adapterForServerSpinner;
    private String server_hint = "<add server>";
    private String mServer;
    private Spinner m_caseSpinner;
    private ArrayAdapter m_adapterForCaseSpinner;
    private String case_uninitialized_hint = "<connect to server>";
    private String case_empty_hint = "<new case>";
    private Button m_newCaseButton;
    private Button m_openCaseButton;
    private Button m_juryButton;
    private String mSelectedFileName;
    public static String CaseName;
    public static String Server;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        m_view = inflater.inflate(R.layout.cases, container, false);
        m_context = m_view.getContext();
        String rootDir = m_context.getFilesDir().getAbsolutePath();
        try {
            rootDir = new File(rootDir).getCanonicalPath();
        } catch (IOException ioe) {
        }
        m_dataDirectory = rootDir + "/content";
        m_tmpDirectory = rootDir + "/tmp";
        File dir = new File(m_tmpDirectory);
        if (!dir.exists()) {
            if (!dir.mkdir()) {
                Log.e(MainActivity.TAG, "onCreateView: cannot create tmp directory " + m_tmpDirectory);
            }
        }
        return m_view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        m_serverSpinner = (Spinner)m_view.findViewById(R.id.server_spinner);
        m_serverSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                updateServerFile();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
            }

        });
        m_adapterForServerSpinner = new ArrayAdapter(m_view.getContext(), android.R.layout.simple_spinner_item);
        m_adapterForServerSpinner.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        m_serverSpinner.setAdapter(m_adapterForServerSpinner);
        Button connectServerButton = (Button)m_view.findViewById(R.id.connect_server_button);
        Button addServerButton = (Button)m_view.findViewById(R.id.add_server_button);
        Button removeServerButton = (Button)m_view.findViewById(R.id.remove_server_button);

        connectServerButton.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View v) {
                getServerCases();
            }
        });
        addServerButton.setOnClickListener(new View.OnClickListener()
                                        {
                                            @Override
                                            public void onClick(View v)
                                            {
                                                final EditText input = new EditText(m_context);

                                                // Show new server input dialog
                                                new AlertDialog.Builder(m_context).
                                                        setTitle("Server:").
                                                        setView(input).setPositiveButton("OK", new DialogInterface.OnClickListener()
                                                        {
                                                            public void onClick(DialogInterface dialog, int whichButton)
                                                            {
                                                                String server = input.getText().toString();
                                                                boolean isEmpty = server == null || server.trim().isEmpty();
                                                                boolean isValid = false;
                                                                if (!isEmpty) {
                                                                    server = server.trim();
                                                                    if (!server.equals(server_hint)) {
                                                                        isValid = true;
                                                                    }
                                                                }
                                                                if (isValid) {
                                                                    if (m_serverSpinner.getSelectedItem().toString().equals(server_hint)) {
                                                                        m_adapterForServerSpinner.clear();
                                                                    }
                                                                    boolean dup = false;
                                                                    for (int i = 0, j = m_serverSpinner.getCount(); i < j; i++)
                                                                    {
                                                                        if (m_adapterForServerSpinner.getItem(i).toString().equals(server)) {
                                                                            dup = true;
                                                                            break;
                                                                        }
                                                                    }
                                                                    if (!dup) {
                                                                        CharSequence textHolder = input.getText().toString().trim();
                                                                        m_adapterForServerSpinner.add(textHolder);
                                                                        m_serverSpinner.setSelection(m_serverSpinner.getCount() - 1);
                                                                        updateServerFile();
                                                                    } else {
                                                                        Toast toast = Toast.makeText(m_context, "Duplicate server name '"
                                                                                + server + "'", Toast.LENGTH_LONG);
                                                                        toast.setGravity(Gravity.CENTER, 0, 0);
                                                                        toast.show();
                                                                    }
                                                                } else {
                                                                    Toast toast = Toast.makeText(m_context, "Invalid server name", Toast.LENGTH_LONG);
                                                                    toast.setGravity(Gravity.CENTER, 0, 0);
                                                                    toast.show();
                                                                }
                                                            }
                                                        }
                                                ).setNegativeButton("Cancel", null).show();
                                            }
                                        }
        );
        removeServerButton.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View v) {
                removeServer();
            }
        });

        BufferedReader rd = null;
        try {
            rd = new BufferedReader(new FileReader(new File(MainActivity.mServersFile)));
            String line;
            while ((line = rd.readLine()) != null) {
                CharSequence textHolder = line;
                m_adapterForServerSpinner.add(textHolder);
            }
        } catch (Exception e) {
        }
        if (rd != null) {
            try {
                rd.close();
            } catch (Exception e) {}
        }
        if (m_adapterForServerSpinner.isEmpty()) {
            CharSequence textHolder = server_hint;
            m_adapterForServerSpinner.add(textHolder);
        }

        m_caseSpinner = (Spinner)m_view.findViewById(R.id.case_spinner);
        m_adapterForCaseSpinner = new ArrayAdapter(m_view.getContext(), android.R.layout.simple_spinner_item);
        m_adapterForCaseSpinner.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        m_caseSpinner.setAdapter(m_adapterForCaseSpinner);
        CharSequence textHolder = case_uninitialized_hint;
        m_adapterForCaseSpinner.add(textHolder);
        m_newCaseButton = (Button)m_view.findViewById(R.id.new_case_button);
        m_newCaseButton.setEnabled(false);
        m_openCaseButton = (Button)m_view.findViewById(R.id.open_case_button);
        m_openCaseButton.setEnabled(false);
        m_juryButton = (Button)m_view.findViewById(R.id.jury_button);
        m_juryButton.setEnabled(false);

        m_newCaseButton.setOnClickListener(new View.OnClickListener()
                                     {
                                         @Override
                                         public void onClick(View v)
                                         {
                                             final EditText input = new EditText(m_context);
                                             
                                             // Show new case input dialog
                                             new AlertDialog.Builder(m_context).
                                                     setTitle("Case name:").
                                                     setView(input).setPositiveButton("OK", new DialogInterface.OnClickListener()
                                                     {
                                                         public void onClick(DialogInterface dialog, int whichButton)
                                                         {
                                                             String caseName = input.getText().toString();
                                                             boolean isEmpty = caseName == null || caseName.trim().isEmpty();
                                                             boolean isValid = false;
                                                             if (!isEmpty) {
                                                                 caseName = caseName.trim();
                                                                 if (!caseName.equals(case_empty_hint)) {
                                                                     isValid = true;
                                                                 }
                                                             }
                                                             if (isValid) {
                                                                 if (m_caseSpinner.getSelectedItem().toString().equals(case_empty_hint)) {
                                                                     m_adapterForCaseSpinner.clear();
                                                                 }
                                                                 boolean dup = false;
                                                                 for (int i = 0, j = m_caseSpinner.getCount(); i < j; i++)
                                                                 {
                                                                     if (m_adapterForCaseSpinner.getItem(i).toString().equals(caseName)) {
                                                                         dup = true;
                                                                         break;
                                                                     }
                                                                 }
                                                                 if (!dup) {
                                                                     if (!MainActivity.isAlphanumeric(caseName))
                                                                     {
                                                                         Toast toast = Toast.makeText(m_context, "Invalid character in case name '"
                                                                                 + caseName + "'", Toast.LENGTH_LONG);
                                                                         toast.setGravity(Gravity.CENTER, 0, 0);
                                                                         toast.show();
                                                                     } else {
                                                                         newCase(caseName);
                                                                     }
                                                                 } else {
                                                                     Toast toast = Toast.makeText(m_context, "Duplicate case name '"
                                                                             + caseName + "'", Toast.LENGTH_LONG);
                                                                     toast.setGravity(Gravity.CENTER, 0, 0);
                                                                     toast.show();
                                                                 }
                                                             } else {
                                                                 Toast toast = Toast.makeText(m_context, "Invalid case name", Toast.LENGTH_LONG);
                                                                 toast.setGravity(Gravity.CENTER, 0, 0);
                                                                 toast.show();
                                                             }
                                                         }
                                                     }
                                             ).setNegativeButton("Cancel", null).show();
                                         }
                                     }
        );

        m_openCaseButton.setOnClickListener(new View.OnClickListener()
        {

            @Override
            public void onClick(View v) {
                if (m_caseSpinner.getCount() > 0) {
                    String caseName = m_caseSpinner.getSelectedItem().toString();
                    if (!caseName.equals(case_empty_hint)) {
                        openCase(caseName);
                    }
                }
            }
        });

        // Jury screen.
        m_juryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (m_caseSpinner.getCount() > 0) {
                    String caseName = m_caseSpinner.getSelectedItem().toString();
                    if (!caseName.equals(case_empty_hint)) {
                        CaseName = caseName;
                        Server = mServer;
                        FragmentManager fm = getFragmentManager();
                        FragmentTransaction fragmentTransaction = fm.beginTransaction();
                        fragmentTransaction.replace(R.id.frameLayout, new Jury());
                        fragmentTransaction.addToBackStack(null);
                        fragmentTransaction.commit();
                    }
                }
            }
        });
    }

    // Get cases from server.
    private void getServerCases() {
        if (m_serverSpinner.getCount() > 0) {
            if (!m_serverSpinner.getSelectedItem().toString().equals(server_hint)) {
                m_caseSpinner = (Spinner)m_view.findViewById(R.id.case_spinner);
                m_adapterForCaseSpinner = new ArrayAdapter(m_view.getContext(), android.R.layout.simple_spinner_item);
                m_adapterForCaseSpinner.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                m_caseSpinner.setAdapter(m_adapterForCaseSpinner);
                m_newCaseButton.setEnabled(false);
                m_openCaseButton.setEnabled(false);
                m_juryButton.setEnabled(false);
                mServer = m_serverSpinner.getSelectedItem().toString();
                String s = mServer + "/EvenTheOdds/rest/service/get_cases";
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
                                        if (response.equals("[]")) {
                                            Toast toast = Toast.makeText(m_context, "No cases on server "
                                                    + mServer, Toast.LENGTH_LONG);
                                            toast.setGravity(Gravity.CENTER, 0, 0);
                                            toast.show();
                                            CharSequence textHolder = case_empty_hint;
                                            m_adapterForCaseSpinner.add(textHolder);
                                            m_openCaseButton.setEnabled(false);
                                            m_juryButton.setEnabled(false);
                                        } else {
                                            String[] cases = response.split("\\[");
                                            cases = cases[1].split("\\]");
                                            cases = cases[0].split(",");
                                            for (String caseName : cases) {
                                                CharSequence textHolder = caseName.trim();
                                                m_adapterForCaseSpinner.add(textHolder);
                                            }
                                            m_openCaseButton.setEnabled(true);
                                            m_juryButton.setEnabled(true);
                                        }
                                        m_newCaseButton.setEnabled(true);
                                    }
                                });
                            } else {
                                handler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast toast;
                                        if (status == -1) {
                                            toast = Toast.makeText(m_context, "Cannot get cases",
                                                    Toast.LENGTH_LONG);
                                        } else {
                                            toast = Toast.makeText(m_context, "Cannot get cases: status="
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
                                    Toast toast = Toast.makeText(m_context, "Cannot get cases: exception=" +
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
    }

    private void removeServer() {
        if (m_serverSpinner.getCount() > 0) {
            if (!m_serverSpinner.getSelectedItem().toString().equals(server_hint)) {
                String selected = m_serverSpinner.getSelectedItem().toString();
                int c = m_serverSpinner.getCount();
                ArrayList<String> servers = new ArrayList<String>();
                for (int i = 0; i < c; i++) {
                    String s = m_serverSpinner.getItemAtPosition(i).toString();
                    if (!s.equals(selected)) {
                        servers.add(s);
                    }
                }
                m_serverSpinner = (Spinner)m_view.findViewById(R.id.server_spinner);
                m_adapterForServerSpinner = new ArrayAdapter(m_view.getContext(), android.R.layout.simple_spinner_item);
                m_adapterForServerSpinner.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                m_serverSpinner.setAdapter(m_adapterForServerSpinner);
                for (String s : servers) {
                    CharSequence textHolder = s;
                    m_adapterForServerSpinner.add(textHolder);
                }
                updateServerFile();
                if (m_serverSpinner.getCount() == 0) {
                    CharSequence textHolder = server_hint;
                    m_adapterForServerSpinner.add(textHolder);
                }
            }
        }
    }

    private void updateServerFile()
    {
        FileWriter writer = null;
        try {
            writer = new FileWriter(MainActivity.mServersFile);
            PrintWriter pr = new PrintWriter(writer);
            int c = m_serverSpinner.getCount();
            if (c > 0) {
                String selected = m_serverSpinner.getSelectedItem().toString();
                pr.write(selected + "\n");
                for (int i = 0; i < c; i++) {
                    String current = m_serverSpinner.getItemAtPosition(i).toString();
                    if (!selected.equals(current)) {
                        pr.write(current + "\n");
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (writer != null) {
            try {
                writer.close();
            } catch (Exception e) {}
        }
    }

    // Open case.
    private void openCase(final String caseName) {
        FileManager caseSelector = new FileManager(m_context, m_dataDirectory, FileManager.SELECT_CASE,
                new FileManager.Listener() {
                    @Override
                    public void onSave(String savedFile) {
                    }

                    @Override
                    public void onSelect(final String selectedDir) {
                        mSelectedFileName = selectedDir;
                        new Thread(new Runnable() {
                            Handler handler = new Handler(Looper.getMainLooper());

                            @RequiresApi(api = Build.VERSION_CODES.O)
                            @Override
                            public void run() {
                                // Download case and unzip to selected directory.
                                String zipFileName = m_tmpDirectory + "/" + caseName + ".zip";
                                File zipFile = new File(zipFileName);
                                if (downloadCase(caseName, zipFile))
                                {
                                    if (unzip(zipFileName, selectedDir)) {
                                        handler.post(new Runnable() {
                                            @Override
                                            public void run() {
                                                Toast toast = Toast.makeText(m_context, "Case name '"
                                                        + caseName + "' downloaded", Toast.LENGTH_LONG);
                                                toast.setGravity(Gravity.CENTER, 0, 0);
                                                toast.show();
                                            }
                                        });
                                    } else {
                                        handler.post(new Runnable() {
                                            @Override
                                            public void run() {
                                                Toast toast = Toast.makeText(m_context, "Cannot download case name '"
                                                        + caseName, Toast.LENGTH_LONG);
                                                toast.setGravity(Gravity.CENTER, 0, 0);
                                                toast.show();
                                            }
                                        });
                                    }
                                    zipFile.delete();
                                }
                            }
                        }).start();
                    }

                    @Override
                    public void onDelete(String deletedFile) {
                    }

                    @Override
                    public void onStorage() {
                    }
                }
        );
        caseSelector.chooseFile_or_Dir();
    }

    // Download case.
    @RequiresApi(api = Build.VERSION_CODES.O)
    private boolean downloadCase(final String caseName, final File downloadZip) {
        String charset = "UTF-8";
        boolean result = false;

        String s = mServer + "/EvenTheOdds/rest/service/get_case_file/" + caseName + ".zip";
        if (!s.startsWith("http"))
        {
            s = "http://" + s;
        }
        String URLname = s;
        Handler handler = new Handler(Looper.getMainLooper());
        HTTPget http = null;
        try {
            http = new HTTPget(URLname);
            final int status = http.get();
            if (status == HttpURLConnection.HTTP_OK) {
                Reader reader = new InputStreamReader(http.httpConn.getInputStream());
                ArrayList<Byte> zipByteList = new ArrayList<Byte>();
                for (int ch = reader.read(); ch != -1; ch = reader.read()) {
                   zipByteList.add((byte)ch);
                }
                byte[] zipBytes = new byte[zipByteList.size()];
                for (int i = 0, j = zipBytes.length; i < j; i++) {
                   zipBytes[i] = zipByteList.get(i);
                }
                byte[] zipBytesDecoded = Base64.getDecoder().decode(zipBytes);
                FileOutputStream outputStream = new FileOutputStream(downloadZip);
                outputStream.write(zipBytesDecoded);
                outputStream.close();
                result = true;
            } else {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        Toast toast;
                        if (status == -1) {
                            toast = Toast.makeText(m_context, "Cannot download case " + caseName,
                                    Toast.LENGTH_LONG);
                        } else {
                            toast = Toast.makeText(m_context, "Cannot download case " + caseName + ": status="
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
                    Toast toast = Toast.makeText(m_context, "Cannot download case " + caseName + ": exception=" +
                            e.getMessage(), Toast.LENGTH_LONG);
                    toast.setGravity(Gravity.CENTER, 0, 0);
                    toast.show();
                }
            });
        } finally {
            if (http != null) {
                http.close();
            }
        }
        return result;
    }

    // Unzip to target directory.
    public boolean unzip(String zipFileName, String targetDirName) {
        ZipInputStream zin = null;
        try {
            FileInputStream fin = new FileInputStream(zipFileName);
            zin = new ZipInputStream(fin);
            ZipEntry ze = null;
            while ((ze = zin.getNextEntry()) != null) {
                if (ze.isDirectory()) {
                    File d = new File(targetDirName + "/" + ze.getName());
                    if (d.exists()) {
                        if (d.isDirectory()) {
                            zin.close();
                            return false;
                        }
                    } else {
                        if (!d.mkdir()) {
                            zin.close();
                            return false;
                        }
                    }
                } else {
                    FileOutputStream fout = new FileOutputStream(targetDirName + "/" + ze.getName());
                    for (int c = zin.read(); c != -1; c = zin.read()) {
                        fout.write(c);
                    }
                    zin.closeEntry();
                    fout.close();
                }
            }
            zin.close();
        } catch (Exception e) {
            if (zin != null) {
                try {
                    zin.close();
                } catch (Exception ex) {}
            }
            return false;
        }
        return true;
    }
    
    // New case.
    private void newCase(final String caseName) {
        FileManager caseAdder = new FileManager(m_context, m_dataDirectory, FileManager.NEW_CASE,
                new FileManager.Listener() {
                    @Override
                    public void onSave(String savedFile) {
                    }

                    @Override
                    public void onSelect(String selectedFile) {
                        mSelectedFileName = selectedFile;
                        new Thread(new Runnable() {
                            Handler handler = new Handler(Looper.getMainLooper());

                            @RequiresApi(api = Build.VERSION_CODES.O)
                            @Override
                            public void run() {
                                // Zip case and upload.
                                String zipFileName = m_tmpDirectory + "/" + caseName + ".zip";
                                if (zip(mSelectedFileName, zipFileName)) {
                                    File zipFile = new File(zipFileName);
                                    if (uploadCase(caseName, zipFile)) {
                                        handler.post(new Runnable() {
                                            @Override
                                            public void run() {
                                                CharSequence textHolder = caseName;
                                                m_adapterForCaseSpinner.add(textHolder);
                                                m_caseSpinner.setSelection(m_caseSpinner.getCount() - 1);
                                                m_openCaseButton.setEnabled(true);
                                                m_juryButton.setEnabled(true);
                                                Toast toast = Toast.makeText(m_context, "Case name '"
                                                        + caseName + "' created", Toast.LENGTH_LONG);
                                                toast.setGravity(Gravity.CENTER, 0, 0);
                                                toast.show();
                                            }
                                        });
                                    } else {
                                        handler.post(new Runnable() {
                                            @Override
                                            public void run() {
                                            if (m_caseSpinner.getCount() == 0) {
                                                CharSequence textHolder = case_empty_hint;
                                                m_adapterForCaseSpinner.add(textHolder);
                                            }
                                            Toast toast = Toast.makeText(m_context, "Cannot upload case name '"
                                                    + caseName + "'", Toast.LENGTH_LONG);
                                            toast.setGravity(Gravity.CENTER, 0, 0);
                                            toast.show();
                                            }
                                        });
                                    }
                                    zipFile.delete();
                                } else {
                                    handler.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            Toast toast = Toast.makeText(m_context, "Cannot zip case name '"
                                                    + caseName + "'", Toast.LENGTH_LONG);
                                            toast.setGravity(Gravity.CENTER, 0, 0);
                                            toast.show();
                                        }
                                    });
                                }
                            }
                        }).start();
                    }

                    @Override
                    public void onDelete(String deletedFile) {
                    }

                    @Override
                    public void onStorage() {
                    }
                }
        );
        caseAdder.chooseFile_or_Dir();
    }

    // Zip directory to file.
    private boolean zip(String sourceDirName, String zipFileName) {
        List<File> fileList = new ArrayList<File>();
        File sourceDir = new File(sourceDirName);
        if (sourceDir.isDirectory()) {
            fileList = getSubFiles(sourceDir);
        } else {
            fileList.add(sourceDir);
            sourceDir = sourceDir.getParentFile();
        }
        ZipOutputStream zout = null;
        try {
            File zipFile = new File(zipFileName);
            zipFile.delete();
            zout = new ZipOutputStream(new FileOutputStream(zipFile));
            int bufferSize = 1024;
            byte[] buf = new byte[bufferSize];
            ZipEntry zipEntry;
            for(int i = 0; i < fileList.size(); i++) {
                File file = fileList.get(i);
                zipEntry = new ZipEntry(sourceDir.toURI().relativize(file.toURI()).getPath());
                zout.putNextEntry(zipEntry);
                if (!file.isDirectory()) {
                    InputStream inputStream = new BufferedInputStream(new FileInputStream(file));
                    int readLength;
                    while ((readLength = inputStream.read(buf, 0, bufferSize)) != -1) {
                        zout.write(buf, 0, readLength);
                    }
                }
            }
        } catch (Exception e) {
            return false;
        } finally {
            if (zout != null) {
                try {
                    zout.close();
                } catch (Exception ex) {
                }
            }
        }
        return true;
    }

    public static List<File> getSubFiles(File baseDir) {
        List<File> fileList = new ArrayList<File>();
        File[] tmpList = baseDir.listFiles();
        for (File file : tmpList) {
            fileList.add(file);
            if (file.isDirectory()) {
                fileList.addAll(getSubFiles(file));
            }
        }
        return fileList;
    }

    // Upload case
    @RequiresApi(api = Build.VERSION_CODES.O)
    private boolean uploadCase(final String caseName, final File uploadZip) {
        String charset = "UTF-8";
        boolean result = false;

        String s = mServer + "/EvenTheOdds/rest/service/put_case_file";
        if (!s.startsWith("http"))
        {
            s = "http://" + s;
        }
        String URLname = s;
        Handler handler = new Handler(Looper.getMainLooper());
        HTTPpost http = null;
        try {
            // Encode to Base64 string.
            byte[] zipFileContent = Files.readAllBytes(uploadZip.toPath());
            String zipString = Base64.getEncoder().encodeToString(zipFileContent);
            try (PrintWriter out = new PrintWriter(uploadZip)) {
                out.print(zipString);
            }

            http = new HTTPpost(URLname, charset, false);
            http.addFilePart("file_name", uploadZip);
            final int status = http.post();
            if (status == HttpURLConnection.HTTP_OK) {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        Toast toast = Toast.makeText(m_context, "Case " + caseName + " uploaded",
                                Toast.LENGTH_LONG);
                        toast.setGravity(Gravity.CENTER, 0, 0);
                        toast.show();
                    }
                });
                result = true;
            } else {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        Toast toast;
                        if (status == -1) {
                            toast = Toast.makeText(m_context, "Cannot upload case " + caseName,
                                    Toast.LENGTH_LONG);
                        } else {
                            toast = Toast.makeText(m_context, "Cannot upload case " + caseName + ": status="
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
                    Toast toast = Toast.makeText(m_context, "Cannot upload case " + caseName + ": exception=" +
                            e.getMessage(), Toast.LENGTH_LONG);
                    toast.setGravity(Gravity.CENTER, 0, 0);
                    toast.show();
                }
            });
        } finally {
            if (http != null) {
                http.close();
            }
        }
        return result;
    }
}
