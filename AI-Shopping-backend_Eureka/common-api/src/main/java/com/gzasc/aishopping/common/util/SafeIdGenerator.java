package com.gzasc.aishopping.common.util;

/**
 * 安全 Long ID 生成器
 * <p>
 * 生成 53 位以内的 Long ID，确保不超出 JavaScript 安全整数范围
 * ({@value #MAX_ID})，前后端互传时无需精度修复。
 * </p>
 *
 * 结构：{@code [41位时间戳差值 | 12位序列号]} = 53位
 * <ul>
 *   <li>时间戳：从 2025-02-20 起算的毫秒数，可持续约 69 年</li>
 *   <li>序列号：每毫秒最多 4096 个 ID，支持高并发</li>
 * </ul>
 *
 * <pre>{@code
 * // 调用方式与 SnowflakeIdGenerator 完全一致：
 * entity.setId(SafeIdGenerator.nextId());          // long
 * entity.setId(Long.valueOf(SafeIdGenerator.nextIdStr()));  // String → long
 * }</pre>
 */
public class SafeIdGenerator {

    /** JS Number.MAX_SAFE_INTEGER = 2^53 - 1 */
    public static final long MAX_ID = 9007199254740991L;

    /** 自定义纪元（2025-02-20T00:00:00Z） */
    private static final long EPOCH = 1740000000000L;

    private static final int SEQUENCE_BITS = 12;
    private static final long MAX_SEQUENCE = (1L << SEQUENCE_BITS) - 1; // 4095

    private long lastTimestamp = -1L;
    private long sequence = 0L;

    // ========== 公开 API ==========

    public static long nextId() {
        return Holder.INSTANCE._nextId();
    }

    public static String nextIdStr() {
        return String.valueOf(nextId());
    }

    // ========== 内部实现 ==========

    private SafeIdGenerator() {}

    /** 静态内部类实现延迟加载的单例模式 */
    private static class Holder {
        static final SafeIdGenerator INSTANCE = new SafeIdGenerator();
    }

    private synchronized long _nextId() {
        long timestamp = System.currentTimeMillis();
        if (timestamp < lastTimestamp) {
            // 时钟回拨：等待追上最后时间戳
            timestamp = waitUntil(lastTimestamp);
        }

        if (timestamp == lastTimestamp) {
            sequence = (sequence + 1) & MAX_SEQUENCE;
            if (sequence == 0) {
                // 当前毫秒序列号耗尽，等待下一毫秒
                timestamp = waitUntil(timestamp);
            }
        } else {
            sequence = 0L;
        }

        lastTimestamp = timestamp;

        long id = ((timestamp - EPOCH) << SEQUENCE_BITS) | sequence;

        // 断言确保不超出安全范围（生产环境可去除）
        assert id <= MAX_ID : "ID exceeds JS safe integer range: " + id;
        return id;
    }

    private long waitUntil(long target) {
        long now = System.currentTimeMillis();
        while (now <= target) {
            now = System.currentTimeMillis();
        }
        return now;
    }
}
