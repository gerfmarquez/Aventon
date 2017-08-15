package com.smidur.aventon.http;

;
import android.content.Context;
import android.support.annotation.Nullable;
import android.util.Log;
import android.util.Pair;


import com.amazonaws.mobile.AWSMobileClient;
import com.amazonaws.mobile.user.IdentityManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;


/**
 * Created by marqueg on 12/18/14.
 * do not re-use this class, after a call has been made then spawn another object.
 */
public class HttpWrapper {

    private String TAG = this.getClass().getCanonicalName();

    public HttpWrapper() {
        byte unique_timestamp = (byte)System.currentTimeMillis();
        TAG = TAG+" "+unique_timestamp;
    }
    public HttpWrapper(String rootUrl) {
        this.rootUrl = rootUrl;
        byte unique_timestamp = (byte)System.currentTimeMillis();
        TAG = TAG+" "+unique_timestamp;
    }


//    private String rootUrl = "http://custom-env.ewpmtrqu8z.us-east-1.elasticbeanstalk.com/";
    private String rootUrl = "http://10.0.0.200:5000/";

    //avoid loading ssl certificates every http call
    private static SSLContext sslContext = null;

    private HttpURLConnection connection;

    UpdateCallback updateCallback;
    public interface UpdateCallback {
        public void onUpdate(String message);
    }



    private enum HttpRequestType { PUT, POST, GET,DELETE}

    public HttpResponse httpGET(String path, UpdateCallback updateCallback,Context context) throws IOException {
        this.updateCallback = updateCallback;
        return genericHttpCall(path,context, HttpRequestType.GET,null);
    }

    public HttpResponse httpGET(String path,Context context) throws IOException {
        return genericHttpCall(path,context, HttpRequestType.GET,null);
    }
    public HttpResponse httpPOST(String path, @Nullable Hashtable<String, String> postValues, Context context) throws  IOException {
        return genericHttpCall(path,context, HttpRequestType.POST,postValues);
    }
    public HttpResponse httpPOST(String path, @Nullable UpdateCallback updateCallback, @Nullable String json, Context context) throws  IOException {
        this.updateCallback = updateCallback;
        return genericHttpCall(path,context, HttpRequestType.POST,json);
    }
    public HttpResponse httpPUT(String path, @Nullable String jsonObj, Context context) throws  IOException {
        return genericHttpCall(path,context, HttpRequestType.PUT,jsonObj);
    }
    public HttpResponse httpDELETE(String path, Context context) throws  IOException {
        return genericHttpCall(path,context, HttpRequestType.DELETE,null);
    }

    private HttpResponse genericHttpCall(String path,Context context,HttpRequestType requestType
            , Object requestParams) throws IOException {

        try {
            if(rootUrl==null)rootUrl="";
            String fullURL = rootUrl+path;
            Log.d(TAG+"","Http Request "+requestType.name()+": "+fullURL);
            connection = connectionInitialization(fullURL,context);

            HttpResponse response = new HttpResponse();
            try {
                switch(requestType) {
                    case GET:
                        break;
                    case POST:
                        if(requestParams!=null) {

                            if(requestParams instanceof Hashtable) {
                                makePostRequest(connection, (Hashtable<String,String>) requestParams);
                            } else if(requestParams instanceof String) {
                                makePostRequestWithJson(connection,(String)requestParams);
                            }
                        }
                        break;
                    case PUT:
                        if(requestParams!=null)makePutRequest(connection, (String)requestParams);
                        break;
                    case DELETE:
                        makeDeleteRequest(connection);
                        break;
                }



                response.message = getMessageFromInputStream(connection.getInputStream());

                response.code = connection.getResponseCode();
            } catch(IOException ioe ) {
                Log.w(TAG,ioe);
                response.code = connection.getResponseCode();
            }
//            if(response.code==200) {
//                if(requestType!=HttpRequestType.DELETE) {
//                    response.message = getMessageFromInputStream(connection.getInputStream());
//                }
//            } else {
//                response.message = getMessageFromInputStream(connection.getErrorStream());
//            }
            connection.disconnect();

            Log.d(TAG, "Response Code: " + response.code);
            Log.d(TAG, "Response Message: " + response.message);

            return response;
        } catch (SSLException sslExc) {
            Log.w(TAG,sslExc);
            //force reloading of the certificates in next call
            sslContext = null;
            throw new IOException("SSL issue"+rootUrl+path,sslExc);

        }
    }

    private void makePostRequestWithJson(HttpURLConnection con, String json) throws IOException {
        if(json != null && !json.isEmpty()) {
            con.setRequestMethod("POST");
            con.setRequestProperty("Content-Type", "application/json");
            con.setDoInput(true);
            con.setDoOutput(true);


            OutputStream outputStream = con.getOutputStream();
            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(outputStream,"UTF-8"));
            bw.write(json);
            bw.flush();
            bw.close();
            outputStream.close();


            Log.d(TAG, "Request Sent: " + json + " ");

            con.connect();

        }
    }

    private void makePostRequest(HttpURLConnection con, Hashtable<String, String> postValues) throws IOException {
        if(postValues != null && !postValues.isEmpty()) {
            con.setRequestMethod("POST");
            con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            con.setDoInput(true);
            con.setDoOutput(true);
            List<Pair> postParams = new ArrayList<Pair>();
            Iterator<String> postValuesIterator = postValues.keySet().iterator();

            while(postValuesIterator.hasNext()) {
                String currentKey = postValuesIterator.next();
                postParams.add(new Pair(currentKey,postValues.get(currentKey)));
            }

            OutputStream outputStream = con.getOutputStream();
            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(outputStream,"UTF-8"));
            String requestSent = getQuery(postParams);
            bw.write(requestSent);
            bw.flush();
            bw.close();
            outputStream.close();

            Log.d(TAG, "Request Sent: " + requestSent + " ");

            con.connect();

        }
    }


    public HttpURLConnection getConnection() {
        return connection;
    }
    private void makePutRequest(HttpURLConnection con, String jsonObjString) throws IOException {
        con.setRequestProperty("Content-Type", "application/json");
        con.setRequestMethod("PUT");
        con.setDoInput(true);
        con.setDoOutput(true);

        if(jsonObjString != null && !jsonObjString.isEmpty()) {

            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(con.getOutputStream(),"UTF-8");
            outputStreamWriter.write(jsonObjString);

            outputStreamWriter.flush();
            outputStreamWriter.close();

            Log.d(TAG, "Request Sent: " + jsonObjString + " ");
        }
        con.connect();
    }
    private void makeDeleteRequest(HttpURLConnection con) throws IOException {
        con.setRequestProperty("Content-Type", "application/plain");
        con.setRequestMethod("DELETE");
        con.setDoInput(false);
        con.setDoOutput(false);

        con.connect();
    }
    /**
     /**
     * This method handles the whole http call.
     *
     *  @param url path under order.aventons.com domain
     *  @param context
     *  @return HttpURLConnection
     **/
    private HttpURLConnection connectionInitialization(String url,Context context) throws IOException {
        URL aventonsUrl = new URL(url);
        HttpURLConnection con = null;
        if(url.contains("https")) {
            con = (HttpsURLConnection)aventonsUrl.openConnection();
//            if(server == Constants.URL.API || server == Constants.URL.NOLO)
//                onlyAcceptValidaventonsCertificates((HttpsURLConnection)con,context);
        } else {
            con = (HttpURLConnection)aventonsUrl.openConnection();

        }

        con.setRequestProperty("Accept-Charset", "UTF-8");
        con.setRequestProperty("User-Agent", getUserAgent());



        IdentityManager identityManager = AWSMobileClient.defaultMobileClient()
                .getIdentityManager();


        String accessToken = identityManager.getAccessToken();

        con.setRequestProperty("Authorization",accessToken);
        Log.d(TAG,"header auth sent: "+accessToken);

        con.setConnectTimeout(240000);
        con.setReadTimeout(240000);


        return con;
    }


    private String getMessageFromInputStream(InputStream inputStream) throws  IOException {
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));


        String readLine = null;
        StringBuilder builder = new StringBuilder();
        while ((readLine = bufferedReader.readLine())!=null) {

            if(updateCallback!=null) {
                updateCallback.onUpdate(readLine);
                updateCallback = null;
            }
            if(updateCallback == null) {
                builder.append(readLine);
            }
            Log.d(TAG,"Read Line: "+readLine);

        }
        if(updateCallback == null) {
            return builder.toString();
        }


        try {
            if(bufferedReader!=null)bufferedReader.close();

        } catch (IOException ioe) {}
        finally {
            return "";
        }
    }
    private Object getJsonObjectOrArrayFromString(final String jsonObject) throws JSONException {
        try {
            return  new JSONObject(jsonObject);

        } catch (JSONException jsonExc) {
            if(jsonExc.getMessage().contains("org.json.JSONArray")) {
                try {
                    return new JSONArray(jsonObject);
                } catch (JSONException jsonException) {
                    return null;
                }
            }
            return null;
        }
    }
    private String getQuery(List<Pair> params) throws UnsupportedEncodingException
    {
        StringBuilder result = new StringBuilder();
        boolean first = true;

        for (Pair<String,String> pair : params)
        {
            if (first)
                first = false;
            else
                result.append("&");

            result.append(URLEncoder.encode(pair.first, "UTF-8"));
            result.append("=");
            result.append(URLEncoder.encode(pair.second, "UTF-8"));
        }
        Log.d("Output","String Http POST : "+result.toString());
        return result.toString();
    }
//    private void onlyAcceptValidCertificates(HttpsURLConnection httpsUrlConnection,Context context)  {
//        try {
//
//            if(sslContext ==null) {
//                CertificateFactory cf = CertificateFactory.getInstance("X.509");
//
//                InputStream g2CaInput = new BufferedInputStream(context.getAssets().open("entrust_g2.cer"));
//                InputStream l1kCaInput = new BufferedInputStream(context.getAssets().open("entrust_l1k.cer"));
//                InputStream dxpCaInput = new BufferedInputStream(context.getAssets().open("dxp.cert"));
//
//                Certificate entrustCaG2 = cf.generateCertificate(g2CaInput);
//                Certificate entrustCaL1K = cf.generateCertificate(l1kCaInput);
//                Certificate dxpCa = cf.generateCertificate(dxpCaInput);
//
//                g2CaInput.close();
//                l1kCaInput.close();
//                dxpCaInput.close();
//
//                KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
//                keyStore.load(null, null);
//
//                keyStore.setCertificateEntry("g2", entrustCaG2);
//                keyStore.setCertificateEntry("l1k", entrustCaL1K);
//                keyStore.setCertificateEntry("dxp", dxpCa);
//
//                TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
//                trustManagerFactory.init(keyStore);
//
//
//                this.sslContext = SSLContext.getInstance("SSL");
//                this.sslContext.init(null, trustManagerFactory.getTrustManagers(), new java.security.SecureRandom());
//            }
//            httpsUrlConnection.setSSLSocketFactory(sslContext.getSocketFactory());
//
//
//
//        } catch(NoSuchAlgorithmException| KeyManagementException| IOException|CertificateException|KeyStoreException e) {
//            Log.e("HttpWrapper","",new IOException("Could not load s certificates ",e));
//            sslContext = null;
//
//        }
//    }

    private static String getUserAgent(){
        StringBuilder builder = new StringBuilder();
//        builder.append(" API");
//        builder.append("/" + BuildConfigUtil.getAppVersion());
//        builder.append(" (Android " + Build.VERSION.RELEASE);
//        builder.append("; " + Build.MANUFACTURER);
//        builder.append("/" + Build.MODEL);
//        builder.append("; " + Locale.getDefault().getLanguage());
//        builder.append(")");
        return builder.toString();
    }
}
