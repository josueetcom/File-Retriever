package me.josueetcom.file_retriever;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Created by t-sirio on 8/23/16.
 */
public class DisplayActivity extends AppCompatActivity {
    private static final String TAG = "DisplayActivity";
    SharedPreferences prefs;
    TextView viewer;
    String fileName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display);
        viewer = (TextView) findViewById(R.id.tvViewer);

        // Get the preferences for the app
        prefs = getSharedPreferences(getPackageName(), MODE_PRIVATE);

        // Asynchronously fetch the given URL
        new DataFetcherTask().execute(getIntent());
    }

    @Override
    public void onBackPressed() {
        // Get the temporary file
        File temp = new File(getFilesDir(), fileName + ".tmp");
        if (temp.exists())
            temp.delete();
        super.onBackPressed();
    }

    /** Downloads the outputted text into a file in downloads **/
    private void downloadFile() {
        if (isExternalStorageWritable()) {
            // Get the temporary file
            File temp = new File(getFilesDir(), fileName + ".tmp");
            if (!temp.exists()) {
                // Means they already have it downloaded
                return;
            }

            // Get the downloads directory and create a new File in there to write to
            File dir = getDownloadsDir();
            File outFile = new File(dir, fileName);

            try {
                // Create streams to read from and write to
                FileInputStream in = new FileInputStream(temp);
                BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                PrintStream out = new PrintStream(outFile);

                // Read in and write out one line at a time
                String line;
                while ((line = reader.readLine()) != null)
                    out.println(line);

                // Close the streams and delete the temporary file
                in.close();
                out.close();
                temp.delete();

                showOkDialog("Download successful", fileName + " downloaded successfully to your device.");
            } catch (IOException e) {
                e.printStackTrace();
                Log.e(TAG, e.getMessage());
                showOkDialog("Write Failed", "An error occured while saving this file.");
            }
        } else {
            showOkDialog("Write Failed", "Please mount the external storage");
        }
    }

    /**
     * Creates and shows a simple confirmation dialog to the user
     * @param title the title of the dialog
     * @param message the message of the dialog
     */
    private void showOkDialog(String title, String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(title);
        builder.setMessage(message);
        builder.setPositiveButton(android.R.string.ok, (d, w) -> d.dismiss());
        builder.create().show();
    }

    /**
     * @return the downloads directory
     */
    private File getDownloadsDir() {
        // Get the directory for the user's public downloads directory.
        return Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOWNLOADS);
    }

    /** Checks if external storage is available for read and write */
    private boolean isExternalStorageWritable() {
        // This would be false if the user takes out an SD card or mounts his phone to the computer
        return Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
    }

    /**
     * This class loads the file from the URL asynchronously and writes a temporary file with the
     * contents
     */
    private class DataFetcherTask extends AsyncTask<Intent, Integer, String> {
        ProgressDialog dialog;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            dialog = new ProgressDialog(DisplayActivity.this);
            dialog.setTitle("Downloading...");
            dialog.setMessage("0 of 0 bytes");
            dialog.show();
        }

        @Override
        protected String doInBackground(Intent... params) {
            Intent i = params[0];
            String message = "";
//            String netid = prefs.getString(Key.NETID, "jrios777");
            String relativePath = i.getStringExtra(Key.FILEPATH);
            if (relativePath.endsWith("/"))
                relativePath += "index.html";

            URL url = null;
            try {
                // Setup the connections
                url = new URL(i.getStringExtra(Key.PROTOCOL),
                        i.getStringExtra(Key.HOST),
                        i.getIntExtra(Key.PORT, 80),
                        relativePath);

                // Figure out the fileName from the URL
                fileName = Uri.parse(url.toString()).getLastPathSegment();

                // Setup the connection (assuming http connection)
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setReadTimeout(10000 /* milliseconds */);
                connection.setConnectTimeout(15000 /* milliseconds */);
                connection.setRequestMethod("GET");
                connection.setDoInput(true);
                connection.connect();

                // Check the response tag
                int response = connection.getResponseCode();
                Log.d(TAG, "The response is: " + response);

                // Create the streams to read and write from
                InputStream in = connection.getInputStream();
                PrintStream out = new PrintStream(openFileOutput(fileName + ".tmp", MODE_PRIVATE));

                // Get the contentLength, we're going to use the progress bar
                int contentLength = connection.getContentLength();
                int totalRead = 0;

                // Is this a plain text file?
                int type = fileType(fileName);
                if (type == 0) {
                    // For storing the resulting String
                    StringBuilder result = new StringBuilder();

                    // We'll use a reader and PrintStream
                    BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                    String line;
                    while ((line = reader.readLine()) != null) {
                        out.println(line);
                        totalRead += line.getBytes().length;
                        result.append(line + "\n");
                        publishProgress(totalRead, contentLength);
                    }

                    message = result.toString();
                } else {
                    // It's in a format I don't know how to display as plain text
                    byte[] buffer = new byte[1024];
                    int readLength;
                    do {
                        publishProgress(totalRead, contentLength);
                        readLength = in.read(buffer, 0, 1024);
                        totalRead += readLength;
                        out.write(buffer, 0, readLength);
                    } while (totalRead < contentLength && readLength != -1);

                    message = fileName + " doesn't seem to be a plain text or image format. You can still save the " +
                            "file though...";
                }

                // Close the streams
                in.close();
                out.close();
            } catch (MalformedURLException e) {
                e.printStackTrace();
                Log.e("DataFetcherTask", "Bad URL: " + e.getMessage());
                if (url != null ) message = "Could not access given URL: " + url.toString();
            } catch (IOException e) {
                e.printStackTrace();
                Log.e("DataFetcherTask", "An error occur while reading from: " + e.getMessage());
                message = "An error occur while reading from the given URL: " + url.toString();
            }
            return message;
        }

        /**
         * Returns the type of file
         * @param filename the name of the file
         * @return 0 if plain text, 1 if image, 2 if something else
         */
        private int fileType(String filename) {
            int lastDot = filename.lastIndexOf('.');
            if (lastDot != -1) {
                String suffix = filename.substring(lastDot + 1).toLowerCase();
                String[] plain = {"txt", "c", "cpp", "cs", "css", "csv", "htm",
                        "html", "java", "js", "jsp", "php", "py",
                        "rb", "sh", "sql", "tsv", "xhtml", "xml"};
                for (String s: plain)
                    if (s.equals(suffix)) return 0;

                String[] imageTypes = {"jpg", "png", "bmp", "gif", "tiff"};
                for (String s: imageTypes)
                    if (s.equals(suffix)) return 1;
            }
            return 2;
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
            int percent = values[0] * 10000 / values[1];
            dialog.setProgress(percent);    // Assumes values[1] != -1
            dialog.setMessage("Downloaded " + values[0] / 1024 + "KB/" + values[1] / 1024 + "KB");
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            dialog.dismiss();
            viewer.setText(s);
        }
    }

}
