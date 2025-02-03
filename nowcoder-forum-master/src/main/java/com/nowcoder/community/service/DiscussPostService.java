package com.nowcoder.community.service;

import com.github.benmanes.caffeine.cache.CacheLoader;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.nowcoder.community.dao.DiscussPostMapper;
import com.nowcoder.community.entity.DiscussPost;
import com.nowcoder.community.filter.SensitiveWordFilter;
import com.nowcoder.community.util.CommonUtil;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 用户发表帖子service层
 * @author Alex
 * @version 1.0
 * @date 2022/2/1 16:00
 */
@Service
@Slf4j
public class DiscussPostService {


    @Autowired
    private DiscussPostMapper discussPostMapper;

    @Autowired
    private SensitiveWordFilter sensitiveWordFilter;

    @Value("${caffeine.posts.max-size}")
    private int maxSize;

    @Value("${caffeine.posts.expire-seconds}")
    private int expireSeconds;

    /**
     * 帖子列表缓存
     */
    private LoadingCache<String,List<DiscussPost>> postListCache;

    /**
     * 帖子总数缓存
     */
    private LoadingCache<Integer,Integer> postRowsCache;

    public static final int SPLIT_SIZE = 2;

    @PostConstruct
    public void init(){
        // 本地缓存(一级缓存)

        // 初始化帖子列表缓存
        postListCache = Caffeine.newBuilder()
                .maximumSize(maxSize)
                .expireAfterWrite(expireSeconds, TimeUnit.SECONDS)
                .build(new CacheLoader<String, List<DiscussPost>>() {
                    @Nullable
                    @Override
                    public List<DiscussPost> load(@NonNull String key) throws Exception {
                        if (key==null||key.length()==0){
                            throw new IllegalArgumentException("参数错误!");
                        }

                        String[] params = key.split(":");

                        if (params==null||params.length!=SPLIT_SIZE){
                            throw new IllegalArgumentException("参数错误!");
                        }

                        int offset = Integer.valueOf(params[0]);
                        int limit = Integer.valueOf(params[1]);
                        // 二级缓存：redis -> mysql
                        log.debug("load post list from DB.===============================>>");
                        return discussPostMapper.selectDiscussPostsByPage(0,offset,limit,1);
                    }
                });

        // 初始化帖子列表缓存
        postRowsCache = Caffeine.newBuilder()
                .maximumSize(maxSize)
                .expireAfterWrite(expireSeconds, TimeUnit.SECONDS)
                .build(new CacheLoader<Integer, Integer>() {
                    @Nullable
                    @Override
                    public Integer load(@NonNull Integer key) throws Exception {
                        // 查询数据库得到数据
                        log.debug("load post list from DB.================================>>");
                        return discussPostMapper.selectDiscussPostCount(key);
                    }
                });
    }


    /**
     * 分页查询(用户)帖子列表
     *  使用caffeine缓存帖子列表
     * @param userId
     * @param offset
     * @param limit
     * @return List<DiscussPost>
     */
    public List<DiscussPost> findDiscussPostList(int userId,int offset,int limit,int orderMode) {
        if (userId==0&&orderMode==1){
            return postListCache.get(offset + ":" +limit);
        }
        log.debug("load post list from DB.================================>>");
        return discussPostMapper.selectDiscussPostsByPage(userId,offset,limit,orderMode);
    }

    /**
     * 根据用户id查询用户帖子总数
     *  使用caffeine缓存帖子列表
     * @param userId
     * @return
     */
    public int findDiscussPostCount(int userId){
        if (userId==0){
            return postRowsCache.get(userId);
        }

        log.debug("load post rows from db.=================================>>");
        return discussPostMapper.selectDiscussPostCount(userId);
    }

    /**
     * 添加帖子
     * @param post
     * @return int
     */
    public int addDiscussPost(DiscussPost post){
        if (CommonUtil.isEmtpy(post)){
            throw new IllegalArgumentException("参数不能为空");
        }

        // 转义html标签
        post.setTitle(HtmlUtils.htmlEscape(post.getTitle()));
        post.setContent(HtmlUtils.htmlEscape(post.getContent()));

        // 过滤敏感词
        post.setTitle(sensitiveWordFilter.filterSensitiveWords(post.getTitle()));
        post.setContent(sensitiveWordFilter.filterSensitiveWords(post.getContent()));

        return discussPostMapper.insertDiscussPost(post);
    }

    /**
     * 根据帖子id查询帖子信息
     * @param id
     * @return DiscussPost
     */
    public DiscussPost findDiscussPostById(int id){
        return discussPostMapper.selDiscussPostById(id);
    }

    /**
     * 根据id更新评论数量
     * @param id
     * @param commentCount
     * @return
     */
    public int updateCommentCount(int id,int commentCount){
        return discussPostMapper.updateCommentCount(id,commentCount);
    }

    /**
     * 根据id修改帖子类型
     * @param id
     * @param type
     * @return
     */
    public int updateType(int id,int type){
        return discussPostMapper.updateType(id,type);
    }

    /**
     * 根据id修改帖子类型
     * @param id
     * @param status
     * @return
     */
    public int updateStatus(int id,int status){
        return discussPostMapper.updateStatus(id,status);
    }

    public int updateScore(int id,double score){
        return discussPostMapper.updateScore(id,score);
    }
}
