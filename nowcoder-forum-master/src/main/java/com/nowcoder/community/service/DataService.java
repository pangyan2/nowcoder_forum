package com.nowcoder.community.service;

import com.nowcoder.community.util.RedisKeyUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisStringCommands;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * @author Alex
 * @version 1.0
 * @company xx
 * @copyright (c)  xxInc. All rights reserved.
 * @date 2022/2/21 11:40
 */
@Service
public class DataService {
    @Autowired
    private RedisTemplate redisTemplate;

    private SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");

    /**
     * 将指定的ip记录uv
     * @param ip
     */
    public void recordUv(String ip){
        String redisKey = RedisKeyUtil.getDailyUvKey(simpleDateFormat.format(new Date()));
        redisTemplate.opsForHyperLogLog().add(redisKey,ip);
    }

    /**
     * 统计指定日期范围内的uv
     * @param start
     * @param end
     * @return
     */
    public long calculateUv(Date start,Date end){
        if(start == null || end == null){
            throw new IllegalArgumentException("参数不能为空");
        }

        // 整理该日期范围内的key
        List<String> keyList = new ArrayList<>();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(start);
        while (!calendar.getTime().after(end)){
            String dailyUvKey = RedisKeyUtil.getDailyUvKey(simpleDateFormat.format(calendar.getTime()));
            keyList.add(dailyUvKey);
            calendar.add(Calendar.DATE,1);
        }

        // 合并这些天的uv数据
        String rangeUvKey = RedisKeyUtil.getRangeUv(simpleDateFormat.format(start), simpleDateFormat.format(end));
        redisTemplate.opsForHyperLogLog().union(rangeUvKey,keyList.toArray());

        // 返回统计结果
        return redisTemplate.opsForHyperLogLog().size(rangeUvKey);
    }

    /**
     * 将指定用户计入DAU
     * @param userId
     */
    public void recordDau(int userId){
        String dailyDauKey = RedisKeyUtil.getDailyDauKey(simpleDateFormat.format(new Date()));
        redisTemplate.opsForValue().setBit(dailyDauKey,userId,true);
    }

    /**
     * 统计指定日期范围的Dau
     * @param start
     * @param end
     * @return
     */
    public long calculateDau(Date start,Date end){
        if (start == null && end == null){
            throw new IllegalArgumentException("参数不能为空");
        }

        // 整理该日期范围内的key
        List<byte[]> keyList = new ArrayList<>();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(start);
        while (!calendar.getTime().after(end)){
            String dailyDauKey = RedisKeyUtil.getDailyDauKey(simpleDateFormat.format(calendar.getTime()));
            keyList.add(dailyDauKey.getBytes());
            calendar.add(Calendar.DATE,1);
        }

        // 进行Or运算
        return (long) redisTemplate.execute(new RedisCallback() {
            @Override
            public Object doInRedis(RedisConnection redisConnection) throws DataAccessException {
                String rangeDauKey = RedisKeyUtil.getRangeDauKey(simpleDateFormat.format(start), simpleDateFormat.format(end));
                redisConnection.bitOp(RedisStringCommands.BitOperation.OR,rangeDauKey.getBytes(),keyList.toArray(new byte[0][0]));
                return redisConnection.bitCount(rangeDauKey.getBytes());
            }
        });
    }
}
