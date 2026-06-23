package cn.it.cast.keshe.service;

import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentSender;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.service.autofill.AutofillService;
import android.service.autofill.Dataset;
import android.service.autofill.FillCallback;
import android.service.autofill.FillContext;
import android.service.autofill.FillRequest;
import android.service.autofill.FillResponse;
import android.service.autofill.SaveCallback;
import android.service.autofill.SaveRequest;
import android.view.autofill.AutofillId;
import android.view.autofill.AutofillValue;
import android.widget.RemoteViews;

import androidx.annotation.NonNull;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import cn.it.cast.keshe.MainActivity;
import cn.it.cast.keshe.R;
import cn.it.cast.keshe.VaultApp;
import cn.it.cast.keshe.data.CredentialRepository;
import cn.it.cast.keshe.model.Credential;
import cn.it.cast.keshe.util.SessionManager;

public class EchoValueAutofillService extends AutofillService {

    private static final int MAX_DATASETS = 10;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    public void onFillRequest(@NonNull FillRequest request, @NonNull CancellationSignal cancellation,
                              @NonNull FillCallback callback) {
        List<FillContext> contexts = request.getFillContexts();
        if (contexts.isEmpty()) {
            callback.onSuccess(null);
            return;
        }
        FillContext context = contexts.get(contexts.size() - 1);
        android.app.assist.AssistStructure structure = context.getStructure();
        String packageName = structure.getActivityComponent() == null
                ? null : structure.getActivityComponent().getPackageName();
        AutofillFieldCollector fields = new AutofillFieldCollector();
        collectAutofillFields(structure, fields);

        if (fields.usernameId == null && fields.passwordId == null) {
            callback.onSuccess(null);
            return;
        }

        if (!VaultApp.isMasterPasswordAvailable()) {
            callback.onSuccess(buildUnlockResponse(fields));
            return;
        }

        final AutofillFieldCollector fFields = fields;
        final String fPackage = packageName;
        executor.execute(() -> {
            try {
                FillResponse response = buildFillResponse(fPackage, fFields);
                callback.onSuccess(response);
            } catch (Exception e) {
                callback.onSuccess(null);
            }
        });
    }

    private FillResponse buildUnlockResponse(AutofillFieldCollector fields) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pi = PendingIntent.getActivity(this, 0, intent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        IntentSender sender = pi.getIntentSender();

        RemoteViews presentation = new RemoteViews(getPackageName(), R.layout.autofill_dataset);
        presentation.setTextViewText(R.id.autofill_dataset_title, getString(R.string.autofill_unlock_dataset));
        presentation.setImageViewResource(R.id.autofill_dataset_icon, R.drawable.ic_lock);

        Dataset.Builder builder = new Dataset.Builder(presentation);
        if (fields.usernameId != null) {
            builder.setValue(fields.usernameId, AutofillValue.forText(""));
        }
        if (fields.passwordId != null) {
            builder.setValue(fields.passwordId, AutofillValue.forText(""));
        }
        builder.setAuthentication(sender);
        return new FillResponse.Builder().addDataset(builder.build()).build();
    }

    private FillResponse buildFillResponse(String packageName, AutofillFieldCollector fields) {
        SessionManager session = new SessionManager(this);
        if (!session.isLoggedIn()) return null;

        CredentialRepository repo = new CredentialRepository(this);
        repo.setMasterPassword(VaultApp.getMasterPassword());
        List<Credential> all = repo.list();

        FillResponse.Builder responseBuilder = new FillResponse.Builder();
        int count = 0;
        for (Credential cred : all) {
            if (count >= MAX_DATASETS) break;
            if (!matches(cred, packageName)) continue;

            String password = repo.decrypt(cred.getPasswordEncrypted());
            String username = cred.getUsername() == null ? "" : cred.getUsername();

            RemoteViews presentation = new RemoteViews(getPackageName(), R.layout.autofill_dataset);
            String label = cred.getName();
            if (!username.isEmpty()) {
                label += " · " + username;
            }
            presentation.setTextViewText(R.id.autofill_dataset_title, label);
            presentation.setImageViewResource(R.id.autofill_dataset_icon, R.drawable.ic_person);

            Dataset.Builder builder = new Dataset.Builder(presentation);
            if (fields.usernameId != null) {
                builder.setValue(fields.usernameId, AutofillValue.forText(username));
            }
            if (fields.passwordId != null) {
                builder.setValue(fields.passwordId, AutofillValue.forText(password));
            }
            responseBuilder.addDataset(builder.build());
            count++;
        }
        return count == 0 ? null : responseBuilder.build();
    }

    private boolean matches(Credential cred, String packageName) {
        String website = cred.getWebsite();
        if (website == null || website.trim().isEmpty()) return false;
        String w = website.trim().toLowerCase();
        String p = packageName == null ? "" : packageName.toLowerCase();
        if (w.isEmpty() || p.isEmpty()) return false;
        return w.contains(p) || p.contains(w);
    }

    @Override
    public void onSaveRequest(@NonNull SaveRequest request, @NonNull SaveCallback callback) {
        if (!VaultApp.isMasterPasswordAvailable()) {
            callback.onSuccess();
            return;
        }
        List<FillContext> contexts = request.getFillContexts();
        if (contexts.isEmpty()) {
            callback.onSuccess();
            return;
        }
        FillContext context = contexts.get(contexts.size() - 1);
        android.app.assist.AssistStructure structure = context.getStructure();
        String packageName = structure.getActivityComponent() == null
                ? null : structure.getActivityComponent().getPackageName();
        AutofillFieldCollector fields = new AutofillFieldCollector();
        collectAutofillFields(structure, fields);

        String username = fields.usernameValue == null ? "" : fields.usernameValue;
        String password = fields.passwordValue == null ? "" : fields.passwordValue;
        if (username.isEmpty() && password.isEmpty()) {
            callback.onSuccess();
            return;
        }

        try {
            SessionManager session = new SessionManager(this);
            if (!session.isLoggedIn()) {
                callback.onSuccess();
                return;
            }
            CredentialRepository repo = new CredentialRepository(this);
            repo.setMasterPassword(VaultApp.getMasterPassword());
            repo.saveNew(packageName, username, password, packageName, "");
        } catch (Exception e) {
            // 保存失败不影响系统行为
        }
        callback.onSuccess();
    }

    private void collectAutofillFields(android.app.assist.AssistStructure structure,
                                       AutofillFieldCollector out) {
        for (int i = 0; i < structure.getWindowNodeCount(); i++) {
            walk(structure.getWindowNodeAt(i).getRootViewNode(), out);
        }
    }

    private void walk(android.app.assist.AssistStructure.ViewNode node, AutofillFieldCollector out) {
        String[] hints = node.getAutofillHints();
        if (hints != null) {
            for (String hint : hints) {
                if (hint == null) continue;
                String h = hint.toLowerCase();
                if (h.contains("username") || h.contains("email")) {
                    out.usernameId = node.getAutofillId();
                    captureValue(node, out, true);
                } else if (h.contains("password")) {
                    out.passwordId = node.getAutofillId();
                    captureValue(node, out, false);
                }
            }
        }
        // 无 hint 时按 inputType 兜底
        if (out.passwordId == null && isPasswordType(node)) {
            out.passwordId = node.getAutofillId();
            captureValue(node, out, false);
        }
        for (int i = 0; i < node.getChildCount(); i++) {
            walk(node.getChildAt(i), out);
        }
    }

    private void captureValue(android.app.assist.AssistStructure.ViewNode node,
                              AutofillFieldCollector out, boolean isUsername) {
        AutofillValue v = node.getAutofillValue();
        if (v != null && v.isText()) {
            if (isUsername) out.usernameValue = v.getTextValue().toString();
            else out.passwordValue = v.getTextValue().toString();
        }
    }

    private boolean isPasswordType(android.app.assist.AssistStructure.ViewNode node) {
        int type = node.getAutofillType();
        // TYPE_TEXT == 1, 且 inputType 含 password 标志
        return type == 1 && (node.getInputType() & 0x80) != 0; // InputType.TYPE_MASK_VARIATION & TYPE_TEXT_VARIATION_PASSWORD
    }

    private static class AutofillFieldCollector {
        AutofillId usernameId;
        AutofillId passwordId;
        String usernameValue;
        String passwordValue;
    }

    @Override
    public void onConnected() {}

    @Override
    public void onDisconnected() {
        executor.shutdownNow();
    }
}
