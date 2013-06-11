package edu.umich.its.lti.google;

import java.io.IOException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.http.client.HttpClient;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.conn.ssl.X509HostnameVerifier;
import org.apache.http.impl.client.DefaultHttpClient;

/**
 * This class provides method to wrap an HTTP Client to handle SSL requests in
 * manner safe for development, accepting the server's certificate.
 * 
 * This code comes from the following:
 * <ol>
 * 	<li>http://javaskeleton.blogspot.com/2010/07/avoiding-peer-not-authenticated-with.html</li>
 * 	<li>http://hc.apache.org/httpcomponents-client-ga/httpclient/examples/org/apache/http/examples/client/ClientCustomSSL.java</li>
 * 	<li>http://hc.apache.org/httpcomponents-client-ga/tutorial/pdf/httpclient-tutorial.pdf</li>
 * </ol>
 */
public class ClientSslWrapper {
	public static HttpClient wrapClient(HttpClient base) {
		//http://javaskeleton.blogspot.com/2010/07/avoiding-peer-not-authenticated-with.html
		try {
			SSLContext ctx = SSLContext.getInstance("TLS");
			X509TrustManager tm = new X509TrustManager() {
 
				public void checkClientTrusted(X509Certificate[] xcs, String string) throws CertificateException {
				}
 
				public void checkServerTrusted(X509Certificate[] xcs, String string) throws CertificateException {
				}
 
				public X509Certificate[] getAcceptedIssuers() {
				    return null;
				}
			};
			X509HostnameVerifier verifier = new X509HostnameVerifier() {
 				@Override
				public void verify(String string, SSLSocket ssls) throws IOException {
				}
 
				@Override
				public void verify(String string, X509Certificate xc) throws SSLException {
				}
 
				@Override
				public void verify(String string, String[] strings, String[] strings1) throws SSLException {
				}

				@Override
				public boolean verify(String string, SSLSession ssls) {
				    return true;
				}
			};
			ctx.init(null, new TrustManager[]{tm}, null);
			SSLSocketFactory ssf = new SSLSocketFactory(ctx, verifier);
			ClientConnectionManager ccm = base.getConnectionManager();
			SchemeRegistry sr = ccm.getSchemeRegistry();
			sr.register(new Scheme("https", 443, ssf));
			return new DefaultHttpClient(ccm, base.getParams());
		} catch (Exception ex) {
			ex.printStackTrace();
			return null;
		}
	}
}
