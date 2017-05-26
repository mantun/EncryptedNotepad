package enotes.encryptednotepad;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import enotes.doc.Doc;
import enotes.doc.DocException;
import enotes.doc.DocMetadata;
import enotes.doc.DocPasswordException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class EditNoteActivity extends AppCompatActivity {

    private static final String TAG = "ENOTES";
    private static final int ID_OPEN_NOTE = 1872;
    private static final int ID_CREATE_NOTE = 1873;

    private EditText editText;
    private DocMetadata docMeta;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_note);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        editText = (EditText) findViewById(R.id.editText);
        docMeta = new DocMetadata();

        Intent intent = getIntent();
        if (intent != null && intent.getAction().equals(Intent.ACTION_VIEW)) {
            openNote(intent.getData());
        } else {
            chooseDocument();
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
        switch (id) {
            case R.id.action_new:
                docMeta = new DocMetadata();
                editText.getText().clear();
                return true;
            case R.id.action_open:
                chooseDocument();
                return true;
            case R.id.action_save:
                saveDocument();
                return true;
            case R.id.action_save_as:
                createDocument();
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

    private void saveDocument() {
        if (docMeta.key != null && docMeta.filename != null) {
            Doc doc = new Doc(editText.getText().toString(), docMeta);
            saveNote(doc);
        } else {
            createDocument();
        }
    }

    private void createDocument() {
        withPassword("Enter password to encrypt the file:", new Action<String>() {
            @Override
            public boolean execute(String pwd) {
                docMeta.setKey(pwd);
                Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("application/*");
                startActivityForResult(intent, ID_CREATE_NOTE);
                return true;
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == ID_OPEN_NOTE && resultCode == Activity.RESULT_OK) {
            if (data != null) {
                Uri uri = data.getData();
                openNote(uri);
            }
        } else if (requestCode == ID_CREATE_NOTE && resultCode == Activity.RESULT_OK) {
            if (data != null) {
                Uri uri = data.getData();
                docMeta.filename = uri.toString();
                Doc doc = new Doc(editText.getText().toString(), docMeta);
                saveNote(doc);
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void openNote(final Uri uri) {
        withPassword("Enter password:", new Action<String>() {
            @Override
            public boolean execute(String pwd) {
                try (InputStream inputStream = getContentResolver().openInputStream(uri)) {
                    Doc doc = Doc.open(inputStream, pwd);
                    docMeta = doc.getDocMetadata();
                    docMeta.filename = uri.toString();
                    editText.setText(doc.getText());
                    return true;
                } catch (DocPasswordException ignored) {
                    // TODO: ask again?
                    Snackbar.make(editText, "Password is not valid", Snackbar.LENGTH_LONG)
                            .setAction(android.R.string.ok, null).show();
                    return false;
                } catch (IOException | DocException e) {
                    Snackbar.make(editText, e.getMessage(), Snackbar.LENGTH_LONG)
                            .setAction(android.R.string.ok, null).show();
                    Log.e(TAG, e.getMessage(), e);
                    return false;
                }
            }
        });
    }

    private void withPassword(String message, final Action<String> action) {
        AlertDialog.Builder pwDialog = new AlertDialog.Builder(this);
        final EditText edit = new EditText(this);
        edit.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        pwDialog.setView(edit);
        pwDialog.setTitle(message);
        pwDialog.setCancelable(true);
        pwDialog.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int i) {
                String pwd = edit.getText().toString();
                if (pwd != null && !pwd.isEmpty()) {
                    action.execute(pwd);
                }
            }
        });
        pwDialog.setNegativeButton(android.R.string.cancel, null);
        pwDialog.show();
    }

    private void saveNote(Doc doc) {
        try {
            OutputStream out = this.getContentResolver().openOutputStream(Uri.parse(doc.getDocMetadata().filename));
            doc.save(out);
            System.out.println(out);
        } catch (IOException | DocPasswordException e) {
            Snackbar.make(editText, e.getMessage(), Snackbar.LENGTH_LONG)
                    .setAction(android.R.string.ok, null).show();
            Log.e(TAG, e.getMessage(), e);
        }
    }

    private interface Action<T> {
        boolean execute(T arg);
    }
/*    private SparseArray<Action> permissionRequests = new SparseArray<>();
    public void withPermission(String permission, String message, Action action) {
        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, permission)) {
                Snackbar.make(editText, message, Snackbar.LENGTH_LONG).setAction(android.R.string.ok, null).show();
            }
            int reqId = new Random().nextInt(1000);
            permissionRequests.put(reqId, action);
            ActivityCompat.requestPermissions(this, new String[] { permission }, reqId);
        } else {
            action.execute(null);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        Action action = permissionRequests.get(requestCode);
        permissionRequests.remove(requestCode);
        if (action != null && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            action.execute(null);
        }
    }*/

}
