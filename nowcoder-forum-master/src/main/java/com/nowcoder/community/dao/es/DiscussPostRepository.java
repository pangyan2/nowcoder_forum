package com.nowcoder.community.dao.es;

import com.nowcoder.community.entity.DiscussPost;
import org.elasticsearch.search.SearchHit;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

/**
 * es数据访问层：实现帖子存储在es之上的CURD
 * @author Alex
 * @version 1.0
 * @date 2022/2/16 16:01
 */
@Repository
public interface DiscussPostRepository extends ElasticsearchRepository<DiscussPost,Integer> {


}
