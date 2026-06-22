package cn.ntit.passwordmanager;

/**
 * MainActivity 提供给 Fragment 调用的回调接口。
 */
public interface MainActivityCallback {
    void setFabVisible(boolean visible);
    void setToolbarForVault();
    void setToolbarForTab(String title);
    void setToolbarTitle(String title, boolean showAvatar, boolean showSearch);
    void logout();
}
