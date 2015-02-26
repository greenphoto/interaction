package edu.uml.swin.logger;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.io.File;


public class MainActivity extends ActionBarActivity implements IApiAccessUploadResponse{

    private TextView messageText;
    private Button uploadButton;
    private String uploadResult;
    private Context mySelf;
    private IApiAccessUploadResponse responseHolder;
    private String resultMessage="";
    private Menu mMenu;

    ProgressDialog dialog = null;
    String upLoadServerUri = null;

    FileUploader uploader = null;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.d("TAG", "in onCreate");
        mySelf = this;
        responseHolder = this;

        uploadButton = (Button) findViewById(R.id.uploadButton);
        messageText = (TextView) findViewById(R.id.messageText);
        messageText.setText(resultMessage);

        uploadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog = ProgressDialog.show(MainActivity.this, "", "Uploading file...", true);

                uploader = new FileUploader(mySelf);
                uploader.delegate = responseHolder;
                resultMessage = "Uploading started...";
                messageText.setText(resultMessage);
                uploader.execute();
            }
        });
    }

    @Override
    protected void onResume(){
        super.onResume();
        Log.d(FileUploader.TAG, "in onResume");
        messageText.setText(resultMessage);
    }

    @Override
    protected void onPause(){
        super.onPause();
        resultMessage = "";
        Log.d(FileUploader.TAG, "in onPause");
        messageText.setText(resultMessage);
    }

    public void postResult(String asyncResult){
        uploadResult = asyncResult;
        resultMessage = Utils.getTimeAsFileName() + " - " + uploadResult;
        messageText.setText(resultMessage);
        dialog.dismiss();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        mMenu = menu;
        updateMenuTitle();
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_enable_settings) {
            if(InteractionService.isLoggingEnabled()){
                InteractionService.enableLogging(false);
            }else {
                InteractionService.enableLogging(true);
            }
            updateMenuTitle();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void updateMenuTitle(){
        MenuItem bedMenuItem = mMenu.findItem(R.id.action_enable_settings);
        if (InteractionService.isLoggingEnabled()) {
            bedMenuItem.setTitle("Disable logging");
        } else {
            bedMenuItem.setTitle("Enable logging");
        }
    }
}
