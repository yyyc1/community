package com.nowcoder.community.util;

public class RedisUtil {

    private static final String SPLIT = ":";
    private static final String PRE_TICKET = "ticket";
    private static final String PRE_POST = "post";
    private static final String PRE_ENTITY_LIKE = "like:entity";
    private static final String PRE_USER_LIKE = "like:user";
    private static final String PRE_FOLLOWEE = "followee";
    private static final String PRE_FOLLOWER = "follower";
    private static final String PRE_FORGET = "forget:code";
    private static final String PRE_CAPTCHA = "captcha";

    public static String getTicketKey(String ticket){
        return PRE_TICKET + SPLIT + ticket;
    }

    public static String getPostScoreKey(){
        return PRE_POST + SPLIT + "score";
    }

    public static String getEntityLikeKey(int entityType, int entityId){return PRE_ENTITY_LIKE + SPLIT + + entityType + SPLIT + entityId;}

    public static String getUserLikeKey(int userId) {
        return PRE_USER_LIKE + SPLIT + userId;
    }

    public static String getFolloweeKey(int entityType, int entityId) {
        return PRE_FOLLOWEE + SPLIT + entityType + SPLIT + entityId;
    }
    public static String getFollowerKey(int entityType, int entityId) {
        return PRE_FOLLOWER + SPLIT + entityType + SPLIT + entityId;
    }

    public static String getForgetCodeKey(String email) {
        return PRE_FORGET + SPLIT + email;
    }

    public static String getLoginCode(String captchaId) {
        return PRE_CAPTCHA + SPLIT + captchaId;
    }
}
