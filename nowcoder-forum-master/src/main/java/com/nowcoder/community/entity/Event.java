package com.nowcoder.community.entity;

import java.util.HashMap;
import java.util.Map;

/**
 * 高层次设计 事件驱动编程
 * kafka事件：封装kafka事件触发的一系列相关信息
 * @author Alex
 * @version 1.0
 * @date 2022/2/15 12:21
 */
public class Event {
    /**
     * 事件主题
     */
    private String topic;
    /**
     * 用户id
     */
    private int userId;
    /**
     * 实体类型 点赞/回复/关注等操作涉及的实体类型
     */
    private int entityType;
    /**
     * 实体id
     */
    private int entityId;
    /**
     * 实体作者
     */
    private int entityUserId;

    /**
     * 其他业务数据字段，使用map存储，便于扩展
     */
    private Map<String,Object> data = new HashMap<>();

    public String getTopic() {
        return topic;
    }

    public Event setTopic(String topic) {
        this.topic = topic;
        return this;
    }

    public int getUserId() {
        return userId;
    }

    public Event setUserId(int userId) {
        this.userId = userId;
        return this;
    }

    public int getEntityType() {
        return entityType;
    }

    public Event setEntityType(int entityType) {
        this.entityType = entityType;
        return this;
    }

    public int getEntityId() {
        return entityId;
    }

    public Event setEntityId(int entityId) {
        this.entityId = entityId;
        return this;
    }

    public int getEntityUserId() {
        return entityUserId;
    }

    public Event setEntityUserId(int entityUserId) {
        this.entityUserId = entityUserId;
        return this;
    }

    public Map<String, Object> getData() {
        return data;
    }

    public Event setData(String key,Object value) {
        this.data.put(key,value);
        return this;
    }
}
