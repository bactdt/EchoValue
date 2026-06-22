package cn.it.cast.keshe.data;

import android.content.Context;

import java.util.List;

import cn.it.cast.keshe.model.Credential;
import cn.it.cast.keshe.util.CryptoUtil;
import cn.it.cast.keshe.util.SessionManager;

/**
 * 凭据仓库：在 SQLite 基础上加一层加解密。
 * 主密码作为 AES 密钥来源，缓存于实例（仅当前会话内有效）。
 */
public class CredentialRepository {

    private final VaultDbHelper helper;
    private final String ownerEmail;
    private char[] masterPassword;

    public CredentialRepository(Context context) {
        this.helper = new VaultDbHelper(context);
        this.ownerEmail = new SessionManager(context).getUserEmail();
    }

    public void setMasterPassword(char[] password) {
        this.masterPassword = password;
    }

    public boolean isReady() {
        return masterPassword != null && masterPassword.length > 0;
    }

    public long saveNew(String name, String username, String plainPassword,
                        String website, String notes) {
        ensureReady();
        Credential cred = new Credential();
        long now = System.currentTimeMillis();
        cred.setName(name);
        cred.setOwnerEmail(requireOwnerEmail());
        cred.setUsername(username);
        cred.setPasswordEncrypted(CryptoUtil.encrypt(plainPassword, masterPassword));
        cred.setWebsite(website);
        cred.setNotes(notes);
        cred.setFavorite(false);
        cred.setCreatedAt(now);
        cred.setUpdatedAt(now);
        return helper.insertCredential(cred);
    }

    public boolean update(long id, String name, String username, String plainPassword,
                          String website, String notes) {
        ensureReady();
        Credential cred = helper.getCredential(id, requireOwnerEmail());
        if (cred == null) return false;
        cred.setName(name);
        cred.setUsername(username);
        cred.setPasswordEncrypted(CryptoUtil.encrypt(plainPassword, masterPassword));
        cred.setWebsite(website);
        cred.setNotes(notes);
        cred.setUpdatedAt(System.currentTimeMillis());
        return helper.updateCredential(cred, requireOwnerEmail()) > 0;
    }

    public boolean delete(long id) {
        return helper.deleteCredential(id, requireOwnerEmail()) > 0;
    }

    public Credential get(long id) {
        Credential cred = helper.getCredential(id, requireOwnerEmail());
        if (cred != null) {
            cred.setPasswordEncrypted(decrypt(cred.getPasswordEncrypted()));
        }
        return cred;
    }

    /**
     * 返回解密密码后的全部凭据（不含明文，已填充解密结果）。
     * 注意：为列表性能考虑，不解密密码字段，详情页使用 get(id) 解密。
     */
    public List<Credential> list() {
        return helper.getAllCredentials(requireOwnerEmail());
    }

    public int count() {
        return helper.countCredentials(requireOwnerEmail());
    }

    public String decrypt(String payload) {
        if (payload == null || payload.isEmpty()) return "";
        ensureReady();
        try {
            return CryptoUtil.decrypt(payload, masterPassword);
        } catch (Exception e) {
            return "";
        }
    }

    private void ensureReady() {
        if (!isReady()) throw new IllegalStateException("CredentialRepository 尚未设置主密码");
    }

    private String requireOwnerEmail() {
        if (ownerEmail == null || ownerEmail.trim().isEmpty()) {
            throw new IllegalStateException("CredentialRepository 尚未设置当前用户");
        }
        return ownerEmail.trim();
    }
}
