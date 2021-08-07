package com.examples.backendless;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.OpenableColumns;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.backendless.Backendless;
import com.backendless.async.callback.AsyncCallback;
import com.backendless.exceptions.BackendlessFault;
import com.backendless.files.BackendlessFile;
import com.backendless.messaging.MessageStatus;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import okio.BufferedSink;
import okio.BufferedSource;
import okio.Okio;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();
    private static final String PROPERTY_STORAGE = MainActivity.class.getName();

    private static final int rc_ScanQR = 1;
    private static final int select_files = 9;

    public static final Uri zxingUri = Uri.parse("https://play.google.com/store/apps/details?id=com.google.zxing.client.android");
    private static final String ZXING_PACKAGE = "com.google.zxing.client.android";

    private static final String appId = "95AC1130-CE53-E5BC-FF8D-472B57705B00" ;
    private static final String apiKey = "F40D23FC-FAB5-4B0B-9587-1E0F51AA8E64";


    private Uri fileUri;
    private Button button_loginWithQR;

    private Button button_upload;
    private Button button_upload2;
    private Button button_dc;
    private EditText editText_connectInfo;
    private EditText editText_fileInfo;

    private String channelName = null;


    private boolean isConnected = false;
    private String uploadLink = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initUI();
        Backendless.initApp(this.getApplicationContext(), appId, apiKey);

        if (!isPackageInstalled(ZXING_PACKAGE))
        {
            button_loginWithQR.setEnabled(false);

            Handler handler = new Handler();
            handler.postDelayed(() ->
            {
                MainActivity.this.runOnUiThread(() ->
                {
                    if (getCurrentFocus() != null) {
                        InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                        inputMethodManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
                    }

                    View view = findViewById(R.id.main_activity);
                    Snackbar snackbar = Snackbar.make(view, "Unable to find QR scanner app. Please make sure to install the 'Barcode Scanner' app by ZXing Team", Snackbar.LENGTH_INDEFINITE);
                    View snackbarView = snackbar.getView();
                    TextView textView = (TextView) snackbarView.findViewById(android.support.design.R.id.snackbar_text);
                    textView.setMaxLines(5);
                    snackbar.setAction("Install", this::installZXing);
                    snackbar.show();
                });
            }, 1500);
        }
    }

    private void initUI() {

        editText_connectInfo = findViewById(R.id.editText_userInfo);;
        button_loginWithQR = findViewById(R.id.button_loginWithQR);
        button_loginWithQR.setOnClickListener(this::scanDataFromQRCode);

        editText_fileInfo = findViewById(R.id.editText_fileInfo);
        button_upload = findViewById(R.id.button_pick);
        button_upload2 = findViewById(R.id.button_upload);
        button_dc = findViewById(R.id.button_dc);


    }


    @Override
    protected void onPostResume() {
        super.onPostResume();
        SharedPreferences sharedPreferences = getSharedPreferences(PROPERTY_STORAGE, MODE_PRIVATE);
       // this.isConnected = sharedPreferences.getBoolean(APP_IS_CONNECTED, false);
        Log.i(MainActivity.class.getSimpleName(), "onPostResume: restore data successfully, isConnected is: " + isConnected);

        if (isConnected) { //is connected
            button_loginWithQR.setVisibility(View.INVISIBLE);
            /*
            button_login_logout.setText("Logout");
            button_login_logout.setOnClickListener(this::backendlessLogout);
            */

            button_upload.setVisibility(View.VISIBLE);
            button_dc.setVisibility(View.VISIBLE);
            button_upload.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    //upload files goes to the second activity,
                    uploadFiles();
                }
            });

            button_dc.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    //upload files goes to the second activity,
                    disconnect();
                }
            });
        } else {
            //button_login_logout.setOnClickListener(this::backendlessLogin);
            button_loginWithQR.setVisibility(View.VISIBLE);
            button_upload.setVisibility(View.INVISIBLE);
            button_upload2.setVisibility(View.INVISIBLE);
            button_dc.setVisibility(View.INVISIBLE);
        }

    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.i(MainActivity.class.getSimpleName(), "onStart: ");
    }


    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        Log.i(MainActivity.class.getSimpleName(), "onRestoreInstanceState: ");
    }

    private void scanDataFromQRCode(View view) {
        Intent intent = new Intent(ZXING_PACKAGE + ".SCAN");
        intent.setPackage(ZXING_PACKAGE);
        intent.putExtra("SCAN_MODE", "QR_CODE_MODE");
        startActivityForResult(intent, rc_ScanQR);
    }

    private void uploadFiles() {
        Intent i = new Intent(this, upload.class);
        i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivityForResult(i, select_files);
    }

    private void disconnect() {
        isConnected = false;
        onPostResume();
    }


    /* method that I'm trying to get working
        Backendless.Files.upload throws exception that the file cannot be read
     */
    private void pushToBackEnd(){

        //String path = getPath(this, fileUri);
        Log.i(TAG, "Pushing to backend, current fileUri is: " + fileUri);
        if(fileUri != null) {
            //File file = new File(path);
            String fileName = queryName(fileUri);
            File tempFile = createTempFile(fileName);
            File file = saveContentToFile(fileUri, tempFile);

            Backendless.Files.upload(file, "/user_files", new AsyncCallback<BackendlessFile>() {
                @Override
                public void handleResponse(BackendlessFile uploadedFile) {
                    uploadLink = uploadedFile.getFileURL();

                    Log.i(TAG, "File has been uploaded. File URL is - " + uploadLink);
                    Backendless.Messaging.publish(channelName, uploadLink, new AsyncCallback<MessageStatus>() {
                        @Override
                        public void handleResponse(MessageStatus response) {
                            Log.i(MainActivity.class.getSimpleName(), "Uploaded link sent, link is: " + uploadLink);
                        }

                        @Override
                        public void handleFault(BackendlessFault fault) {

                        }
                    });
                    editText_fileInfo.setText("Upload Complete");
                    Toast.makeText(MainActivity.this, fileName + "uploaded successfully", Toast.LENGTH_SHORT).show();
                    file.delete();
                }

                @Override
                public void handleFault(BackendlessFault fault) {
                    Toast.makeText(MainActivity.this, fault.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e(TAG, fault.getMessage());
                }
            });
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (resultCode == RESULT_OK) {
            String contents = intent.getStringExtra("SCAN_RESULT");
            //String format = intent.getStringExtra("SCAN_RESULT_FORMAT");
            Log.i(TAG, "OnActivityResult OK");
            switch (requestCode) {
                case rc_ScanQR:
                    editText_connectInfo.setText("Connected to Web Browser!");
                    this.channelName = contents;
                    isConnected = true;
                    Backendless.Messaging.publish(channelName, "Connecting", new AsyncCallback<MessageStatus>() {
                        @Override
                        public void handleResponse(MessageStatus response) {
                            Log.i(MainActivity.class.getSimpleName(), "Connected to web browser");
                        }
                        @Override
                        public void handleFault(BackendlessFault fault) {
                        }

                    });
                    break;
                case select_files: //returning from selecting file for upload activity
                    this.fileUri = intent.getData();
                    if(fileUri != null) {
                        editText_fileInfo.setText("File Selected: " + queryName(fileUri));

                        button_upload2.setVisibility(View.VISIBLE);
                        button_upload2.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                pushToBackEnd();
                            }
                        });
                    }
                    else{
                        editText_fileInfo.setText("No File Selected");
                    }
                    break;
            }
        } else if (resultCode == RESULT_CANCELED) {
            Log.i(TAG, "OnActivityResult CANCELED");
            // Handle cancel
        }
    }

    private boolean isPackageInstalled(String packageName) {
        PackageManager pm = this.getApplicationContext().getPackageManager();

        try {
            pm.getPackageInfo(packageName, 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    private void installZXing(View view) {
        Intent googlePlayIntent = new Intent(Intent.ACTION_VIEW, zxingUri);
        googlePlayIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT | Intent.FLAG_ACTIVITY_NO_HISTORY);
        startActivity(googlePlayIntent);

    }


    // private String queryName(ContentResolver resolver, Uri uri) {
    private String queryName(Uri uri) {
        //Cursor returnCursor = resolver.query(uri, null, null, null, null);
        Cursor returnCursor = getContentResolver().query(uri, null, null, null, null);
        assert returnCursor != null;
        int nameIndex = returnCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
        returnCursor.moveToFirst();
        String name = returnCursor.getString(nameIndex);
        returnCursor.close();
        Log.i(TAG, "The file name is: " + name);
        return name;
    }


    private File createTempFile(String name) {
        File file = null;
        //using this to avoid tmp renaming
        file = new File(this.getCacheDir(), name);
        /*
        try {
            //file = File.createTempFile(name, null, getContext().getCacheDir());
            //temp create temp files adds a .tmp, use create new file or something like that
            file = File.createTempFile(name, null, this.getCacheDir());

        } catch (IOException e) {
            e.printStackTrace();
        }
         */
        return file;
    }
    private File saveContentToFile(Uri uri, File file) {
        try {
            //InputStream stream = contentResolver.openInputStream(uri);
            InputStream stream = getContentResolver().openInputStream(uri);
            BufferedSource source = Okio.buffer(Okio.source(stream));
            BufferedSink sink = Okio.buffer(Okio.sink(file));
            sink.writeAll(source);
            sink.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return null;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        return file;
    }
}
