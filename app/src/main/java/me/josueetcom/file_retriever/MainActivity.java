package me.josueetcom.file_retriever;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

public class MainActivity extends AppCompatActivity {
    EditText input, protocol, host, port, path;
    Button go;
    SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        go = (Button) findViewById(R.id.bGo);

        // Get the preferences for this app
        prefs = getSharedPreferences(getPackageName(), MODE_PRIVATE);

        // Get the EditText fields
        protocol = (EditText) findViewById(R.id.etProtocol);
        host = (EditText) findViewById(R.id.etHost);
        port = (EditText) findViewById(R.id.etPort);
        path = (EditText) findViewById(R.id.etPath);

        go.setOnClickListener(v -> {
            // Pass this the input values to the DisplayActivity
            Intent intent = new Intent(MainActivity.this, DisplayActivity.class)
                    .putExtra(Key.PROTOCOL, protocol.getText().toString())
                    .putExtra(Key.HOST, host.getText().toString())
                    .putExtra(Key.PORT, Integer.parseInt(port.getText().toString()))
                    .putExtra(Key.FILEPATH, path.getText().toString());
            startActivity(intent);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (prefs.getBoolean(Key.FIRST_RUN, true)) {
            prefs.edit().putBoolean(Key.FIRST_RUN, false).apply();
            promptForNetID();
        }
        updatePath();
    }

    /**
     * Creates a dialog the first time the app is run to prompt the user for their net id
     */
    private void promptForNetID() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Provide your CSENetID");
        builder.setMessage("Please provide your CSENetID for your CSE student home page");

        // Create an EditText view that'll be displayed
        input = new EditText(this);
        input.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        builder.setView(input);

        builder.setPositiveButton(android.R.string.ok, (d, id) -> {
            String username = input.getEditableText().toString();
            prefs.edit().putString(Key.NETID, username).apply();
            updatePath();
        });
        builder.create().show();
    }

    /**
     * Updates the textView below the path with the netid in the settings
     */
    private void updatePath() {
        String username = prefs.getString(Key.NETID, "");
//        ((TextView) findViewById(R.id.tvNetID)).setText("/~" + username + "/");
    }
}
