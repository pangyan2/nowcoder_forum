package com.nowcoder.community.util;

/**
 * RedisKey生成工具
 * @author Alex
 * @version 1.0
 * @date 2022/2/13 15:42
 */
public class RedisKeyUtil {

    private static final String SPLIT = ":";
    private static final String PREFIX_ENTITY_LIKE="like:entity";
    private static final String PREFIX_USER_LIKE="like:user";
    private static final String PREFIX_FOLLOWER="follower";
    private static final String PREFIX_FOLLOWEE="followee";
    private static final String PREFIX_KAPTCHA="kaptcha";
    private static final String PREFIX_TICKET="ticket";
    private static final String PREFIX_USER="user";
    private static final String PREFIX_POST="post";


    /**
     * 统计网站独立访客数量key
     */
    private static final String PREFIX_UV="uv";
    /**
     * 统计网站独立活跃用户数量key
     */
    private static final String PREFIX_UAV="uav";


    private RedisKeyUtil(){

    }

    /**
     * 生成某个实体的赞的RedisKey
     *  like:entity:entityType:entityId  ==> set(userId)
     * @param entityType
     * @param entityId
     * @return
     */
    public static String getEntityLikeKey(int entityType,int entityId){
        return PREFIX_ENTITY_LIKE + SPLIT + entityType + SPLIT + entityId;
    }

    /**
     * 某个用户的赞
     *  like:user:userId -> int
     * @param userId
     * @return
     */
    public static String getUserLikeKey(int userId){
        return PREFIX_USER_LIKE + SPLIT + userId;
    }

    /**
     * 某个用户关注的实体
     *   followee:userId:entityType -> zset(entityId,now())
     *   使用zset便于排序，使用now当前时间作为score，从而进行排序
     * @param userId
     * @param entityType
     * @return
     */
    public static String getFolloweeKey(int userId,int entityType){
        return PREFIX_FOLLOWEE + SPLIT +userId +SPLIT +entityType;
    }

    /**
     * 某个实体拥有的粉丝
     *  follower:entityType:entityId ->zset(userId,now)
     * @param entityType
     * @param entityId
     * @return
     */
    public static String getFollowerKey(int entityType,int entityId){
        return PREFIX_FOLLOWER + SPLIT +entityType + SPLIT + entityId;
    }

    /**
     * 登录验证码key
     * @param owner
     * @return
     */
    public static String getKaptchaKey(String owner){
        return PREFIX_KAPTCHA + SPLIT + owner;
    }

    /**
     * 登录凭证key
     * @param ticket
     * @return
     */
    public static String getTicketKey(String ticket){
        return PREFIX_TICKET + SPLIT + ticket;
    }

    /**
     * 用户信息key
     * @param userId
     * @return
     */
    public static String getUserKey(int userId){
        return PREFIX_USER + SPLIT +userId;
    }

    /**
     * 单日uv
     * @param date
     * @return
     */
    public static String getDailyUvKey(String date){
        return PREFIX_UV + SPLIT + date;
    }

    /**
     * 获取区间范围的uv
     * @param startDate
     * @param endDate
     * @return
     */
    public static String getRangeUv(String startDate,String endDate){
        return PREFIX_UV + SPLIT + startDate + SPLIT + endDate;
    }

    /**
     * 单日活跃用户
     * @param date
     * @return
     */
    public static  String getDailyDauKey(String date){
        return PREFIX_UAV +SPLIT + date;
    }

    /**
     * 区间活跃用户
     * @param startDate
     * @param endDate
     * @return
     */
    public static String getRangeDauKey(String startDate,String endDate){
        return PREFIX_UAV + SPLIT + startDate + SPLIT + endDate;
    }

    /**
     * 帖子分数
     * @return
     */
    public static String getPostScoreKey(){
        return PREFIX_POST + SPLIT + "score";
    }

}
