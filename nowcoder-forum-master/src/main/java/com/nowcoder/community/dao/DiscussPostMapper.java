package com.nowcoder.community.dao;

import com.nowcoder.community.entity.DiscussPost;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 用户帖子实体类
 * @author Alex
 * @version 1.0
 * @date 2022/1/30 17:31
 */
@Mapper
public interface DiscussPostMapper {

    /**
     * 分页查询用户发表的帖子
     * @param userId 用户ID
     * @param offset 帖子分页起始条数
     * @param limit  每页显示的帖子条数
     * @param orderMode 是否是显示热门帖子
     * @return
     */
    List<DiscussPost> selectDiscussPostsByPage(int userId, int offset, int limit,int orderMode);

    /**
     * 查询用户发表的帖子总数
     * @param userId 用户ID
     * @return
     */
    int selectDiscussPostCount(@Param("userId") int userId);

    /**
     * 插入帖子
     * @param discussPost
     * @return
     */
    int insertDiscussPost(DiscussPost discussPost);

    /**
     * 根据id查询帖子
     * @param id
     * @return
     */
    DiscussPost selDiscussPostById(int id);

    /**
     * 根据id更新帖子评论数量
     * @param id
     * @param commentCount
     * @return
     */
    int updateCommentCount(int id,int commentCount);

    /**
     * 查询所有帖子
     * @return
     */
    List<DiscussPost> selAllDiscussPost();

    /**
     * 根据id修改帖子类型
     * @param id
     * @param type
     * @return
     */
    int updateType(int id,int type);

    /**
     * 根据id修改帖子状态
     * @param id
     * @param status
     * @return
     */
    int updateStatus(int id,int status);

    /**
     * 修改帖子分数
     * @param id
     * @param score
     * @return
     */
    int updateScore(int id,double score);
}
