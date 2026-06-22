package cn.it.cast.keshe.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import cn.it.cast.keshe.R;
import cn.it.cast.keshe.data.CredentialRepository;
import cn.it.cast.keshe.model.Credential;
import cn.it.cast.keshe.util.ClipboardUtil;

public class CredentialAdapter extends RecyclerView.Adapter<CredentialAdapter.VH> {

    public interface OnItemClickListener {
        void onClick(Credential cred);
    }

    private final List<Credential> items;
    private final CredentialRepository repository;
    private final OnItemClickListener listener;
    private int selectedPosition = -1;

    public CredentialAdapter(List<Credential> items, CredentialRepository repository, OnItemClickListener listener) {
        this.items = items;
        this.repository = repository;
        this.listener = listener;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_credential, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        Credential c = items.get(position);
        holder.nameText.setText(c.getName());
        holder.usernameText.setText(c.getUsername());

        char firstLetter = (c.getName() != null && !c.getName().isEmpty())
                ? Character.toUpperCase(c.getName().charAt(0)) : '?';
        holder.iconLetter.setText(String.valueOf(firstLetter));

        // 警告图标：弱密码
        // 注：为列表性能不解密密码，使用名称长度作为大致信号；详情页才真实评估

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onClick(c);
        });

        holder.copyBtn.setOnClickListener(v -> {
            // 拷贝明文密码（解密）
            String plain = repository.decrypt(c.getPasswordEncrypted());
            Context ctx = v.getContext();
            ClipboardUtil.copyPassword(ctx, plain);
            Toast.makeText(ctx, R.string.detail_password_copied, Toast.LENGTH_SHORT).show();

            // 视觉反馈
            holder.copyBtn.setImageResource(R.drawable.ic_check);
            holder.copyBtn.postDelayed(() -> holder.copyBtn.setImageResource(R.drawable.ic_content_copy), 1500);
        });
    }

    @Override
    public int getItemCount() {
        return items == null ? 0 : items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView iconLetter;
        TextView nameText;
        TextView usernameText;
        ImageButton copyBtn;

        VH(@NonNull View itemView) {
            super(itemView);
            iconLetter = itemView.findViewById(R.id.icon_letter);
            nameText = itemView.findViewById(R.id.name_text);
            usernameText = itemView.findViewById(R.id.username_text);
            copyBtn = itemView.findViewById(R.id.copy_btn);
        }
    }
}
