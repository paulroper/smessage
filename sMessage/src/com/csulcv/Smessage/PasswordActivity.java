package com.csulcv.Smessage;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class PasswordActivity extends ActionBarActivity {

    private static final String TAG = "Smessage: Password Activity";
    public static final String USER_PASSWORD = "com.csulcv.smessage.userPassword";

    public static boolean firstRun = true;
    private SharedPreferences settings = null;

    /**
     *
     * @see android.app.Activity
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_password);

        getSupportActionBar().hide();

        //final boolean keyStoreExists = KeyStoreManager.keyStoreExists(this);
        settings = getSharedPreferences("settings", MODE_PRIVATE);

        setupActivity();

    }

    /**
     * Put in to a separate method so that the activity can be put back to its first run form when the password is reset.
     */
    public void setupActivity() {

        firstRun = settings.getBoolean("firstRun", true);

        if (firstRun) {
            TextView passwordMessage = (TextView) findViewById(R.id.enter_password);
            passwordMessage.setText("Please enter a new password");
            Button resetPassword = (Button) findViewById(R.id.reset_password);
            resetPassword.setVisibility(View.GONE);
        } else {
            TextView passwordMessage = (TextView) findViewById(R.id.enter_password);
            passwordMessage.setText("Please enter your password");
        }

        EditText password = (EditText) findViewById(R.id.password);
        password.setOnEditorActionListener(new TextView.OnEditorActionListener() {

            @Override
            public boolean onEditorAction(TextView view, int actionId, KeyEvent event) {

                // Called when the user presses the done button on their keyboard
                if (actionId == EditorInfo.IME_ACTION_DONE) {

                    String password = view.getText().toString();

                    // Sanitise the password input
                    if (password.length() == 0 || password.length() < 5 || password.contains(" ")) {

                        Toast.makeText(PasswordActivity.this.getBaseContext(),
                                "Please enter a password with a length greater than 5 containing no spaces",
                                Toast.LENGTH_LONG).show();

                        return false;
                    }

                    if (!PasswordActivity.firstRun) {

                        Log.d(TAG, "Testing password");

                        // Try and open the key store - If it fails, the password is incorrect
                        try {
                            KeyStoreManager keyStoreManager = new KeyStoreManager(PasswordActivity.this, password);
                        } catch (Exception e) {

                            Toast.makeText(PasswordActivity.this.getBaseContext(), "Incorrect password",
                                    Toast.LENGTH_SHORT).show();

                            return false;

                        }

                    }

                    // Create intent used to pass the user's password to the main activity and move there
                    Intent intent = new Intent(PasswordActivity.this, MainActivity.class);

                    Bundle userPassword = new Bundle();

                    intent.putExtra(USER_PASSWORD, userPassword);
                    userPassword.putString("PASSWORD", password);

                    Log.d(TAG, "Starting main activity");

                    startActivity(intent);

                }

                return false;

            }

        });

    }

    /**
     * Reset the user's password by deleting the key store when the button is clicked then reload the activity.
     */
    public void resetPassword(View view) {

        AlertDialog.Builder dialogBox = new AlertDialog.Builder(this);

        dialogBox.setTitle("Are you sure you want to reset your password?");
        dialogBox.setMessage("You'll have to restart your secure conversations.");

        // Add the buttons
        dialogBox.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {

            public void onClick(DialogInterface dialog, int id) {

                KeyStoreManager.deleteKeyStore(PasswordActivity.this.getBaseContext());

                // Put the app back to its first run state
                SharedPreferences.Editor editor = settings.edit();
                editor.putBoolean("firstRun", true);
                editor.commit();

                PasswordActivity.this.setupActivity();

            }

        });

        dialogBox.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {

            public void onClick(DialogInterface dialog, int id) {

            }

        });

        AlertDialog dialog = dialogBox.create();
        dialog.show();

    }


}