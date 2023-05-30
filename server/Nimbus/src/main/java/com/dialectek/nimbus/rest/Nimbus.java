// Nimbus server REST interface.

package com.dialectek.nimbus.rest;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;

import org.jboss.resteasy.plugins.providers.multipart.InputPart;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataInput;

@Path("/service")
public class Nimbus
{
   // Nimbus.
   public static com.dialectek.nimbus.Nimbus nimbusApp;

   // Synchronization.
   private static Object lock;

   // Initialize.
   static
   {
      nimbusApp  = new com.dialectek.nimbus.Nimbus();
      lock = new Object();
   }

   // Get cases.
   @GET
   @Path("/get_cases")
   @Produces(MediaType.TEXT_PLAIN)
   public Response get_files()
   {
      synchronized (lock)
      {
    	 String output = "[";
    	 ArrayList<String> dirNames = nimbusApp.getCaseNames();
    	 for (int i = 0, j = dirNames.size(); i < j; i++) 
    	 {
	         output += dirNames.get(i);
	         if (i < j - 1)
	         {
    			 output += ",";    	        	 
	         }
    	 }    	 
    	 output += "]";
         return(Response.status(200).entity(output).build());
      }
   }
   
   // Get case file.
   @GET
   @Path("/get_case_file/{file_name}")
   @Produces(MediaType.APPLICATION_OCTET_STREAM)
   public Response get_case_file(@PathParam ("file_name") String fileName) throws WebApplicationException 
   {
	   synchronized (lock)
	   {
	       String pathName = nimbusApp.getPathName(fileName);
	       File file = new File(pathName);
		   if (file.exists()) 
		   {	
		       StreamingOutput fileStream =  new StreamingOutput() 
		       {
		           @Override
		           public void write(OutputStream outputStream) throws IOException, WebApplicationException 
		           {
		               try
		               {
		                   java.nio.file.Path path = Paths.get(pathName);
		                   byte[] data = Files.readAllBytes(path);
		                   String dataString = Base64.getEncoder().encodeToString(data);
		                   try (PrintWriter writer = new PrintWriter(outputStream)) {
		                       writer.print(dataString);
		                   }		                   
		               } 
		               catch (Exception e) 
		               {
		                   throw new WebApplicationException(400);
		               }
		           }
		       };
		       return Response
		               .ok(fileStream, MediaType.APPLICATION_OCTET_STREAM)
		               .header("content-disposition","attachment; filename=" + fileName)
		               .build();
		   } else {
			  return(Response.status(404).build()); 
		   }		   
	   }
   }
   
   @POST   
   @Path("/put_case_file")
   @Consumes(MediaType.MULTIPART_FORM_DATA)
   @Produces(MediaType.TEXT_PLAIN)   
   public Response put_case_file(MultipartFormDataInput input) throws WebApplicationException
   {
       synchronized (lock)
       {	   
		   String fileName = null;
		   Map<String, List<InputPart>> formParts = input.getFormDataMap();
		   List<InputPart> inPart = formParts.get("file_name"); 
		   for (InputPart inputPart : inPart) 
		   {
		       MultivaluedMap<String, String> headers = inputPart.getHeaders();
		       String[] contentDispositionHeader = headers.getFirst("Content-Disposition").split(";");
		       for (String name : contentDispositionHeader) 
		       {
		         if ((name.trim().startsWith("filename"))) 
		         {
		           String[] tmp = name.split("=");
		           fileName = tmp[1].trim().replaceAll("\"","");
		           break;
		         }
		       }
		   }
		   if (fileName != null)
		   {
		       try
		       {
		          InputStream fileStream = input.getFormDataPart("file_name", InputStream.class, null);	
		     	  if (nimbusApp.putCaseFile(fileName, fileStream))
		     	  {
		 			  return(Response.status(200).build());
		 		  } else { 
		 			  return(Response.status(400).build());
		 		  }
		       } 
		       catch (Exception e) 
		       {
		           throw new WebApplicationException(400);
		       } 
		   } else {
	           throw new WebApplicationException(400);		   
		   }
   		}
   }

   // Get case jury.
   @GET
   @Path("/get_jury/{case_name}")
   @Produces(MediaType.TEXT_PLAIN)
   public Response get_jury(@PathParam ("case_name") String caseName)
   {
      synchronized (lock)
      {
    	 String output = "[";
    	 List<String> juryInfo = nimbusApp.getJury(caseName);
    	 for (int i = 0, j = juryInfo.size(); i < j; i++) 
    	 {
	         output += juryInfo.get(i);
	         if (i < j - 1)
	         {
    			 output += ",";    	        	 
	         }
    	 }    	 
    	 output += "]";
         return(Response.status(200).entity(output).build());
      }
   }
   
   // Add juror.
   @GET
   @Path("/add_juror/{case_name}/{juror_name}/{juror_status}")
   @Produces(MediaType.TEXT_PLAIN)
   public Response add_juror(@PathParam ("case_name") String caseName,
		   @PathParam ("juror_name") String jurorName,
		   @PathParam ("juror_status") String jurorStatus)   
   {
      synchronized (lock)
      {
    	 if (nimbusApp.addJuror(caseName, jurorName, jurorStatus))
    	  {
			  return(Response.status(200).build());
		  } else { 
			  return(Response.status(400).build());
		  }
      }
   }
   
   // Update juror status.
   @GET
   @Path("/update_juror_status/{case_name}/{juror_name}/{juror_status}")
   @Produces(MediaType.TEXT_PLAIN)
   public Response update_juror_status(@PathParam ("case_name") String caseName,
		   @PathParam ("juror_name") String jurorName,
		   @PathParam ("juror_status") String jurorStatus)   
   {
      synchronized (lock)
      {
    	 if (nimbusApp.updateJurorStatus(caseName, jurorName, jurorStatus))
    	  {
			  return(Response.status(200).build());
		  } else { 
			  return(Response.status(404).build());
		  }
      }
   } 
   
   // Remove juror.
   @GET
   @Path("/remove_juror/{case_name}/{juror_name}")
   @Produces(MediaType.TEXT_PLAIN)
   public Response remove_juror(@PathParam ("case_name") String caseName,
		   @PathParam ("juror_name") String jurorName)
   {
      synchronized (lock)
      {
    	 if (nimbusApp.removeJuror(caseName, jurorName))
    	  {
			  return(Response.status(200).build());
		  } else { 
			  return(Response.status(404).build());
		  }
      }
   }      
}
