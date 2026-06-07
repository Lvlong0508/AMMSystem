package com.gzasc.aishopping.common.util;

import java.util.UUID;

/**
 * UUID ID 生成器
 * <p>
 * 生成 32 位无连字符的 UUID 字符串，用于替换雪花算法 ID。
 * 相比 SnowflakeIdGenerator（返回 long），UUID 不会超出 JS 安全整数范围，
 * 前后端传递时无需精度修复。
 * </p>
 *
 * <pre>{@code
 * // 替换前（雪花算法）：
 * entity.setId(SnowflakeIdGenerator.nextId());       // long
 *
 * // 替换后（UUID）：
 * entity.setId(UuidGenerator.nextId());              // String
 * }</pre>
 */
public class UuidGenerator {

    /**
     * 生成 UUID 字符串（32 位，无连字符）
     * <p>
     * 示例：{@code 550e8400e29b41d4a716446655440000}
     *
     * @return UUID 字符串
     */
    public static String nextId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * 生成带连字符的标准 UUID 字符串（36 位）
     * <p>
     * 示例：{@code 550e8400-e29b-41d4-a716-446655440000}
     *
     * @return 标准 UUID 字符串
     */
    public static String nextUuid() {
        return UUID.randomUUID().toString();
    }
}
