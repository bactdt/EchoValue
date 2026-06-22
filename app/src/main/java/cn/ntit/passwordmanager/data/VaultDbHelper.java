package cn.ntit.passwordmanager.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

import cn.ntit.passwordmanager.model.Credential;
import cn.ntit.passwordmanager.model.UserAccount;
import cn.ntit.passwordmanager.util.SessionManager;

public class VaultDbHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "vault.db";
    private static final int DB_VERSION = 2;

    private static final String TABLE_USERS = "users";
    private static final String TABLE_CREDS = "credentials";
    private final Context appContext;

    public VaultDbHelper(@Nullable Context context) {
        super(context, DB_NAME, null, DB_VERSION);
        appContext = context == null ? null : context.getApplicationContext();
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE_USERS + " (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "name TEXT NOT NULL, " +
                "email TEXT UNIQUE NOT NULL, " +
                "password_hash TEXT NOT NULL, " +
                "password_salt TEXT NOT NULL, " +
                "pin_hash TEXT, " +
                "created_at INTEGER NOT NULL)");

        db.execSQL("CREATE TABLE " + TABLE_CREDS + " (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "owner_email TEXT NOT NULL, " +
                "name TEXT NOT NULL, " +
                "username TEXT, " +
                "password_encrypted TEXT, " +
                "website TEXT, " +
                "notes TEXT, " +
                "favorite INTEGER DEFAULT 0, " +
                "created_at INTEGER NOT NULL, " +
                "updated_at INTEGER NOT NULL)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            migrateToVersion2(db);
        }
    }

    private void migrateToVersion2(SQLiteDatabase db) {
        db.execSQL("ALTER TABLE " + TABLE_CREDS +
                " ADD COLUMN owner_email TEXT NOT NULL DEFAULT ''");

        String email = getCurrentSessionEmail();
        if (!email.isEmpty()) {
            ContentValues cv = new ContentValues();
            cv.put("owner_email", email);
            db.update(TABLE_CREDS, cv, "owner_email = ''", null);
        }
    }

    private String getCurrentSessionEmail() {
        if (appContext == null) return "";
        try {
            String email = new SessionManager(appContext).getUserEmail();
            return email == null ? "" : email.trim();
        } catch (Exception e) {
            return "";
        }
    }

    // ===== 用户 =====

    public long insertUser(UserAccount user) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("name", user.getName());
        cv.put("email", user.getEmail());
        cv.put("password_hash", user.getPasswordHash());
        cv.put("password_salt", user.getPasswordSalt());
        cv.put("pin_hash", user.getPinHash());
        cv.put("created_at", user.getCreatedAt());
        return db.insert(TABLE_USERS, null, cv);
    }

    @Nullable
    public UserAccount findUserByEmail(String email) {
        SQLiteDatabase db = getReadableDatabase();
        try (Cursor c = db.query(TABLE_USERS, null, "email = ?", new String[]{email}, null, null, null)) {
            if (c != null && c.moveToFirst()) {
                return readUser(c);
            }
        }
        return null;
    }

    /**
     * Updates a PIN hash stored as "hash:salt"; callers must create a dedicated PIN salt.
     */
    public int updateUserPin(String email, String pinHash) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("pin_hash", pinHash);
        return db.update(TABLE_USERS, cv, "email = ?", new String[]{email});
    }

    private UserAccount readUser(Cursor c) {
        UserAccount u = new UserAccount();
        u.setId(c.getLong(c.getColumnIndexOrThrow("id")));
        u.setName(c.getString(c.getColumnIndexOrThrow("name")));
        u.setEmail(c.getString(c.getColumnIndexOrThrow("email")));
        u.setPasswordHash(c.getString(c.getColumnIndexOrThrow("password_hash")));
        u.setPasswordSalt(c.getString(c.getColumnIndexOrThrow("password_salt")));
        int idx = c.getColumnIndex("pin_hash");
        if (idx >= 0) u.setPinHash(c.getString(idx));
        u.setCreatedAt(c.getLong(c.getColumnIndexOrThrow("created_at")));
        return u;
    }

    // ===== 凭据 =====

    public long insertCredential(Credential cred) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = credToValues(cred);
        return db.insert(TABLE_CREDS, null, cv);
    }

    public int updateCredential(Credential cred, String ownerEmail) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = credToValues(cred);
        return db.update(TABLE_CREDS, cv, "id = ? AND owner_email = ?",
                new String[]{String.valueOf(cred.getId()), ownerEmail});
    }

    public int deleteCredential(long id, String ownerEmail) {
        SQLiteDatabase db = getWritableDatabase();
        return db.delete(TABLE_CREDS, "id = ? AND owner_email = ?",
                new String[]{String.valueOf(id), ownerEmail});
    }

    @Nullable
    public Credential getCredential(long id, String ownerEmail) {
        SQLiteDatabase db = getReadableDatabase();
        try (Cursor c = db.query(TABLE_CREDS, null, "id = ? AND owner_email = ?",
                new String[]{String.valueOf(id), ownerEmail}, null, null, null)) {
            if (c != null && c.moveToFirst()) return readCred(c);
        }
        return null;
    }

    public List<Credential> getAllCredentials(String ownerEmail) {
        List<Credential> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        try (Cursor c = db.query(TABLE_CREDS, null, "owner_email = ?",
                new String[]{ownerEmail}, null, null, "updated_at DESC")) {
            while (c.moveToNext()) list.add(readCred(c));
        }
        return list;
    }

    public int countCredentials(String ownerEmail) {
        SQLiteDatabase db = getReadableDatabase();
        try (Cursor c = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_CREDS +
                " WHERE owner_email = ?", new String[]{ownerEmail})) {
            if (c.moveToFirst()) return c.getInt(0);
        }
        return 0;
    }

    private ContentValues credToValues(Credential cred) {
        ContentValues cv = new ContentValues();
        cv.put("owner_email", cred.getOwnerEmail());
        cv.put("name", cred.getName());
        cv.put("username", cred.getUsername());
        cv.put("password_encrypted", cred.getPasswordEncrypted());
        cv.put("website", cred.getWebsite());
        cv.put("notes", cred.getNotes());
        cv.put("favorite", cred.isFavorite() ? 1 : 0);
        cv.put("created_at", cred.getCreatedAt());
        cv.put("updated_at", cred.getUpdatedAt());
        return cv;
    }

    private Credential readCred(Cursor c) {
        Credential cred = new Credential();
        cred.setId(c.getLong(c.getColumnIndexOrThrow("id")));
        cred.setName(c.getString(c.getColumnIndexOrThrow("name")));
        int idx = c.getColumnIndex("owner_email");
        if (idx >= 0) cred.setOwnerEmail(c.getString(idx));
        idx = c.getColumnIndex("username");
        if (idx >= 0) cred.setUsername(c.getString(idx));
        idx = c.getColumnIndex("password_encrypted");
        if (idx >= 0) cred.setPasswordEncrypted(c.getString(idx));
        idx = c.getColumnIndex("website");
        if (idx >= 0) cred.setWebsite(c.getString(idx));
        idx = c.getColumnIndex("notes");
        if (idx >= 0) cred.setNotes(c.getString(idx));
        idx = c.getColumnIndex("favorite");
        if (idx >= 0) cred.setFavorite(c.getInt(idx) == 1);
        cred.setCreatedAt(c.getLong(c.getColumnIndexOrThrow("created_at")));
        cred.setUpdatedAt(c.getLong(c.getColumnIndexOrThrow("updated_at")));
        return cred;
    }
}
