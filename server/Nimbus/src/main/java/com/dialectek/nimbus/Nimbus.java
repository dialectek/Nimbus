// Nimbus server.

package com.dialectek.nimbus;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

public class Nimbus
{
   public static final String appDirectory = "nimbus";
   
   // Constructor.
   public Nimbus()
   {
	   File dir = new File(appDirectory);
	   if (!dir.exists())
	   {
		   dir.mkdir();
	   }
   }

   // Get case names.
   public synchronized ArrayList<String> getCaseNames()
   {
 	  File dir = new File(appDirectory);
 	  File[] filesList = dir.listFiles();
 	  ArrayList<String> caseNames = new ArrayList<String>();
 	  for (File file : filesList) 
 	  {
 	     if (file.isDirectory()) 
 	     {
 	    	 caseNames.add(file.getName()); 
 	     }
 	  }	  
      return(caseNames);
   }
   
   // Get case file path name.
   public synchronized String getPathName(String fileName)
   {
	  String[] tokens = fileName.split("\\.(?=[^\\.]+$)");
	  if (tokens.length == 2)
	  {
		  return appDirectory + "/" + tokens[0] + "/" + fileName;
	  } else {
		  return appDirectory + "/" + fileName + "/" + fileName;		  
	  }
   }
   
   // Make parent directory.
   public synchronized boolean mkParentDir(String fileName)
   {
	  String[] tokens = fileName.split("\\.(?=[^\\.]+$)");
	  File dir;
	  if (tokens.length == 2)
	  {
		  dir = new File(appDirectory + "/" + tokens[0]);
	  } else {
		  dir = new File(appDirectory + "/" + fileName);		  
	  }
	  if (!dir.exists())
	  {
		  return dir.mkdir();
	  } else {	  
		  return true;
	  }
   } 
   
   // Put case file.
   public synchronized boolean putCaseFile(String fileName, InputStream fileStream)
   {
	  mkParentDir(fileName);
	  String pathName = getPathName(fileName);	  
	  File file = new File(pathName);  
	  if (fileStream != null)
	  {
		  try 
		  {
	          Reader reader = new InputStreamReader(fileStream);
              ArrayList<Byte> zipByteList = new ArrayList<Byte>();
              for (int ch = reader.read(); ch != -1; ch = reader.read()) {
                 zipByteList.add((byte)ch);
              }
              byte[] zipBytes = new byte[zipByteList.size()];
              for (int i = 0, j = zipBytes.length; i < j; i++) {
                 zipBytes[i] = zipByteList.get(i);
              }
              byte[] zipBytesDecoded = Base64.getDecoder().decode(zipBytes);
              FileOutputStream outputStream = new FileOutputStream(file);
              outputStream.write(zipBytesDecoded);
              outputStream.close();	          
		  } catch (Exception e)
		  {
			  return false;
		  }		  
		  return true;
	  } else { 
		  return false;
	  }
   }
   
   // Get jury.
   public synchronized List<String> getJury(String caseName)
   {
	  String juryFilename = appDirectory + "/" + caseName + "/jury.txt";
 	  List<String> jurorEntries = new ArrayList<String>();
 	  try 
 	  {
 		  jurorEntries = Files.readAllLines(Paths.get(juryFilename));
 	  } catch (IOException e) {}
      return(jurorEntries);
   }
   
   // Add juror.
   public synchronized boolean addJuror(String caseName, String jurorName, String jurorStatus)
   {
	  String caseFilename = appDirectory + "/" + caseName;
	  if (!new File(caseFilename).exists()) 
	  {
		  return false;
	  }
 	  List<String> jurorEntries = new ArrayList<String>();
 	  try 
 	  {
 		  String juryFilename = caseFilename + "/jury.txt";
 		  File juryFile = new File(juryFilename);
 		  if (!juryFile.exists()) 
 		  {
 			  juryFile.createNewFile();
 		  }	  	   		  
 		  jurorEntries = Files.readAllLines(Paths.get(juryFilename));
 		  for (String jurorEntry : jurorEntries)
 		  {
 			  String[] parts = jurorEntry.split("/");
 			  if (parts != null && parts.length > 0)
 			  {
 				  if (jurorName.equals(parts[0]))
 				  {
 					  return false;
 				  }
 			  }
 		  }
 		  File file = new File(juryFilename);
 		  FileWriter fr = new FileWriter(file, true);
 		  BufferedWriter br = new BufferedWriter(fr);
 		  PrintWriter pr = new PrintWriter(br);
 		  pr.println(jurorName + "/" + jurorStatus);
 		  pr.close();
 		  br.close();		  
 		  fr.close(); 		  
 	  } catch (IOException e) 
 	  {
 		  return false;
 	  }
 	  return true;
   }
   
   // Update juror status.
   public synchronized boolean updateJurorStatus(String caseName, String jurorName, String jurorStatus)
   {
	  String juryFilename = appDirectory + "/" + caseName + "/jury.txt";
 	  List<String> jurorEntries = new ArrayList<String>();
 	  try 
 	  {
 		  jurorEntries = Files.readAllLines(Paths.get(juryFilename));
 		  for (int i = 0, j = jurorEntries.size(); i < j; i++)
 		  {
 			  String jurorEntry = jurorEntries.get(i);
 			  String[] parts = jurorEntry.split("/");
 			  if (parts != null && parts.length > 0)
 			  {
 				  if (jurorName.equals(parts[0]))
 				  {
 					  jurorEntries.set(i, jurorName + "/" + jurorStatus);
 			 		  File file = new File(juryFilename);
 			 		  FileWriter fr = new FileWriter(file, false);
 			 		  BufferedWriter br = new BufferedWriter(fr);
 			 		  PrintWriter pr = new PrintWriter(br);
 			 		  for (String entry : jurorEntries)
 			 		  {
 			 			  pr.println(entry);
 			 		  }
 			 		  pr.close();
 			 		  br.close();		  
 			 		  fr.close(); 		  
 					  return true;
 				  }
 			  }
 		  } 		  
 	  } catch (IOException e) {}
 	  return false;
   }
   
   // Remove juror.
   public synchronized boolean removeJuror(String caseName, String jurorName)
   {
	  String juryFilename = appDirectory + "/" + caseName + "/jury.txt";
 	  List<String> jurorEntries = new ArrayList<String>();
 	  try 
 	  {
 		  jurorEntries = Files.readAllLines(Paths.get(juryFilename));
 		  for (int i = 0, j = jurorEntries.size(); i < j; i++)
 		  {
 			  String jurorEntry = jurorEntries.get(i);
 			  String[] parts = jurorEntry.split("/");
 			  if (parts != null && parts.length > 0)
 			  {
 				  if (jurorName.equals(parts[0]))
 				  {
 					  jurorEntries.remove(i);
 			 		  File file = new File(juryFilename);
 			 		  FileWriter fr = new FileWriter(file, false);
 			 		  BufferedWriter br = new BufferedWriter(fr);
 			 		  PrintWriter pr = new PrintWriter(br);
 			 		  for (String entry : jurorEntries)
 			 		  {
 			 			  pr.println(entry);
 			 		  }
 			 		  pr.close();
 			 		  br.close();		  
 			 		  fr.close(); 		  
 					  return true;
 				  }
 			  }
 		  } 		  
 	  } catch (IOException e) {}
 	  return false;
   }        
}
