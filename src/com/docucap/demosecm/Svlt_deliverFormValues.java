//This servlet is meant to receive a call back from SimpleECM's forms processing with a JSON object
//The JSON will look like the data.json in this project, and will include: 
	//matched template names for each image
	//extracted field text
	//confidence levels (out of 100)
	//field locations
	//field sizes
//As you can see, this servlet simply writes the received JSON to the data.json file
//This could be replaced with a write to a database
package com.docucap.demosecm;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


@WebServlet("/deliverFormValues")
public class Svlt_deliverFormValues extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    public Svlt_deliverFormValues() {
        super();
    }
    
    //refer either a get request or a post request to the doProcess function
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		doProcess(request, response);
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		doProcess(request, response);
	}
	
	private void doProcess(HttpServletRequest request, HttpServletResponse response) throws IOException {
		File reportFile = getReportFile(request);
		if (reportFile.exists())
			reportFile.delete();
		StringBuilder sb = new StringBuilder();
		BufferedReader reader = request.getReader();
		StringBuilder bodyBuilder = new StringBuilder();
		String line;
		while ((line = reader.readLine()) != null)
			bodyBuilder.append(line);
		sb.append(bodyBuilder.toString());
		Writer writer = new FileWriter(reportFile);
		writer.write(sb.toString());
		writer.close();
	}
	
	private File getReportFile(HttpServletRequest request) {
		String path = request.getServletContext().getRealPath("");
		File contextDir = new File(path);
		return new File(contextDir, "data.json");
	}
}
