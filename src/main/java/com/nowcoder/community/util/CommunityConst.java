package com.nowcoder.community.util;

public interface CommunityConst {
    /**
     * 激活状态
     */
    int ACTIVATION_TRUE = 0;
    int ACTIVATION_REPEAT = 1;
    int ACTIVATION_FAILURE = 2;

    /**
     * 登录数据留存时间
     */
    int DEFAULT_EXPIRED_SECONDS = 3600 * 12;

    int REMEMBER_EXPIRED_SECONDS = 3600 * 24 * 100;




}
