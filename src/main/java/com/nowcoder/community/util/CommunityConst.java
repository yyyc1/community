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

    /**
     *实体类型：帖子
     */
    int ENTITY_TYPE_POST = 1;

    /**
     * 实体类型：评论
     */
    int ENTITY_TYPE_COMMENT = 2;

    /**
     * 实体类型：用户
     */
    int ENTITY_TYPE_USER = 3;

    /**
     * topic类型：发布帖子
     */
    String TOPIC_PUBLISH = "publish";



    /**
     * topic类型：发表评论
     */
    String TOPIC_COMMENT = "comment";

    /**
     * topic类型：点赞
     */
    String TOPIC_LIKE = "like";

    /**
     * topic类型：关注
     */
    String TOPIC_FOLLOW = "follow";

    /**
     * topic类型：删帖
     */
    String TOPIC_DELETE = "delete";

    /**
     * 系统用户id
     */
    int SYSTEM_USER_ID = 1;

    String AUTHORITY_USER = "ROLE_USER";
    String AUTHORITY_ADMIN = "ROLE_ADMIN";
    String AUTHORITY_MODERATOR = "ROLE_MODERATOR";
    String AUTHORITY_UNKNOWN = "ROLE_UNKNOWN";

    long CODE_EXPIRE_SECONDS = 300;

    // ===================== 核心指标名称 =====================
    String BIZ_OPERATE_TOTAL = "forum.biz.operate.total";   // 业务操作计数
    String CORE_API_DURATION = "forum.core.api.duration";   // 核心接口耗时
    String USER_ONLINE_COUNT = "forum.user.online.count";   // 在线用户数

    // ===================== 标签名 =====================
    String TAG_OPERATE_TYPE = "operate_type"; // 操作类型
    String TAG_RESULT = "result";             // 操作结果
    String TAG_USER_TYPE = "user_type";       // 用户类型
    String TAG_API_TYPE = "api_type";         // 接口类型

    // ===================== 标签值 - 操作类型 =====================
    String OPERATE_TYPE_POST_CREATE = "post_create";
    String OPERATE_TYPE_COMMENT_CREATE = "comment_create";
    String OPERATE_TYPE_LIKE = "like";
    String OPERATE_TYPE_LOGIN = "login";

    // ===================== 标签值 - 操作结果 =====================
    String RESULT_SUCCESS = "success";
    String RESULT_FAIL = "fail";

    // ===================== 标签值 - 用户类型 =====================
    String USER_TYPE_ADMIN = "admin";
    String USER_TYPE_NORMAL = "normal";

    // ===================== 标签值 - 接口类型 =====================
    String API_TYPE_POST_CREATE = "post_create";
    String API_TYPE_POST_QUERY = "post_query";
    String API_TYPE_COMMENT_CREATE = "comment_create";

}
