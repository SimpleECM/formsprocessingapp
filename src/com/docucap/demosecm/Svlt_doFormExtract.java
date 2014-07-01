package com.docucap.demosecm;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.HttpClientBuilder;


@WebServlet("/doFormExtract")
@MultipartConfig(location = "/tmp")
public class Svlt_doFormExtract extends HttpServlet {
	private static final long serialVersionUID = 1L;

	public Svlt_doFormExtract() {
		super();
	}


	protected void doGet(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
	}


	protected void doPost(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		StoreParamsRec paramsRec = handleInput(request);
		callFormExtraction(paramsRec, response);
	}

	//Saves the incoming file and parameter value to an object
	private StoreParamsRec handleInput(HttpServletRequest request)
			throws ServletException, IOException {
		StoreParamsRec paramsRec = new StoreParamsRec();

		Part formSetPart = request.getPart("formSetId");
		paramsRec.formSetId = getPartStringValue(formSetPart);

		Part filePart = request.getPart("filledForms");
		final String fileName = getFileName(filePart);
		OutputStream out = null;
		InputStream filecontent = null;
		String path = "/tmp";

		try {
			paramsRec.fileToExtract = new File(path + File.separator + fileName);
			out = new FileOutputStream(paramsRec.fileToExtract);
			filecontent = filePart.getInputStream();

			int read = 0;
			final byte[] bytes = new byte[1024];

			while ((read = filecontent.read(bytes)) != -1) {
				out.write(bytes, 0, read);
			}

		} catch (FileNotFoundException fne) {
			throw new ServletException(fne);
		} finally {
			if (out != null) {
				out.close();
			}
			if (filecontent != null) {
				filecontent.close();
			}
		}
		return paramsRec;
	}

	private String getPartStringValue(Part part) throws IOException {
		InputStream iStream = part.getInputStream();
		int nBytes = (int) part.getSize();
		byte[] bytesInStream = new byte[nBytes];
		iStream.read(bytesInStream);
		return new String(bytesInStream);
	}

	private String getFileName(final Part part) {
		for (String content : part.getHeader("content-disposition").split(";")) {
			if (content.trim().startsWith("filename")) {
				return content.substring(content.indexOf('=') + 1).trim()
						.replace("\"", "");
			}
		}
		return null;
	}

	private void callFormExtraction(StoreParamsRec paramsRec,
			HttpServletResponse response) throws IOException, ServletException {
		String appAuthCode = "";  //Set your app's auth code here

		String url = Configuration.SERVICE_URL + "/formExtraction";
		HttpPost httpPost = new HttpPost(url);
		//Calls to SimpleECM made from PortalAccess.java
		PortalAccess.setHeaders(httpPost, appAuthCode);
		//An https callback URL where a servlet will be watching for JSON of the extracted values
		String callbackUrl = "https://dev.testco.com/deliverFormValues";

		MultipartEntityBuilder entityBuilder = MultipartEntityBuilder.create();
		entityBuilder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
		entityBuilder.addTextBody("formSetId", paramsRec.formSetId);
		entityBuilder.addTextBody("callback", callbackUrl);
		entityBuilder.addPart("file", new FileBody(paramsRec.fileToExtract));
		httpPost.setEntity(entityBuilder.build());

		HttpClientBuilder clientBuilder = HttpClientBuilder.create();
		HttpClient httpClient = clientBuilder.build();
		HttpResponse serverResponse = httpClient.execute(httpPost);
		HttpEntity serverResponseEntity = serverResponse.getEntity();
		int statusCode = serverResponse.getStatusLine().getStatusCode();
		if (statusCode != HttpStatus.SC_OK) {
			response.getWriter().write("File rejected");
		} else {
			if (serverResponseEntity != null) {
				ContentType contentType = ContentType
						.getOrDefault(serverResponseEntity);
				String mimeType = contentType.getMimeType();
				//System.out.println("responseStr: " + responseStr);
				if ("application/json".equals(mimeType)) {
					response.getWriter().write("File successfully submitted");
				} else {
					response.getWriter().write("Error - please try again");
				}
			} else {
				response.getWriter().write("Error - please try again");
			}
		}
	}

	private static class StoreParamsRec {
		String formSetId;
		File fileToExtract;
	}

}
