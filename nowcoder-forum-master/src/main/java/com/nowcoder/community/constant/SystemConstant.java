package com.nowcoder.community.constant;

/**
 * 系统接口
 *      封装系统常量信息
 * @author Alex
 * @version 1.0
 * @date 2022/2/19 15:53
 */
public interface SystemConstant {
    /**
     * 系统用户ID
     */
    int SYSTEM_USER_ID=1;

    /**
     * 系统权限:普通用户
     */
    String AUTHORITY_USER="user";

    /**
     * 系统权限：管理员
     */
    String AUTHORITY_ADMIN="admin";

    /**
     * 系统权限：版主
     */
    String AUTHORITY_MODERATOR="moderator";

    /**
     * 上传毫秒数
     */
    long UPLOAD_MILLISECONDS = 30000;

    /**
     * 上传次数
     */
    int UPLOAD_TIMES = 3;

    /**
     * 请求中的用户凭证字符串
     */
    String USER_CREDENTIAL_TICKET = "ticket";


}
