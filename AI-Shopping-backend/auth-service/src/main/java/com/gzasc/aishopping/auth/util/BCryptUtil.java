package com.gzasc.aishopping.auth.util;

import org.mindrot.jbcrypt.BCrypt;
import org.springframework.stereotype.Component;

/**
 * BCrypt 加盐加密工具类
 * 
 * BCrypt 特点：
 * 1. 自动生成随机 Salt，无需手动管理
 * 2. Salt 嵌入在密文开头，格式: $2a$12$22位盐值+31位密文
 * 3. 可通过 strength 调整计算强度（默认 10，推荐 12）
 */
@Component
public class BCryptUtil {

    /**
     * 加密强度（2^12 次迭代）
     * 数值越大安全性越高，但计算耗时越长
     * 12 表示约 300ms 的计算时间
     */
    private static final int STRENGTH = 12;

    /**
     * 加密密码（自动生成随机 Salt）
     * 
     * 示例：
     * 明文: "123456"
     * 密文: "$2a$12$N9qo8uLOickgx2ZMRZoMy.MqrWXfNzC5Gt2JXKJcHKV9lpW0S6/4C"
     *       └─┬─┘└──────┬────────┘└──────────────────┬─────────────────────┘
     *        版本      强度因子(12)                盐值(22字符)+密文(31字符)
     *
     * @param plainPassword 明文密码
     * @return 包含 Salt 的密文
     */
    public static String hashPassword(String plainPassword) {
        return BCrypt.hashpw(plainPassword, BCrypt.gensalt(STRENGTH));
    }

    /**
     * 验证密码
     * 
     * BCrypt.checkpw 会自动从 storedHash 中提取 Salt，
     * 然后用该 Salt 对 plainPassword 进行哈希，最后比对结果
     *
     * @param plainPassword  明文密码（用户输入）
     * @param storedHash     数据库中存储的密文（含 Salt）
     * @return true=密码正确，false=密码错误
     */
    public static boolean verifyPassword(String plainPassword, String storedHash) {
        if (plainPassword == null || storedHash == null) {
            return false;
        }
        return BCrypt.checkpw(plainPassword, storedHash);
    }

    /**
     * 检查密码格式是否合法（用于前端校验提示）
     * 
     * @param password 明文密码
     * @return true=格式合法
     */
    public static boolean isValidPasswordFormat(String password) {
        if (password == null || password.length() < 6 || password.length() > 20) {
            return false;
        }
        // 至少包含一个字母和一个数字
        boolean hasLetter = password.matches(".*[a-zA-Z].*");
        boolean hasDigit = password.matches(".*\\d.*");
        return hasLetter && hasDigit;
    }

    /**
     * 检查用户名格式是否合法
     * 
     * @param username 用户名
     * @return true=格式合法
     */
    public static boolean isValidUsernameFormat(String username) {
        if (username == null || username.length() < 3 || username.length() > 20) {
            return false;
        }
        // 只允许字母、数字、下划线
        return username.matches("^[a-zA-Z0-9_]+$");
    }
}
