package com.docucap.demosecm;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

public class PortalAccess {

	private static final boolean USE_GSON_FOR_PARSING = true;

	public static String getAuthTokenOfUserWithEmail(String email) throws ServletException {
		SecmAccount account = getUserWithEmail(email);
		return account.getAuthenticationToken();
	}

	public static SecmAccount getUserWithEmail(String emailOfUser) throws ServletException {
		List<SecmAccount> accounts = getAllAccounts();
		for (SecmAccount account : accounts) {
			if (account.getEmail().equals(emailOfUser))
				return account;
		}
		return null;
	}

	public static String getIdOfUserWithEmail(String emailOfUser) throws ServletException {
		List<SecmAccount> accounts = getAllAccounts();
		for (SecmAccount account : accounts) {
			if (account.getEmail().equals(emailOfUser))
				return account.getId();
		}
		return null;
	}

	public static String getOAuthJavaScriptForUserWithAuthToken(String authToken, String service) throws ServletException {
		String uriPath = "/users/oauth/app/keysets/" + service + "/javascript";
		String responseStr = PortalAccess.callPortal(uriPath, authToken);
    	JSONTokener tokener = new JSONTokener(responseStr);
		JSONObject jsonObj;
		try {
			jsonObj = new JSONObject(tokener);
			String[] fieldNames = JSONObject.getNames(jsonObj);
			if (fieldNames.length != 1 || !"code".equals(fieldNames[0])) {
				throw new ServletException("Expected JSON object with single field 'code'");
			}
			String code = jsonObj.getString("code");
			return code;
		} catch (JSONException e) {
			e.printStackTrace();
			throw new ServletException(e);
		}
	}

	private static List<SecmAccount> getAllAccounts() throws ServletException {
		String AuthCode = ""; //Set your app's auth code here
		String responseStr = PortalAccess.callPortal("/users", AuthCode);
		if (USE_GSON_FOR_PARSING) {
			return SecmAccount.fromJSonArrayString(responseStr);
		} else {
	    	List<SecmAccount> accounts = new ArrayList<SecmAccount>();
	    	JSONTokener tokener = new JSONTokener(responseStr);
	    	JSONArray finalResult;
			try {
				finalResult = new JSONArray(tokener);
		    	int len = finalResult.length();
		    	for (int i = 0; i < len; i++) {
		    		JSONObject jsonObj = finalResult.getJSONObject(i);
					SecmAccount acct = SecmAccount.fromJson(jsonObj);
					accounts.add(acct);
		    	}
		        return accounts;
			} catch (JSONException e) {
				e.printStackTrace();
				throw new ServletException(e);
			}
		}
	}
	
	public static String callPortal(String uriPath, String authCode) throws ServletException {
		String url = uriPath.startsWith("http") ? uriPath : "https://services.simpleecm.com/api/v0/secm" + uriPath;
        HttpGet httpRequest = new HttpGet(url);
		setHeaders(httpRequest, authCode);
        try {
            return executeRequest(httpRequest);
        } catch (Exception e) {
            e.printStackTrace();
        	throw new ServletException(e);
        } finally {
        	((HttpRequestBase)httpRequest).releaseConnection();
        }
	}

	public static String callPortal(String uriPath, String authCode, String bodyJson) throws ServletException {
		return callPortal(RequestType.POST, uriPath, authCode, bodyJson);
	}

	public static String callPortal(RequestType rqType, String uriPath, String authCode, String bodyJson) throws ServletException {
		String url = uriPath.startsWith("http") ? uriPath : "https://services.simpleecm.com/api/v0/secm" + uriPath;
		HttpEntityEnclosingRequestBase httpRequest = rqType == RequestType.POST ? new HttpPost(url) : new HttpPut(url);
		setHeaders(httpRequest, authCode, true);
        try {
            HttpEntity bodyEntity = new StringEntity(bodyJson);
            httpRequest.setEntity(bodyEntity);
            return executeRequest(httpRequest);
        } catch (Exception e) {
            e.printStackTrace();
        	throw new ServletException(e);
        } finally {
        	((HttpRequestBase)httpRequest).releaseConnection();
        }
	}

	public static void setHeaders(HttpUriRequest httpRequest, String authCode) {
		setHeaders(httpRequest, authCode, false);
	}

	public static void setHeaders(HttpUriRequest httpRequest, String authCode, boolean setContentType) {
        String authHeaderValue;
        String authCodeAndPwd = authCode + ":X";
        String b64 = Utils.convertStringToBase64(authCodeAndPwd);
        authHeaderValue = "Basic " + b64;
        httpRequest.setHeader("Authorization", authHeaderValue);
        httpRequest.setHeader("Accept", "application/json");
        if (setContentType)
            httpRequest.setHeader("Content-Type", "application/json");
	}

	private static String executeRequest(HttpUriRequest httpRequest) throws IOException, ServletException {
		HttpClientBuilder clientBuilder = HttpClientBuilder.create();
		HttpClient httpClient = clientBuilder.build();
        HttpResponse usersResponse = httpClient.execute(httpRequest);
        final HttpEntity usersResponseEntity = usersResponse.getEntity();
        StatusLine statusLine = usersResponse.getStatusLine();
        int statusCode = statusLine.getStatusCode();
        String reasonPhrase = statusLine.getReasonPhrase();
        if (statusCode != HttpStatus.SC_OK) {
            String body = EntityUtils.toString(usersResponseEntity);
            System.out.println("statusCode: " + statusCode + "; reasonPhrase: " + reasonPhrase + "; body: " + body);
            throw new ServletException("reasonPhrase: " + reasonPhrase + "; body: " + body);
        } else {
            if (usersResponseEntity != null) {
                String responseStr = EntityUtils.toString(usersResponseEntity);
                ContentType contentType = ContentType.getOrDefault(usersResponseEntity);
                String mimeType = contentType.getMimeType();
                //System.out.println("responseStr: " + responseStr);
                if ("application/json".equals(mimeType)) {
                	return responseStr;
                } else {
                	throw new ServletException("Response from server wasn't JSON, but: " + mimeType);
                }
            } else {
            	throw new ServletException("The call to the server didn't return a response entity");
            }
        }
	}
	
	public static enum RequestType {
		GET, POST, PUT
	}

}
