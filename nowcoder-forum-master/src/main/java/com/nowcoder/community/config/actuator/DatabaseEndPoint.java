package com.nowcoder.community.config.actuator;

import com.nowcoder.community.util.CommonUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * 项目监控:自定义监控端点
 * @author Alex
 * @version 1.0
 * @company xxxx
 * @copyright (c)  xxxInc. All rights reserved.
 * @date 2022/3/3 16:29
 */
@Component
@Endpoint(id = "database")
@Slf4j
@SuppressWarnings("all")
public class DatabaseEndPoint {
    @Autowired
    private DataSource dataSource;

    @ReadOperation
    public String checkConnection(){
        try (Connection connection = dataSource.getConnection();){
            return CommonUtil.getJsonString(0,"获取连接成功！");
        } catch (SQLException e) {
            log.error("获取连接失败:"+e.getMessage());
            return CommonUtil.getJsonString(1,"获取连接失败");
        }
    }

    
}
