package com.example.sqlitedemo;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.sqlitedemo.FeedReaderContract.FeedEntry;

import java.util.ArrayList;
import java.util.List;

/** Démontre les 4 opérations du tutoriel : insérer, lire, supprimer, mettre à jour. */
public class MainActivity extends AppCompatActivity {

    private static final String TAG = "SQLiteDemo";
    private FeedReaderDbHelper dbHelper;
    private TextView output;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        output = findViewById(R.id.output);

        dbHelper = new FeedReaderDbHelper(this);
        StringBuilder log = new StringBuilder();

        // Repartir d'une table vide à chaque lancement (démo)
        dbHelper.getWritableDatabase().delete(FeedEntry.TABLE_NAME, null, null);

        // 1) INSERT
        long id1 = insertEntry("Mon titre", "Mon sous-titre");
        long id2 = insertEntry("SEG2505", "Semaine 6 : SQLite");
        long id3 = insertEntry("Android", "SQLiteOpenHelper");
        log.append("Insertions -> ids : ").append(id1).append(", ")
           .append(id2).append(", ").append(id3).append("\n\n");

        // 2) READ
        log.append("Lecture (title = 'Mon titre') -> ids : ")
           .append(readIdsByTitle("Mon titre")).append("\n");
        log.append("Contenu complet :\n").append(dumpTable()).append("\n");

        // 3) DELETE
        int deleted = deleteByTitle("Android");
        log.append("Suppression (title LIKE 'Android') -> lignes supprimées : ")
           .append(deleted).append("\n\n");

        // 4) UPDATE
        int updated = updateTitle("Mon titre", "Mon nouveau titre");
        log.append("Mise à jour 'Mon titre' -> 'Mon nouveau titre' : ")
           .append(updated).append(" ligne(s)\n\n");

        log.append("Contenu final :\n").append(dumpTable());

        output.setText(log.toString());
        Log.d(TAG, log.toString());
    }

    /** Insertion avec ContentValues ; retourne l'id de la nouvelle ligne (-1 si erreur). */
    private long insertEntry(String title, String subtitle) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(FeedEntry.COLUMN_NAME_TITLE, title);
        values.put(FeedEntry.COLUMN_NAME_SUBTITLE, subtitle);
        return db.insert(FeedEntry.TABLE_NAME, null, values);
    }

    /** Lecture avec query() : projection, sélection, arguments, tri. */
    private List<Long> readIdsByTitle(String title) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String[] projection = {
                FeedEntry._ID,
                FeedEntry.COLUMN_NAME_TITLE,
                FeedEntry.COLUMN_NAME_SUBTITLE
        };
        String selection = FeedEntry.COLUMN_NAME_TITLE + " = ?";
        String[] selectionArgs = { title };
        String sortOrder = FeedEntry.COLUMN_NAME_SUBTITLE + " DESC";

        List<Long> itemIds = new ArrayList<>();
        try (Cursor cursor = db.query(FeedEntry.TABLE_NAME, projection,
                selection, selectionArgs, null, null, sortOrder)) {
            while (cursor.moveToNext()) {
                itemIds.add(cursor.getLong(cursor.getColumnIndexOrThrow(FeedEntry._ID)));
            }
        }
        return itemIds;
    }

    /** Suppression : les « ? » protègent contre l'injection SQL. */
    private int deleteByTitle(String title) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        String selection = FeedEntry.COLUMN_NAME_TITLE + " LIKE ?";
        String[] selectionArgs = { title };
        return db.delete(FeedEntry.TABLE_NAME, selection, selectionArgs);
    }

    /** Mise à jour d'une valeur existante. */
    private int updateTitle(String oldTitle, String newTitle) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(FeedEntry.COLUMN_NAME_TITLE, newTitle);
        String selection = FeedEntry.COLUMN_NAME_TITLE + " LIKE ?";
        String[] selectionArgs = { oldTitle };
        return db.update(FeedEntry.TABLE_NAME, values, selection, selectionArgs);
    }

    /** Affiche toutes les lignes de la table (pour les captures d'écran). */
    private String dumpTable() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        StringBuilder sb = new StringBuilder();
        try (Cursor c = db.query(FeedEntry.TABLE_NAME, null, null, null, null, null,
                FeedEntry._ID + " ASC")) {
            while (c.moveToNext()) {
                sb.append("  [").append(c.getLong(c.getColumnIndexOrThrow(FeedEntry._ID)))
                  .append("] ").append(c.getString(c.getColumnIndexOrThrow(FeedEntry.COLUMN_NAME_TITLE)))
                  .append(" — ").append(c.getString(c.getColumnIndexOrThrow(FeedEntry.COLUMN_NAME_SUBTITLE)))
                  .append("\n");
            }
        }
        return sb.toString();
    }


    /** Connexion persistante : on ferme le helper quand l'activité est détruite. */
    @Override
    protected void onDestroy() {
        dbHelper.close();
        super.onDestroy();
    }
}
