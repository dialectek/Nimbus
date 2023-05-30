// File manager.

package com.dialectek.nimbus;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.database.DataSetObserver;
import android.graphics.Color;
import android.text.Editable;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class FileManager
{
   public static final int      SAVE_RECORD            = 0;
   public static final int      BROWSE_RECORDS         = 1;
   public static final int      DELETE_RECORD          = 2;
   public static final int      SELECT_CASE            = 3;
   public static final int      NEW_CASE               = 4;
   private int      Select_type     = SAVE_RECORD;
   private String   m_dataDirectory = "";
   private Context  m_context;
   private TextView m_operationView;
   private TextView m_titleView;
   private EditText m_input_text;
   public static String    Default_File_Name  = "";
   private String   Selected_File_Name = Default_File_Name;
   private Button m_searchButton;
   private TextView m_searchView;
   private EditText m_search_input_text;
   private ArrayList<String> m_search_results;
   private int m_search_index;

   private String               m_dir         = "";
   private List<String>         m_subdirs     = null;
   private Listener             m_Listener    = null;

   private List<String>         m_showdirs    = null;
   private ArrayAdapter<String> m_listAdapter = null;
   private final int MAX_SHOW_DIRS            = 3;
   private ArrayList<Integer>   m_showIndex   = null;
   Button m_backButton                        = null;
   Button m_upButton                          = null;
   Button m_downButton                        = null;

   // Callback interface for selected file/directory.
   public interface Listener
   {
      public void onSave(String fileName);
      public void onSelect(String filename);
      public void onDelete(String fileName);
      public void onStorage();
   }

   public FileManager(Context context, String dataDirectory, int select_type, Listener listener)
   {
      Select_type = select_type;

      m_context       = context;
      m_dataDirectory = dataDirectory;
      File datadir = new File(m_dataDirectory);
      if (!datadir.exists()) datadir.mkdir();
      m_Listener      = listener;
   }


   // Load directory chooser dialog for initial default directory.
   public void chooseFile_or_Dir()
   {
      // Initial directory is root directory
      if (m_dir.equals("")) { chooseFile_or_Dir(m_dataDirectory); }
      else{ chooseFile_or_Dir(m_dir); }
   }


   // Load directory chooser dialog for initial input 'dir' directory.
   public void chooseFile_or_Dir(String dir)
   {
      File dirFile = new File(dir);

      if (!dirFile.exists() || !dirFile.isDirectory())
      {
         dir = m_dataDirectory;
      }

      try
      {
         dir = new File(dir).getCanonicalPath();
      }
      catch (IOException ioe)
      {
         return;
      }

      m_dir     = dir;
      m_subdirs = getDirectories(dir);

      class Listener implements DialogInterface.OnClickListener
      {
         public void onClick(DialogInterface dialog, int item)
         {
            String m_dir_old = m_dir;
            String sel       = "" + ((AlertDialog)dialog).getListView().getAdapter().getItem(item);

            if (sel.charAt(sel.length() - 1) == '/') { sel = sel.substring(0, sel.length() - 1); }

            m_dir += "/" + sel;
            if ((new File(m_dir).isFile()))
            {
               m_dir = m_dir_old;
               if (Select_type != SAVE_RECORD) {
                  Selected_File_Name = sel;
               }
            } else {
               if (Select_type != SAVE_RECORD) {
                  Selected_File_Name = "";
               }
               m_showIndex.add(0);
            }

            updateDirectory();
         }
      }

      AlertDialog.Builder dialogBuilder = createOperationDialog(dir, new Listener());

      switch (Select_type)
      {
         case SAVE_RECORD:
            dialogBuilder.setPositiveButton("Save", new OnClickListener()
                    {
                       @Override
                       public void onClick(DialogInterface dialog, int which)
                       {
                          Selected_File_Name = m_input_text.getText() + "";
                          String toFile = m_dir + "/" + Selected_File_Name;
                          String displayFile = toFile.replace(m_dataDirectory, "");
                          if (Selected_File_Name.isEmpty() || !(new File(MainActivity.mRecordingFile).exists()))
                          {
                             Toast toast = Toast.makeText(m_context, "No recording", Toast.LENGTH_LONG);
                             toast.setGravity(Gravity.CENTER, 0, 0);
                             toast.show();
                          } else if (new File(toFile).exists()) {
                             Toast toast = Toast.makeText(m_context, "Cannot overwrite existing file " + displayFile, Toast.LENGTH_LONG);
                             toast.setGravity(Gravity.CENTER, 0, 0);
                             toast.show();
                          } else if (!MainActivity.copyFile(MainActivity.mRecordingFile, toFile)) {
                             Toast toast = Toast.makeText(m_context, "Cannot copy recording " + displayFile, Toast.LENGTH_LONG);
                             toast.setGravity(Gravity.CENTER, 0, 0);
                             toast.show();
                          } else {
                             if (m_Listener != null)
                             {
                                m_Listener.onSave(toFile);
                             }
                          }
                       }
                    }
            ).setNegativeButton("Cancel", null);
         break;

         case BROWSE_RECORDS:
            dialogBuilder.setPositiveButton("Select", new OnClickListener()
                    {
                       @Override
                       public void onClick(DialogInterface dialog, int which)
                       {
                          Selected_File_Name = m_input_text.getText() + "";
                          String selectedFile = m_dir + "/" + Selected_File_Name;
                          if (new File(selectedFile).exists())
                          {
                             if (m_Listener != null)
                             {
                                m_Listener.onSelect(selectedFile);
                             }
                          } else {
                             String displayFile = selectedFile.replace(m_dataDirectory, "");
                             Toast toast = Toast.makeText(m_context, displayFile + "  does not exist", Toast.LENGTH_LONG);
                             toast.setGravity(Gravity.CENTER, 0, 0);
                             toast.show();
                          }
                       }
                    }
            );
            dialogBuilder.setNeutralButton("Storage", new OnClickListener() {
                       @Override
                       public void onClick(DialogInterface dialog, int which) {
                          if (m_Listener != null) {
                             m_Listener.onStorage();
                          }
                       }
                    }
            );
            dialogBuilder.setNegativeButton("Cancel", null);
            break;

         case DELETE_RECORD:
            dialogBuilder.setPositiveButton("Delete", new OnClickListener()
                    {
                       @Override
                       public void onClick(DialogInterface dialog, int which)
                       {
                          Selected_File_Name = m_input_text.getText() + "";
                          String deleteFile = m_dir + "/" + Selected_File_Name;
                          File delFile = new File(deleteFile);
                          String displayFile = deleteFile.replace(m_dataDirectory, "");
                          if ((m_dataDirectory + "/").equals(deleteFile))
                          {
                             Toast toast = Toast.makeText(m_context, "Cannot delete root folder", Toast.LENGTH_LONG);
                             toast.setGravity(Gravity.CENTER, 0, 0);
                             toast.show();
                          } else if (!delFile.exists())
                          {
                             Toast toast = Toast.makeText(m_context, displayFile + " does not exist", Toast.LENGTH_LONG);
                             toast.setGravity(Gravity.CENTER, 0, 0);
                             toast.show();
                          } else if (delFile.isDirectory() && delFile.listFiles().length > 0)
                          {
                             Toast toast = Toast.makeText(m_context, displayFile + " must be empty", Toast.LENGTH_LONG);
                             toast.setGravity(Gravity.CENTER, 0, 0);
                             toast.show();
                          } else if (delFile.delete())
                          {
                             if (m_Listener != null)
                             {
                                m_Listener.onDelete(deleteFile);
                             }
                          } else {
                             Toast toast = Toast.makeText(m_context, "Cannot delete " + displayFile, Toast.LENGTH_LONG);
                             toast.setGravity(Gravity.CENTER, 0, 0);
                             toast.show();
                          }
                       }
                    }
            ).setNegativeButton("Cancel", null);
            break;

         case SELECT_CASE:
            dialogBuilder.setPositiveButton("Select", new OnClickListener()
                    {
                       @Override
                       public void onClick(DialogInterface dialog, int which)
                       {
                          Selected_File_Name = "";
                          String toDir = m_dir + "/";
                          if (m_Listener != null)
                          {
                             m_Listener.onSelect(toDir);
                          }
                       }
                    }
            ).setNegativeButton("Cancel", null);
            break;

         case NEW_CASE:
            dialogBuilder.setPositiveButton("Select", new OnClickListener()
                    {
                       @Override
                       public void onClick(DialogInterface dialog, int which)
                       {
                          Selected_File_Name = m_input_text.getText() + "";
                          String selectedFile = m_dir + "/" + Selected_File_Name;
                          if (m_Listener != null)
                          {
                             m_Listener.onSelect(selectedFile);
                          }
                       }
                    }
            ).setNegativeButton("Cancel", null);
            break;
      }

      final AlertDialog operationDialog = dialogBuilder.create();
      operationDialog.setOnShowListener(new DialogInterface.OnShowListener() {

         @Override
         public void onShow(DialogInterface alert) {
            ListView listView = ((AlertDialog)alert).getListView();
            final ListAdapter originalAdapter = listView.getAdapter();

            listView.setAdapter(new ListAdapter()
            {
               @Override
               public int getCount() {
                  return originalAdapter.getCount();
               }

               @Override
               public Object getItem(int id) {
                  return originalAdapter.getItem(id);
               }

               @Override
               public long getItemId(int id) {
                  return originalAdapter.getItemId(id);
               }

               @Override
               public int getItemViewType(int id) {
                  return originalAdapter.getItemViewType(id);
               }

               @Override
               public View getView(int position, View convertView, ViewGroup parent) {
                  View view = originalAdapter.getView(position, convertView, parent);
                  TextView textView = (TextView)view;
                  textView.setTextColor(Color.BLACK);
                  textView.setTextSize(14);
                  return view;
               }

               @Override
               public int getViewTypeCount() {
                  return originalAdapter.getViewTypeCount();
               }

               @Override
               public boolean hasStableIds() {
                  return originalAdapter.hasStableIds();
               }

               @Override
               public boolean isEmpty() {
                  return originalAdapter.isEmpty();
               }
               @Override
               public void registerDataSetObserver(DataSetObserver observer) {
                  originalAdapter.registerDataSetObserver(observer);
               }

               @Override
               public void unregisterDataSetObserver(DataSetObserver observer) {
                  originalAdapter.unregisterDataSetObserver(observer);
               }

               @Override
               public boolean areAllItemsEnabled() {
                  return originalAdapter.areAllItemsEnabled();
               }

               @Override
               public boolean isEnabled(int position) {
                  return originalAdapter.isEnabled(position);
               }

            });
         }
      });

      operationDialog.show();
   }

   private List<String> getDirectories(String dir)
   {
      List<String> dirs = new ArrayList<String>();
      try
      {
         File dirFile = new File(dir);

         if (!dirFile.exists() || !dirFile.isDirectory())
         {
            return(dirs);
         }

         for (File file : dirFile.listFiles())
         {
            if (file.isDirectory())
            {
               // Add "/" to directory names to identify them in the list.
               dirs.add(file.getName() + "/");
            }
            else
            {
               dirs.add(file.getName());
            }
         }
      }
      catch (Exception e) {}

      Collections.sort(dirs, new Comparator<String>()
                       {
                          public int compare(String o1, String o2)
                          {
                             return(o1.compareTo(o2));
                          }
                       }
                       );
      return(dirs);
   }

   // Dialog definition.
   private AlertDialog.Builder createOperationDialog(String title,
                                                            DialogInterface.OnClickListener onClickListener)
   {
      AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(m_context);

      m_operationView = new TextView(m_context);
      m_operationView.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

      switch(Select_type) {
         case SAVE_RECORD:
            m_operationView.setText("Save:");
            break;
         case BROWSE_RECORDS:
            m_operationView.setText("Browse:");
            break;
         case DELETE_RECORD:
            m_operationView.setText("Delete:");
            break;
         case SELECT_CASE:
            m_operationView.setText("Select:");
            break;
         case NEW_CASE:
            m_operationView.setText("Select:");
            break;
      }

      m_operationView.setGravity(Gravity.CENTER_VERTICAL);
      m_operationView.setBackgroundColor(-12303292);
      m_operationView.setTextColor(m_context.getResources().getColor(android.R.color.white));
      m_operationView.setTextSize(18);

      LinearLayout operationLayout = new LinearLayout(m_context);
      operationLayout.setOrientation(LinearLayout.VERTICAL);
      operationLayout.addView(m_operationView);

      if (Select_type == SAVE_RECORD || Select_type == SELECT_CASE)
      {
         // Create New Folder button.
         Button newDirButton = new Button(m_context);
         newDirButton.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
         newDirButton.setText("New Folder");
         newDirButton.setOnClickListener(new View.OnClickListener()
                                         {
                                            @Override
                                            public void onClick(View v)
                                            {
                                               final EditText input = new EditText(m_context);

                                               // Show new folder name input dialog
                                               new AlertDialog.Builder(m_context).
                                                  setTitle("Folder name:").
                                                  setView(input).setPositiveButton("Create", new DialogInterface.OnClickListener()
                                                                                   {
                                                                                      public void onClick(DialogInterface dialog, int whichButton)
                                                                                      {
                                                                                         Editable newDir = input.getText();
                                                                                         String newDirName = newDir.toString();
                                                                                         if (!newDirName.isEmpty() && MainActivity.isAlphanumeric(newDirName)) {
                                                                                            String newDirPath = m_dir + "/" + newDirName;
                                                                                            File newDirFile = new File(newDirPath);
                                                                                            if (newDirFile.exists()) {
                                                                                               Toast toast = Toast.makeText(m_context,
                                                                                                       newDirName + " exists", Toast.LENGTH_LONG);
                                                                                               toast.setGravity(Gravity.CENTER, 0, 0);
                                                                                               toast.show();
                                                                                            } else if (newDirFile.mkdir()) {
                                                                                               // Navigate into the new directory.
                                                                                               m_dir += "/" + newDirName;
                                                                                               m_showIndex.add(0);
                                                                                               updateDirectory();
                                                                                            } else {
                                                                                               Toast toast = Toast.makeText(m_context, "Cannot create '"
                                                                                                       + newDirName + "' folder", Toast.LENGTH_LONG);
                                                                                               toast.setGravity(Gravity.CENTER, 0, 0);
                                                                                               toast.show();
                                                                                            }
                                                                                         } else {
                                                                                            Toast toast = Toast.makeText(m_context, "Invalid folder name '"
                                                                                                    + newDirName + "'", Toast.LENGTH_LONG);
                                                                                            toast.setGravity(Gravity.CENTER, 0, 0);
                                                                                            toast.show();
                                                                                         }
                                                                                      }
                                                                                   }
                                                                                   ).setNegativeButton("Cancel", null).show();
                                            }
                                         }
                                         );
         operationLayout.addView(newDirButton);
      }

      if (Select_type == BROWSE_RECORDS || Select_type == NEW_CASE)
      {
         // Create Search button.
         Button searchButton = new Button(m_context);
         searchButton.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
         searchButton.setText("Search");
         searchButton.setOnClickListener(new View.OnClickListener()
                                         {
                                            @Override
                                            public void onClick(View v)
                                            {

                                               // Show search dialog
                                               createSearchDialog().
                                                       setPositiveButton("Select", new DialogInterface.OnClickListener()
                                                       {
                                                          public void onClick(DialogInterface dialog, int whichButton) {
                                                             if (m_search_results != null) {
                                                                m_dir = m_search_results.get(m_search_index);
                                                                File dirfile = new File(m_dir);
                                                                if ((new File(m_dir).isFile())) {
                                                                   Selected_File_Name = dirfile.getName();
                                                                   m_dir = dirfile.getParent();
                                                                } else {
                                                                   Selected_File_Name = "";
                                                                }
                                                                int c = m_dir.replace(m_dataDirectory, "").split("/").length;
                                                                m_showIndex.clear();
                                                                for (int i = 0; i < c; i++)
                                                                {
                                                                   m_showIndex.add(0);
                                                                }
                                                                updateDirectory();
                                                             } else {
                                                                Toast toast = Toast.makeText(m_context, "No search results", Toast.LENGTH_LONG);
                                                                toast.setGravity(Gravity.CENTER, 0, 0);
                                                                toast.show();
                                                             }
                                                          }
                                                       }
                                               ).setNegativeButton("Cancel", null).show();
                                            }
                                         }
         );
         operationLayout.addView(searchButton);
      }

      // Create navigation buttons.
      LinearLayout navigationLayout = new LinearLayout(m_context);
      navigationLayout.setOrientation(LinearLayout.HORIZONTAL);
      m_backButton = new Button(m_context);
      m_backButton.setEnabled(false);
      LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT, 1f);
      m_backButton.setLayoutParams(params);
      m_backButton.setText("Back");
      m_backButton.setOnClickListener(new View.OnClickListener()
                                      {
                                         @Override
                                         public void onClick(View v)
                                         {
                                            if (!m_dir.equals(m_dataDirectory))
                                            {
                                               m_dir = m_dir.substring(0, m_dir.lastIndexOf("/"));
                                               int i = m_showIndex.size() - 1;
                                               m_showIndex.remove(i);
                                               if (Select_type != SAVE_RECORD)
                                               {
                                                  Selected_File_Name = "";
                                               }
                                               updateDirectory();
                                            }
                                         }
                                      }
      );
      navigationLayout.addView(m_backButton);
      m_upButton = new Button(m_context);
      m_upButton.setEnabled(false);
      m_upButton.setLayoutParams(params);
      m_upButton.setText("Up");
      m_upButton.setOnClickListener(new View.OnClickListener()
                                    {
                                       @Override
                                       public void onClick(View v)
                                       {
                                          int i = m_showIndex.size() - 1;
                                          if (m_showIndex.get(i) > 0)
                                          {
                                             m_showIndex.set(i, m_showIndex.get(i) - 1);
                                             updateDirectory();
                                          }
                                       }
                                    }
      );
      navigationLayout.addView(m_upButton);
      m_downButton = new Button(m_context);
      m_downButton.setEnabled(false);
      m_downButton.setLayoutParams(params);
      m_downButton.setText("Down");
      m_downButton.setOnClickListener(new View.OnClickListener()
                                  {
                                     @Override
                                     public void onClick(View v)
                                     {
                                        int i = m_showIndex.size() - 1;
                                        if ((MAX_SHOW_DIRS * (m_showIndex.get(i) + 1)) < m_subdirs.size())
                                        {
                                           m_showIndex.set(i, m_showIndex.get(i) + 1);
                                           updateDirectory();
                                        }
                                     }
                                  }
      );
      navigationLayout.addView(m_downButton);
      operationLayout.addView(navigationLayout);

      // Create view with folder path and entry text box.
      LinearLayout titleLayout = new LinearLayout(m_context);
      titleLayout.setOrientation(LinearLayout.VERTICAL);
      m_titleView = new TextView(m_context);
      m_titleView.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
      m_titleView.setBackgroundColor(-12303292);           // dark gray -12303292
      m_titleView.setTextColor(m_context.getResources().getColor(android.R.color.white));
      m_titleView.setTextSize(18);
      m_titleView.setGravity(Gravity.CENTER_VERTICAL);
      m_titleView.setText(title);
      titleLayout.addView(m_titleView);

      // Add input text.
      m_input_text = new EditText(m_context);
      m_input_text.setText(Default_File_Name);
      if (Select_type != SAVE_RECORD) {
         m_input_text.setInputType(InputType.TYPE_NULL);
         m_input_text.setFocusable(false);
         m_input_text.setFocusableInTouchMode(false);
         m_input_text.setOnClickListener(new View.OnClickListener() {
                                                   @Override
                                                   public void onClick(View v) {
                                                      m_input_text.setText("");
                                                   }
                                                }
         );
      } else {
         if (Default_File_Name.isEmpty())
         {
            m_input_text.setEnabled(false);
         }
      }
      if (Select_type != SELECT_CASE) {
         titleLayout.addView(m_input_text);
      }

      // Set views and finish dialog builder.
      dialogBuilder.setView(titleLayout);
      dialogBuilder.setCustomTitle(operationLayout);
      m_showIndex = new ArrayList<Integer>();
      m_showIndex.add(0);
      m_showdirs = new ArrayList<String>();
      m_listAdapter = createListAdapter(m_showdirs);
      updateDirectory();
      dialogBuilder.setSingleChoiceItems(m_listAdapter, -1, onClickListener);
      dialogBuilder.setCancelable(false);
      return(dialogBuilder);
   }

   // Search dialog definition.
   private AlertDialog.Builder createSearchDialog()
   {
      AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(m_context);
      LinearLayout searchLayout = new LinearLayout(m_context);
      searchLayout.setOrientation(LinearLayout.VERTICAL);
      m_search_results = null;
      m_searchButton = new Button(m_context);
      m_searchButton.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
      m_searchButton.setText("Run");
      m_searchButton.setOnClickListener(new View.OnClickListener()
                                      {
                                         @Override
                                         public void onClick(View v)
                                         {
                                            if (m_search_results == null)
                                            {
                                               m_search_results = searchPaths(m_dataDirectory,m_search_input_text.getText() + "");
                                               m_search_index = 0;
                                               m_search_input_text.setEnabled(false);
                                               if (m_search_results == null)
                                               {
                                                  m_searchButton.setEnabled(false);
                                                  m_searchView.setText("No match");
                                               } else {
                                                  if (m_search_index == m_search_results.size() - 1)
                                                  {
                                                     m_searchButton.setEnabled(false);
                                                  } else {
                                                     m_searchButton.setText("Next");
                                                  }
                                                  String path = m_search_results.get(0);
                                                  m_searchView.setText(" (" + (m_search_index + 1) + "/" + m_search_results.size() + ") " +
                                                                  path.replace(m_dataDirectory, ""));
                                               }
                                            } else {
                                               if (m_search_index < m_search_results.size() - 1) {
                                                  m_search_index++;
                                                  if (m_search_index == m_search_results.size() - 1)
                                                  {
                                                     m_searchButton.setEnabled(false);
                                                  }
                                                  String path = m_search_results.get(m_search_index);
                                                  m_searchView.setText(" (" + (m_search_index + 1) + "/" + m_search_results.size() + ") " +
                                                          path.replace(m_dataDirectory, ""));
                                               }
                                            }
                                         }
                                      }
      );
      searchLayout.addView(m_searchButton);
      LinearLayout titleLayout = new LinearLayout(m_context);
      titleLayout.setOrientation(LinearLayout.VERTICAL);
      m_searchView = new TextView(m_context);
      m_searchView.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
      m_searchView.setBackgroundColor(-12303292);           // dark gray -12303292
      m_searchView.setTextColor(m_context.getResources().getColor(android.R.color.white));
      m_searchView.setTextSize(18);
      m_searchView.setGravity(Gravity.CENTER_VERTICAL);
      m_searchView.setText("File or folder name:");
      titleLayout.addView(m_searchView);
      m_search_input_text = new EditText(m_context);
      titleLayout.addView(m_search_input_text);

      dialogBuilder.setView(titleLayout);
      dialogBuilder.setCustomTitle(searchLayout);
      return(dialogBuilder);
   }

   private void updateDirectory()
   {
      m_subdirs.clear();
      m_subdirs.addAll(getDirectories(m_dir));
      if (Select_type == SELECT_CASE) {
         ArrayList<String> tmpdirs = new ArrayList<String>();
         for (String d : m_subdirs) {
            File df = new File(m_dir + "/" + d);
            if (df.exists() && df.isDirectory()) {
               tmpdirs.add(d);
            }
         }
         m_subdirs = tmpdirs;
      }
      m_titleView.setText(m_dir.replace(m_dataDirectory, "") + "/");
      m_showdirs.clear();
      int index = m_showIndex.size() - 1;
      int i = MAX_SHOW_DIRS * m_showIndex.get(index);
      for (int j = 0; j < MAX_SHOW_DIRS && i < m_subdirs.size(); j++, i++)
      {
         m_showdirs.add(m_subdirs.get(i));
      }
      m_listAdapter.notifyDataSetChanged();
      m_input_text.setText(Selected_File_Name);
      if (m_dir.equals(m_dataDirectory))
      {
         m_backButton.setEnabled(false);
      } else {
         m_backButton.setEnabled(true);
      }
      if (m_showIndex.get(index) > 0)
      {
         m_upButton.setEnabled(true);
      } else {
         m_upButton.setEnabled(false);
      }
      if ((MAX_SHOW_DIRS * (m_showIndex.get(index) + 1)) < m_subdirs.size())
      {
         m_downButton.setEnabled(true);
      } else {
         m_downButton.setEnabled(false);
      }
   }


   private ArrayAdapter<String> createListAdapter(List<String> items)
   {
      return(new ArrayAdapter<String>(m_context, android.R.layout.select_dialog_item, android.R.id.text1, items)
             {
                @Override
                public View getView(int position, View convertView, ViewGroup parent)
                {
                   View v = super.getView(position, convertView, parent);

                   if (v instanceof TextView)
                   {
                      TextView tv = (TextView)v;
                      tv.getLayoutParams().height = LayoutParams.WRAP_CONTENT;
                      tv.setEllipsize(null);
                   }
                   return v;
                }
             }
             );
   }

   // Search for folders and files that match given expression.
   public ArrayList<String> searchPaths(String dir, String expr) {
      if (expr.isEmpty())
      {
         return null;
      }
      ArrayList<String> results = new ArrayList<String>();
      File[] files = new File(dir).listFiles();
      for(File f:files) {
         String name = f.getName();
         boolean match = false;
         try {
            match = name.matches(expr);
         } catch (Exception e)
         {
            Toast toast = Toast.makeText(m_context, "Invalid folder or file name", Toast.LENGTH_LONG);
            toast.setGravity(Gravity.CENTER, 0, 0);
            toast.show();
            return null;
         }
         if(match) {
            try {
               results.add(f.getCanonicalPath());
            } catch (Exception e) {
               Toast toast = Toast.makeText(m_context, "Cannot get path", Toast.LENGTH_LONG);
               toast.setGravity(Gravity.CENTER, 0, 0);
               toast.show();
               return null;
            }
         }
         if (f.isDirectory()) {
            ArrayList<String> subResults = searchPaths(f.getPath(), expr);
            if (subResults != null) {
               for (String path : subResults)
               {
                  results.add(path);
               }
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
}
