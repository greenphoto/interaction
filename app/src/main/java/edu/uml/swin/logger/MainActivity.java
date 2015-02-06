package edu.uml.swin.logger;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
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

    ProgressDialog dialog = null;
    String upLoadServerUri = null;

    FileUploader uploader = null;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        uploader = new FileUploader(this);
        uploader.delegate = this;

        uploadButton = (Button) findViewById(R.id.uploadButton);
        messageText = (TextView) findViewById(R.id.messageText);

        uploadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog = ProgressDialog.show(MainActivity.this, "", "Uploading file...", true);
                messageText.setText("Uploading started...");
                uploader.execute();
                messageText.setText(uploadResult+"!!!");
            }
        });
    }

    public void postResult(String asyncResult){
        uploadResult = asyncResult;
        dialog.dismiss();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
