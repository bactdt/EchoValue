package cn.ntit.passwordmanager.util;

import java.util.regex.Pattern;

/**
 * 密码强度评估器。
 * 返回 0-100 的整数分数，并对分数映射到弱/中/强等级。
 */
public final class StrengthEvaluator {

    public static final int LEVEL_NONE = 0;
    public static final int LEVEL_WEAK = 1;
    public static final int LEVEL_MEDIUM = 2;
    public static final int LEVEL_STRONG = 3;

    private static final Pattern UPPER = Pattern.compile("[A-Z]");
    private static final Pattern LOWER = Pattern.compile("[a-z]");
    private static final Pattern DIGIT = Pattern.compile("[0-9]");
    private static final Pattern SYMBOL = Pattern.compile("[^A-Za-z0-9]");

    private StrengthEvaluator() {}

    public static int score(String password) {
        if (password == null || password.isEmpty()) return 0;
        int score = 0;
        int len = password.length();
        if (len > 0) score += 10;
        if (len >= 8) score += 20;
        if (len >= 12) score += 10;
        if (len >= 16) score += 5;
        if (UPPER.matcher(password).find()) score += 15;
        if (LOWER.matcher(password).find()) score += 15;
        if (DIGIT.matcher(password).find()) score += 15;
        if (SYMBOL.matcher(password).find()) score += 15;
        return Math.min(score, 100);
    }

    public static int level(int score) {
        if (score == 0) return LEVEL_NONE;
        if (score < 40) return LEVEL_WEAK;
        if (score < 70) return LEVEL_MEDIUM;
        return LEVEL_STRONG;
    }
}
