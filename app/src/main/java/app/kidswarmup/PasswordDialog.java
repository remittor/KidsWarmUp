package app.kidswarmup;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.DialogInterface;
import android.content.Intent;
import android.text.InputType;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

public class PasswordDialog {

    private static final String TAG = PasswordDialog.class.getSimpleName();

    public static void show(Context ctx, String pwd, int requestCode) {
        AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
        builder.setTitle("Enter password");
        // Set up the input
        final EditText input = new EditText(ctx);
        // Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        builder.setView(input);
        // Set up the buttons on dialog
        builder.setPositiveButton("Enter", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String txt = input.getText().toString();
                InputMethodManager imm = (InputMethodManager) ctx.getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(input.getWindowToken(), 0);
                dialog.dismiss();
                if (txt.equals(pwd)) {
                    Activity mainActivity = getActivity(ctx);
                    Intent i = new Intent(mainActivity, SettingsActivity.class);
                    mainActivity.startActivityForResult(i, requestCode);
                } else {
                    Toast.makeText(ctx, R.string.incorrect_password, Toast.LENGTH_LONG).show();
                }
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                InputMethodManager imm = (InputMethodManager) ctx.getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(input.getWindowToken(), 0);
                dialog.cancel();
            }
        });

        final AlertDialog dialog = builder.show();
        // Set up the actions into textedit
        input.setOnKeyListener(new EditText.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (event.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {
                    Button btn_enter = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
                    btn_enter.callOnClick();
                    return true;
                }
                return false;
            }
        });

        input.requestFocus();
        InputMethodManager imm = (InputMethodManager) ctx.getSystemService(ctx.INPUT_METHOD_SERVICE);
        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
    }

    public static Activity getActivity(Context context) {
        if (context == null)
            return null;
        if (context instanceof ContextWrapper) {
            if (context instanceof Activity)
                return (Activity) context;
            return getActivity(((ContextWrapper) context).getBaseContext());
        }
        return null;
    }

}
