package cn.it.cast.keshe;

/**
 * MainActivity 提供给 Fragment 的回调接口。
 */
public interface MainActivityCallback {
    void setFabVisible(boolean visible);
    void setToolbarTitle(String title, boolean showSearch, boolean showAvatar);
    void logout();
}