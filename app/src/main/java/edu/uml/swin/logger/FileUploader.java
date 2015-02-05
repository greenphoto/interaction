package edu.uml.swin.logger;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.util.Log;

public class FileUploader extends AsyncTask<Void, Void, String> {

    private static String TAG = "InteractionService/FileUploader";

    public IApiAccessUploadResponse delegate = null;

    private Context mContext;
	private ConnectionDetector mConnectionDetector;
    private LogDbHelper logDbHelper;

	private static int BUFFER_SIZE = 1024;



	public FileUploader(Context context) {
		mContext = context;
    	mConnectionDetector = new ConnectionDetector(mContext);
        logDbHelper = new LogDbHelper(context);
	}
	
	@Override
    protected void onPreExecute() {
    	super.onPreExecute();
    	
    	if (! mConnectionDetector.isConnectingToInternet()) {
    		Log.d(TAG, "WiFi is not connected to Internet, cannot upload file");
    		cancel(true);
    	}
    }
	
	@Override
	protected String doInBackground(Void... params) {
        String message = "Oops, there must be something wrong.\n";
        SQLiteDatabase db = logDbHelper.getWritableDatabase();
		String DB_PATH = db.getPath();
//		if (android.os.Build.VERSION.SDK_INT >= 4.2) {
//	    	DB_PATH = mContext.getApplicationInfo().dataDir + "/databases/";
//	    } else {
//	    	DB_PATH = mContext.getFilesDir().getPath() + "/" + mContext.getPackageName() + "/databases/";
//	    }
//		String fullFilePath = DB_PATH + Constants.DB_NAME;
		//Log.d(Constants.TAG, "filepath = " + fullFilePath); 
		String zipFilePath = mContext.getFilesDir() + "/"+ Constants.ZIP_FILE_NAME;
		String[] files = {DB_PATH};
		try {
			zip(files, zipFilePath);
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		
		try {
			HttpClient httpClient = new DefaultHttpClient();
			HttpPost httpPost = new HttpPost(Constants.POST_FILE_URL);
			File file = new File(zipFilePath);
			// Check if the database has already been created 
			if (!file.exists()) {
                message = "Database has not been created, cancel uploading.\n";
				Log.d(TAG, "Database has not been created, cancel uploading.");
				httpClient.getConnectionManager().shutdown();
				return null;
			}
			FileBody fileBody = new FileBody(file);
			
			// Build the post 
			MultipartEntityBuilder reqEntity = MultipartEntityBuilder.create();
			reqEntity.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
			reqEntity.addPart("file", fileBody);
			reqEntity.addTextBody("newFileName", Utils.getTimeAsFileName() + ".zip");
			httpPost.setEntity(reqEntity.build());
			
			// execute HTTP post request
			HttpResponse response = httpClient.execute(httpPost);
			HttpEntity resEntity = response.getEntity();
			
			if (resEntity != null) {
				String responseStr = EntityUtils.toString(resEntity).trim();
				Log.d(TAG, "File uploader received response: " + responseStr);
//				if (responseStr.equals("success")) {
//					updateUploadTime();
//				}
                message = "File uploader received response: " + responseStr + ".\n";
                resEntity.consumeContent();
			} else {
				Log.d(TAG, "File uploader got no response from remote server");

                message = "Oops, file uploader got no response from remote server.\n";
			}

			httpClient.getConnectionManager().shutdown();
		} catch (NullPointerException e) {
        	e.printStackTrace();
        } catch (Exception e) {
        	e.printStackTrace();
        }
		
		return message;
	}
	
	private void zip(String[] files, String zipFile) throws IOException {
		BufferedInputStream origin = null;
		ZipOutputStream out = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(zipFile)));
		try {
			byte data[] = new byte[BUFFER_SIZE];
			for (int i = 0; i < files.length; i++) {
				FileInputStream fi = new FileInputStream(files[i]);
				origin = new BufferedInputStream(fi, BUFFER_SIZE);
				try {
					ZipEntry entry = new ZipEntry(files[i].substring(files[i].lastIndexOf("/") + 1));
					out.putNextEntry(entry);
					int count;
					while ((count = origin.read(data, 0, BUFFER_SIZE)) != -1) {
						out.write(data, 0, count);
					}
				} finally {
					origin.close();
				}
			}
		} finally  {
			out.close();
		}
	}

	protected void onPostExecute(String message){
        if(delegate!=null){
            delegate.postResult(message);
        }
        else{
            Log.e(TAG,"You have not assigned IApiAccessUploadResponse delegate");
        }
    }
	
}
