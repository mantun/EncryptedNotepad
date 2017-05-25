package enotes.encryptednotepad;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import enotes.doc.Doc;
import enotes.doc.DocException;
import enotes.doc.DocPasswordException;

import java.io.IOException;
import java.io.InputStream;

public class EditNoteActivity extends AppCompatActivity {

    private static final int ID_OPEN_NOTE = 1872;

    private EditText editText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_note);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });
        editText = (EditText) findViewById(R.id.editText);
        Intent intent = getIntent();
        if (intent != null && intent.getAction().equals(Intent.ACTION_VIEW)) {
            openNote(intent.getData());
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_edit_note, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_open) {
            chooseDocument();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void chooseDocument() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/*");
        startActivityForResult(intent, ID_OPEN_NOTE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == ID_OPEN_NOTE && resultCode == Activity.RESULT_OK) {
            if (data != null) {
                Uri uri = data.getData();
                openNote(uri);
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void openNote(final Uri uri) {
        AlertDialog.Builder pwDialog = new AlertDialog.Builder(this);
        final EditText edit = new EditText(this);
        edit.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        pwDialog.setView(edit);
        pwDialog.setTitle("Enter password:");
        pwDialog.setCancelable(true);
        pwDialog.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int i) {
                String pwd = edit.getText().toString();
                if (pwd != null && !pwd.isEmpty()) {
                    try (InputStream inputStream = getContentResolver().openInputStream(uri)) {
                        Doc doc = Doc.open(inputStream, pwd);
                        editText.setText(doc.getText());
                        dialog.dismiss();
                    } catch (DocPasswordException ignored) {
                        // edit.setText(""); - the dialog is being dismissed anyway
                    } catch (IOException | DocException e) {
                        Log.e("ENOTES", e.getMessage(), e);
                        dialog.dismiss();
                    }
                }
            }
        });
        pwDialog.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int i) {
                dialog.cancel();
            }
        });
        pwDialog.show();
    }

}
