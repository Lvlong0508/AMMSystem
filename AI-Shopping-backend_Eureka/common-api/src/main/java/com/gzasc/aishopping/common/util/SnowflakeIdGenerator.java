package com.gzasc.aishopping.common.util;

import cn.hutool.core.lang.Snowflake;
import cn.hutool.core.util.IdUtil;

public class SnowflakeIdGenerator {

    private static final Snowflake SNOWFLAKE = IdUtil.getSnowflake(1, 1);

    public static long nextId() {
        return SNOWFLAKE.nextId();
    }

    public static String nextIdStr() {
        return String.valueOf(SNOWFLAKE.nextId());
    }
}