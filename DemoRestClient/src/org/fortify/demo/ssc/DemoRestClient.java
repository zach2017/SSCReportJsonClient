package org.fortify.demo.ssc;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

import javax.net.ssl.SSLContext;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.util.EntityUtils;;

public class DemoRestClient {

	public static void main(String[] args) {

		String reportName = null;
		String sscReportToken = null;
		String sscURL = null;
		SSCReport myReport = new SSCReport();
		
		if (args.length > 2) {
			reportName = args[0];
			sscReportToken = args[1];
			sscURL = args[2];			
		} else {
			sscURL = "http://localhost:8080/ssc";

			reportName = "aaademo1";
			sscReportToken = "NTNhMmU0OGItNjFlZi00NDNkLWJhZDQtMDNlZTliOTAxNWQx";
		}
		
	
		
		myReport.id = createReport(sscURL, reportName, sscReportToken);
	
		
	    myReport.downloadToken = getReportToken(sscURL,sscReportToken);
	    
	    downloadReport(sscURL, myReport.downloadToken, myReport.id);

	}

	public static int createReport(String sscURL, String pReportName, String pSscToken) {
		
		String URL = sscURL + "/api/v1/reports";
		int result = -1;

		StringBuffer body = new StringBuffer();

		body.append("{\"name\":\"" + pReportName
				+ "\",\"note\":\"\",\"type\":\"ISSUE\",\"reportDefinitionId\":18,\"format\":\"XLS\",\"inputReportParameters\":");

		body.append("[{\"name\":\"NA\",\"identifier\":\"foo\",\"paramValue\":true,\"type\":\"BOOLEAN\"}]}");

		String jsonData = doPost(URL,pSscToken, body.toString());

		result = 31;
		
		// TODO: Parse JSON for Report ID
		return result;
	}

	public static String getReportToken(String pSscUrl, String pSscToken) {
		
		String URL = pSscUrl + "/api/v1/fileTokens";
		String result = null;

		StringBuffer body = new StringBuffer();

		body.append("{ \"fileTokenType\": \"REPORT_FILE\" }");

		
		String jsonData = doPost(URL,pSscToken, body.toString());
		
		// TODO: Parse JSON for Report Token
        result = "Y2FmMmQ2MjgtMTQyZS00MGVhLTg1ZWQtMjU5MmFjYTA2OTUx";
		return result;
	}

	public static String listReports(String pReportName) {
		String result = null;

		String URL = "http://localhost:8888/ssc/api/v1/reports";

		return result;

	}

	public static String downloadReport(String pSscUrl, String pSscDownloadToken, int pReportId) {
		String result = null;
		
		String URL = pSscUrl + "/transfer/reportDownload.html?mat=" + pSscDownloadToken + "&id=" + pReportId;

		doPost(URL, "", "");
		
		return result;
	}
	
	public static String doPost(String pURL, String pSscToken, String pBody)
	{
		SSLContext sslcontext = null;
		if ( pURL.contains("http"))
		return "";
		
        String result = null;
		try {
			sslcontext = SSLContexts.custom().build();//loadTrustMaterial(new File("tomcat.keystore"), "changeit".toCharArray(),
				//	new TrustSelfSignedStrategy()).build();

			// Allow TLSv1 protocol only
			SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(sslcontext, new String[] { "TLSv1" },
					null, SSLConnectionSocketFactory.getDefaultHostnameVerifier());
			
			CloseableHttpClient httpclient = HttpClients.custom().setSSLSocketFactory(sslsf).build();

			//HttpGet httpget = new HttpGet("http://localhost:8080/ssc/api/v1/reports");
			HttpPost myPost = new HttpPost(pURL);
			
            myPost.setHeader("Authorization", "FortifyToken " + pSscToken);
            myPost.setHeader("Content-Type", "application/json");
            
            StringEntity NewReportEntity = new StringEntity(pBody);
            myPost.setEntity(NewReportEntity);
         
			 
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
	            System.out.println("----------------------------------------");
	            System.out.println("Debug: " + result);
			   
       

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

}
