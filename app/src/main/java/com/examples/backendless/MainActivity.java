package com.examples.backendless;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
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
import com.backendless.BackendlessUser;
import com.backendless.HeadersManager;
import com.backendless.async.callback.AsyncCallback;
import com.backendless.exceptions.BackendlessFault;
import com.backendless.files.BackendlessFile;
import com.backendless.messaging.MessageStatus;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();
    private static final String PROPERTY_STORAGE = MainActivity.class.getName();

    private static final String USER_NAME_KEY = "userName";
    private static final String USER_PASSWORD_KEY = "userPassword";
    private static final String USER_TOKEN_KEY = "userToken";
    private static final int rc_ScanQR = 1;
    private static final int select_files = 9;

    public static final Uri zxingUri = Uri.parse("https://play.google.com/store/apps/details?id=com.google.zxing.client.android");
    private static final String ZXING_PACKAGE = "com.google.zxing.client.android";

    private static final String appId = "95AC1130-CE53-E5BC-FF8D-472B57705B00" ;
    private static final String apiKey = "F40D23FC-FAB5-4B0B-9587-1E0F51AA8E64";


    private Uri fileUri;
    private EditText editText_name;
    private EditText editText_password;
    private EditText editText_userInfo;
    private Button button_login_logout;
    private Button button_loginWithQR;

    private Button button_upload;
    private Button button_upload2;
    private EditText editText_fileInfo;

    private String channelName = null;
    private String userName = null;
    private String userPassword = null;
    private String userToken = null;

    private String uploadLink = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initUI();
        Backendless.initApp(this.getApplicationContext(), appId, apiKey);

        if (!isPackageInstalled(ZXING_PACKAGE))
        {
            editText_name.setEnabled(false);
            editText_password.setEnabled(false);
            editText_userInfo.setEnabled(false);
            button_login_logout.setEnabled(false);
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
        editText_name = findViewById(R.id.editText_name);
        editText_password = findViewById(R.id.editText_password);
        editText_userInfo = findViewById(R.id.editText_userInfo);

        button_login_logout = findViewById(R.id.button_login);
        button_loginWithQR = findViewById(R.id.button_loginWithQR);
        button_loginWithQR.setOnClickListener(this::scanDataFromQRCode);

        editText_fileInfo = findViewById(R.id.editText_fileInfo);
        button_upload = findViewById(R.id.button_upload);
        button_upload2 = findViewById(R.id.button_upload2);
    }

    @Override
    protected void onPause() {
        super.onPause();
        SharedPreferences sharedPreferences = getSharedPreferences(PROPERTY_STORAGE, MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(USER_NAME_KEY, this.userName);
        editor.putString(USER_PASSWORD_KEY, this.userPassword);
        editor.putString(USER_TOKEN_KEY, this.userToken);
        editor.apply();
        Log.i(MainActivity.class.getSimpleName(), "onPause: saved data successfully [userName=" + this.userName + ", userPassword=" + this.userPassword + ", userToken=" + this.userToken + "]");
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        SharedPreferences sharedPreferences = getSharedPreferences(PROPERTY_STORAGE, MODE_PRIVATE);
        this.userName = sharedPreferences.getString(USER_NAME_KEY, null);
        this.editText_name.setText(this.userName);
        this.userPassword = sharedPreferences.getString(USER_PASSWORD_KEY, null);
        this.editText_password.setText(this.userPassword);
        this.userToken = sharedPreferences.getString(USER_TOKEN_KEY, null);
        Log.i(MainActivity.class.getSimpleName(), "onPostResume: restore data successfully [userName=" + this.userName + ", userPassword=" + this.userPassword + ", userToken=" + this.userToken + "]");

        if (userToken != null) {
            button_loginWithQR.setVisibility(View.VISIBLE);
            button_login_logout.setText("Logout");
            button_login_logout.setOnClickListener(this::backendlessLogout);

            button_upload.setVisibility(View.VISIBLE);
            button_upload.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    //upload files goes to the second activity,
                    uploadFiles();
                }
            });
        } else {
            button_login_logout.setOnClickListener(this::backendlessLogin);
        }

        if (channelName != null)
            loginWithQRCode(this.channelName);
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

    private void backendlessLogin(View view) {
        this.userName = editText_name.getText().toString();
        this.userPassword = editText_password.getText().toString();

        Backendless.UserService.login(MainActivity.this.userName, MainActivity.this.userPassword, new AsyncCallback<BackendlessUser>() {
            @Override
            public void handleResponse(BackendlessUser response) {
                button_upload.setVisibility(View.VISIBLE);
                button_loginWithQR.setVisibility(View.VISIBLE);
                button_login_logout.setText("Logout");
                button_login_logout.setOnClickListener(MainActivity.this::backendlessLogout);

                StringBuilder sb = new StringBuilder();
                for (Map.Entry<String, Object> property : response.getProperties().entrySet())
                    sb.append(property.getKey()).append(" : ").append(property.getValue()).append('\n');

                //editText_userInfo.setText(sb.toString());
                editText_userInfo.setText("Login Successful");
                userToken = HeadersManager.getInstance().getHeader(HeadersManager.HeadersEnum.USER_TOKEN_KEY);
                Log.i(MainActivity.class.getSimpleName(), "backendlessLogin [userToken=" + MainActivity.this.userToken + "]");

            }

            @Override
            public void handleFault(BackendlessFault fault) {
                userToken = null;
                editText_userInfo.setText(fault.getCode() + '\n' + fault.getMessage() + '\n' + fault.getDetail());
            }
        });
    }

    private void backendlessLogout(View view) {
        Backendless.UserService.logout(new AsyncCallback<Void>() {
            @Override
            public void handleResponse(Void response) {
                userToken = null;
                button_loginWithQR.setVisibility(View.INVISIBLE);
                button_upload.setVisibility(View.INVISIBLE);
                button_upload2.setVisibility(View.INVISIBLE);

                button_login_logout.setText("Login to Backendless");
                button_login_logout.setOnClickListener(MainActivity.this::backendlessLogin);
                editText_userInfo.setText("");
            }

            @Override
            public void handleFault(BackendlessFault fault) {
                editText_userInfo.setText(fault.toString());
            }
        });
    }

    private void scanDataFromQRCode(View view) {
        Intent intent = new Intent(ZXING_PACKAGE + ".SCAN");
        intent.setPackage(ZXING_PACKAGE);
        intent.putExtra("SCAN_MODE", "QR_CODE_MODE");
        startActivityForResult(intent, rc_ScanQR);
    }

    private void uploadFiles() {
        Intent i = new Intent(this, upload.class);
        startActivityForResult(i, select_files);
    }


    /* method that I'm trying to get working
        Backendless.Files.upload throws exception that the file cannot be read
     */
    private void pushToBackEnd(){
        //
        /*

        String path = fileUri.getPath();
        String path = fileUri.getEncodedPath();
        rn I'm trying to use a getPath function from StackOverflow to pull the file path
         */

        String path = getPath(this, fileUri);
        Log.i(TAG, "Pushing to backend, current fileUri is: " + path);
        if(fileUri != null) {
            File file = new File(path);
            Backendless.Files.upload(file, "/user_files", new AsyncCallback<BackendlessFile>() {
                @Override
                public void handleResponse(BackendlessFile uploadedFile) {
                    Log.i(TAG, "File has been uploaded. File URL is - " + uploadedFile.getFileURL());
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

    //tester method that pushes a text file to the back end, confirmed works!
    private void pushText() throws IOException {

        Log.i(TAG, "Pushing a text file");
        String filename = "new-text.txt";
        File file = new File(this.getFilesDir() + "/" + filename);

        FileOutputStream fileOutputStream = new FileOutputStream(file);
        fileOutputStream.write("I am in pain\nPlease help".getBytes());
        fileOutputStream.close();

        Backendless.Files.upload(file, "/user_files", new AsyncCallback<BackendlessFile>() {
            @Override
            public void handleResponse(BackendlessFile uploadedFile) {
                uploadLink = uploadedFile.getFileURL();
                Log.i(TAG, "File has been uploaded. File URL is - " + uploadedFile.getFileURL());
                Backendless.Messaging.publish(channelName, uploadLink, new AsyncCallback<MessageStatus>() {
                    @Override
                    public void handleResponse(MessageStatus response) {
                        Log.i(MainActivity.class.getSimpleName(), "Uploaded link sent, link is: " + uploadLink);
                    }

                    @Override
                    public void handleFault(BackendlessFault fault) {

                    }
                });
                file.delete();
            }

            @Override
            public void handleFault(BackendlessFault fault) {
                Log.e(TAG, fault.getMessage());
            }
        });


    }

    private void loginWithQRCode(String channelName) {
        Log.i(MainActivity.class.getSimpleName(), "loginWithQRCode: start remote login process");
        if (userToken == null) {
            Log.i(TAG, "loginWithQRCode: userToken is null.");
            return;
        }

        Log.i(MainActivity.class.getSimpleName(), "loginWithQRCode [channelName=" + channelName + ", userToken=" + this.userToken + "]");
        Backendless.Messaging.publish(channelName, this.userToken, new AsyncCallback<MessageStatus>() {
            @Override
            public void handleResponse(MessageStatus response) {
                Log.i(MainActivity.class.getSimpleName(), "loginWithQRCode: sent token successfully");
            }

            @Override
            public void handleFault(BackendlessFault fault) {

            }
        });
        // persist channel
        //this.channelName = null;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (resultCode == RESULT_OK) {
            String contents = intent.getStringExtra("SCAN_RESULT");
            String format = intent.getStringExtra("SCAN_RESULT_FORMAT");
            Log.i(TAG, "OnActivityResult OK");
            switch (requestCode) {
                case rc_ScanQR:
                    this.channelName = contents;
                    break;
                case select_files: //returning from selecting file for upload activity
                    this.fileUri = intent.getData();
                    editText_fileInfo.setText("the uri is: " + fileUri);
                    button_upload2.setVisibility(View.VISIBLE);
                    button_upload2.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {

                            /*pushToBackEnd here is the function to upload
                                  the user selected files. Push text is a tester function that
                                  uploads a text file to the backend (which works!)
                                 */
                            pushToBackEnd();
                            /*
                            try {

                               // pushText();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            */

                        }
                    });
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


    /*Pulled from this stack overflow post: https://stackoverflow.com/questions/17546101/get-real-path-for-uri-android
       also related to https://stackoverflow.com/questions/19985286/convert-content-uri-to-actual-path-in-android-4-4/27271131#27271131

       didn't work for me though :(

     */
    public static String getPath(final Context context, final Uri uri) {
        final boolean isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;

        // DocumentProvider
        if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {
            System.out.println("getPath() uri: " + uri.toString());
            System.out.println("getPath() uri authority: " + uri.getAuthority());
            System.out.println("getPath() uri path: " + uri.getPath());

            // ExternalStorageProvider
            /*originally used "com.android.externalstorage.documents"
            the files I was testing was in so I changed it: com.android.providers.downloads.documents

             */
            if ("com.android.providers.downloads.documents".equals(uri.getAuthority())) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];
                System.out.println("getPath() docId: " + docId + ", split: " + split.length + ", type: " + type);

                // This is for checking Main Memory
                if ("primary".equalsIgnoreCase(type)) {
                    if (split.length > 1) {
                        return Environment.getExternalStorageDirectory() + "/" + split[1] + "/";
                    } else {
                        return Environment.getExternalStorageDirectory() + "/";
                    }
                    // This is for checking SD Card
                } else {
                    return "storage" + "/" + docId.replace(":", "/");
                }

            }
        }
        return null;
    }
}
