package com.examples.backendless;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.OpenableColumns;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import okio.BufferedSink;
import okio.BufferedSource;
import okio.Okio;

public class upload extends AppCompatActivity {

    //private ContentResolver contentResolver;
    private Button button_return;
    private Button button_select;
    private EditText et_fileSelect;
    private Uri outUri;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upload);

        et_fileSelect = findViewById(R.id.et_fileSelect);
        button_select = findViewById(R.id.button_select);
        button_return = findViewById(R.id.button_return);

        button_select.setOnClickListener(this::openFile);
        button_return.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent returnIntent = new Intent();
                returnIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                //returnIntent.putExtra("result", outUri);\
                returnIntent.setData(outUri);
                setResult(Activity.RESULT_OK,returnIntent);
                finish();
            }
        });
    }

    // Request code
    private static final int FILE_RC = 2;

    //private void openFile(Uri pickerInitialUri) {
    public void openFile(View view) {
        Log.d("UPLOAD", "Attempting to open file");
        //Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.setType("*/*");
        //intent.setType("application/pdf");
        // Optionally, specify a URI for the file that should appear in the
        // system file picker when it loads.
        //intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, pickerInitialUri);
        //intent = Intent.createChooser(intent, "Choose a file");
        startActivityForResult(intent, FILE_RC);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode,
                                 Intent resultData) {

        /* This block is pulled from the android docs here:
        https://developer.android.com/training/data-storage/shared/documents-files#perform-operations */

        if (requestCode == FILE_RC
                && resultCode == Activity.RESULT_OK) {
            // The result data contains a URI for the document or directory that
            // the user selected.
            Uri uri = null;
            if (resultData != null) {
                uri = resultData.getData();
                // Perform operations on the document using its URI.
            }

            if(uri != null){
                et_fileSelect.setText("Current File is: " + queryName(uri));
            }else{
                et_fileSelect.setText("No File Selected");
            }

            //Log.d("UPLOAD", "the URI returned is: " + uri);
            outUri = uri;
        }
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
        return name;
    }

}