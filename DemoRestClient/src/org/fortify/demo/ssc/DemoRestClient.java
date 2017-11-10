/*
 * Copyright (c) 2017 Fortify Professional Services

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
 * 
 */


package org.fortify.demo.ssc;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

import javax.net.ssl.SSLContext;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.util.EntityUtils;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;;

/*
 * @Author Zach Lewis
 * @Description Demo Code for SSC 17.20 Rest Interface.
 * 
 * NOTE: The design was single workflow for Report generation and downlaod from SSC.
 * 
 * The Report used is a custom report created to get data for a PowerBI data analytics demo.
 * 
 */
public class DemoRestClient {
	
	private static boolean showDebugInfo = false; 
	private static String myOutputFileName = null;
	
	// NOTE Limit wait for report compeleted to 60 minutes;
    private static int MAX_WAIT_REPORT_CREATE = 60;
    
	public static void main(String[] args) {

		String reportName = null;
		String sscReportToken = null;
		String sscURL = null;
		SSCReport myReport = new SSCReport();
		
		if (args.length > 2) {
			reportName = args[2];
			
			myOutputFileName = args[2] + ".xls";
			
			sscReportToken = args[1];
			sscURL = args[0];	
			if (args.length > 3)
				showDebugInfo = true;
			
		} else {
			System.err.println("usage: SSCDemoRestReport <SSC URL> <SSC Unified Token (BASE64)> <Report Name>");
			return;
		}
		
		/*
		 * NOTE: No Error Handeling was performed and we did use Static StringBuffer for
		 * httpclient Post and Get actions.
		 * 		
		 */
		debugOutput("DEBUG: Starting: createReport(" + sscURL+ "," + reportName + "," +  sscReportToken + ");");
		
		myReport.id = createReport(sscURL, reportName, sscReportToken);
	
		debugOutput("DEBUG: Starting: getReportToken(" + sscURL+ "," + sscReportToken + ")");
		
	    myReport.downloadToken = getReportToken(sscURL,sscReportToken);
	    
	   
	    int start =  0;
	    while ( MAX_WAIT_REPORT_CREATE > start  )
	    {
	    	start = start + 1;
	    	if ( reportInProcess(sscURL, myReport.id, sscReportToken ) )
	    	{
	    		   start = MAX_WAIT_REPORT_CREATE + 1;
	    	} else {
	    	try {
	 			Thread.sleep(60000);
	 		} catch (InterruptedException e) {
	 			// TODO Auto-generated catch block
	 			e.printStackTrace();
	 		};
	    	}
	    }
	    debugOutput("DEBUG: Starting: downloadReport(" + sscURL + "," + sscReportToken + "," + myReport.downloadToken +  "," + myReport.id + ")" );
	   
	    downloadReport(sscURL, sscReportToken, myReport.downloadToken, myReport.id);

	}

	private static void debugOutput(String msg) {
		if (showDebugInfo)
			System.out.println(msg);
		
	}

	public static boolean reportInProcess(String pSscUrl, long pReportId, String pSscToken)
	{
		boolean result = false;
		
		String URL = pSscUrl + "/api/v1/reports/" + pReportId;
		
		debugOutput("DEBUG: [waitReport]: URL: (" + URL + ")");
	
	    String jsonresult = doGet(URL, pSscToken);
		
	    debugOutput("DEBUG: [waitReport]: Result (" + jsonresult + ")");
	    if (jsonresult.contains("PROCESS_COMPLETE"))
	    {
	    	result = true;
	    }
	    else 
	    {
	    	result = false;
	    }
		
		return result;
	}
	public static Long createReport(String sscURL, String pReportName, String pSscToken) {
		
		String URL = sscURL + "/api/v1/reports";
		Long result = -1L;

		StringBuffer body = new StringBuffer();

		body.append("{\"name\":\"" + pReportName
				+ "\",\"note\":\"\",\"type\":\"ISSUE\",\"reportDefinitionId\":18,\"format\":\"XLS\",\"inputReportParameters\":");

		body.append("[{\"name\":\"NA\",\"identifier\":\"foo\",\"paramValue\":true,\"type\":\"BOOLEAN\"}]}");
		
		debugOutput("DEBUG: [createReport]: JSON sent data (" + body.toString() + ")");
		
		String jsonData = doPost(URL,pSscToken, body.toString());
		
		debugOutput("DEBUG: [createReport]: JSON response data (" + jsonData + ")");
		
		JSONParser parser = new JSONParser();
		try {
			Object obj = parser.parse(jsonData);
			JSONObject jsonObject = (JSONObject) obj;
			
            JSONObject data = (JSONObject) jsonObject.get("data");
            debugOutput("DEBUG: [createReport]: JSON Object data (" + data + ")");
			
            result = (Long) data.get("id");
            
         


			
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return result;
	}

	public static String getReportToken(String pSscUrl, String pSscToken) {
		
		String URL = pSscUrl + "/api/v1/fileTokens";
		String result = null;

		StringBuffer body = new StringBuffer();

		body.append("{ \"fileTokenType\": \"REPORT_FILE\" }");

		debugOutput("DEBUG: [getReportToken]: JSON sent data (" + body.toString() + ")");
		
		String jsonData = doPost(URL,pSscToken, body.toString());
		debugOutput("DEBUG: [getReportToken]: JSON response data (" + jsonData + ")");
		
		JSONParser parser = new JSONParser();
		try {
			Object obj = parser.parse(jsonData);
			JSONObject jsonObject = (JSONObject) obj;
         
            JSONObject data = (JSONObject) jsonObject.get("data");
            debugOutput("DEBUG: [getReportToken]: JSON Object data (" + data + ")");
			
            result = (String) data.get("token");
        
            debugOutput("DEBUG: [getReportToken]: " + result);

			
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	
		return result;
	}

	/* TODO: Future ???
	public static String listReports(String pReportName) {
		String result = null;

		String URL = "http://localhost:8888/ssc/api/v1/reports";

		return result;

	}
	*/

	public static String downloadReport(String pSscUrl, String pSscToken, String pSscDownloadToken, Long pReportId) {
		String result = null;
		
		String URL = pSscUrl + "/transfer/reportDownload.html?mat=" + pSscDownloadToken + "&id=" + pReportId;
		
		debugOutput("DEBUG: [downloadReport]: URL: (" + URL + ")");
	
	    doGet(URL, null);
		
		return result;
	}
	
	public static String doPost(String pURL, String pSscToken, String pBody)
	{
		SSLContext sslcontext = null;
	
        String result = null;
		try {
			
			sslcontext = SSLContexts.custom().build();
			
			//TODO:  Add SSL base on Certicate
			
			//loadTrustMaterial(new File("tomcat.keystore"), "changeit".toCharArray(),
				//	new TrustSelfSignedStrategy()).build();

			// Allow TLSv1 protocol only
			SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(sslcontext, new String[] { "TLSv1" },
					null, SSLConnectionSocketFactory.getDefaultHostnameVerifier());
			
			CloseableHttpClient httpclient = HttpClients.custom().setSSLSocketFactory(sslsf).build();

			HttpPost myPost = new HttpPost(pURL);
			//if (pSscToken != null)
            myPost.setHeader("Authorization", "FortifyToken " + pSscToken);
            myPost.setHeader("Content-Type", "application/json");
            
            if (pBody != null ) {
            StringEntity NewReportEntity = new StringEntity(pBody);
            myPost.setEntity(NewReportEntity);
            }
			 
	            // Create a custom response handler
	            ResponseHandler<String> responseHandler = new ResponseHandler<String>() {

	                @Override
	                public String handleResponse(
	                        final HttpResponse response) throws ClientProtocolException, IOException {
	                    
	                	int httpstatus = response.getStatusLine().getStatusCode();
	                    
	                    if (httpstatus >= 200 && httpstatus < 300) {
	                        HttpEntity entity = response.getEntity();
	                        return entity != null ? EntityUtils.toString(entity) : null;
	                    } else {
	                        throw new ClientProtocolException("Unexpected response status: " + httpstatus);
	                    }
	                }

	            };
	            result = httpclient.execute(myPost, responseHandler);
	            debugOutput("DEBUG: [doPost]: result=(" + result + ")"); 

		} catch (KeyManagementException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
	/*	} catch (KeyStoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (CertificateException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		*/
			} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		return result;
	}
	
	public static String doGet(String pURL, String pSscToken)
	{
		SSLContext sslcontext = null;
		
	    boolean hasJsonResult = false;
        
	    String result = null;
        
        if (pSscToken == null)
        {
             hasJsonResult = false;	
        } else
        {
        	hasJsonResult = true;
        }
			
		try {
			sslcontext = SSLContexts.custom().build();
			
			// TODO:  ADD trusted Keystore.
			
			//loadTrustMaterial(new File("tomcat.keystore"), "changeit".toCharArray(),
				//	new TrustSelfSignedStrategy()).build();

			// Allow TLSv1 protocol only
			SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(sslcontext, new String[] { "TLSv1" },
					null, SSLConnectionSocketFactory.getDefaultHostnameVerifier());
			
			CloseableHttpClient httpclient = HttpClients.custom().setSSLSocketFactory(sslsf).build();

			
			HttpGet myGet = new HttpGet(pURL);
			myGet.setHeader("Content-Type","application/json");
		    if (hasJsonResult)
			myGet.setHeader("Authorization", "FortifyToken " + pSscToken );
            
            myGet.getRequestLine();
          
	            // Create a custom response handler
	        ResponseHandler<String> responseHandler = new ResponseHandler<String>() {

	                @Override
	                public String handleResponse(
	                        final HttpResponse response) throws ClientProtocolException, IOException {
	                    
	                	int httpstatus = response.getStatusLine().getStatusCode();
	                    
	                    if (httpstatus >= 200 && httpstatus < 300) {
	                        HttpEntity entity = response.getEntity();
	                        return entity != null ? EntityUtils.toString(entity) : null;
	                    } else {
	                        throw new ClientProtocolException("Unexpected response status: " + httpstatus);
	                    }
	                }

	            };
	          
	            CloseableHttpResponse   response = httpclient.execute(myGet);
	        		   HttpEntity entity = response.getEntity();
	        		   if (entity != null) {
	        			   
	        			   if ( hasJsonResult )
	        			   {
	        				   ByteArrayOutputStream byte1=new ByteArrayOutputStream();
	        				   entity.writeTo(byte1);
	        				   result =byte1.toString();
	   			   
	        			   } else {
	        		       FileOutputStream fos = new FileOutputStream(myOutputFileName);
	        		       entity.writeTo(fos);
	        		       fos.close();
	        			   }
	        		   }
	        		   debugOutput("DEBUG: [doGet]: Response=(" + response.toString() + ")"); 
	        	response.close();
	        	
		} catch (KeyManagementException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
	/*	} catch (KeyStoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (CertificateException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		
			} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		*/
		} catch (ClientProtocolException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	    
		return result;
	}


}
