package com.lxc.framework.listener;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.stream.ObjectRecord;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.stream.StreamListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class MyListener implements StreamListener<String,ObjectRecord<String,String>> {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Override
    public void onMessage(ObjectRecord<String,String> record) {
        try {
            String value = record.getValue();
            log.info("stream name :{}, body:{}",record,value);
            if (StrUtil.isBlank(value)) {
                return;
            }
            // todo 业务逻辑
            // 手动确认消息 如果不ack 消息就会进入到pending队列中 这个队列都是维护消费者的未确认的消息
            redisTemplate.opsForStream().acknowledge("mystream","group1",record.getId().getValue());
        } catch (Exception e) {
            log.error("error message:{}",e.getMessage());
        }
    }

}
