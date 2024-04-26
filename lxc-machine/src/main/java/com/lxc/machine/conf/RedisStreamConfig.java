package com.lxc.machine.conf;

import com.lxc.machine.listener.RedisStreamListener;
import java.time.Duration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.ObjectRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;
import org.springframework.data.redis.stream.Subscription;

@Slf4j
@Configuration
public class RedisStreamConfig {

    @Autowired
    private RedisStreamListener myListener;

    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 实际生产环境中  我们应该把消费者组等信息  写入配置环境中
     */
    //    @Autowired
    //    private StreamProperty streamProperty;

    /**
     * 收到消息后不自动确认，需要用户选择合适的时机确认 当某个消息被ACK，PEL列表就会减少 如果忘记确认（ACK），则PEL列表会不断增长占用内存 如果服务器发生意外，重启连接后将再次收到PEL中的消息ID列表
     */
    @Bean
    public Subscription subscription(RedisConnectionFactory factory) {
        initGroup("mystream","group1");
        // 创建Stream消息监听容器配置
        StreamMessageListenerContainer.StreamMessageListenerContainerOptions<String,ObjectRecord<String,Object>> options =
            StreamMessageListenerContainer
                .StreamMessageListenerContainerOptions
                .builder()
                // 读取超时时间
                .pollTimeout(Duration.ofSeconds(3))
                // 配置消息类型
                .targetType(Object.class)
                // 异常处理器
                .errorHandler(t -> log.info("redis listener error",t))
                .build();
        // 创建Stream消息监听容器
        StreamMessageListenerContainer<String,ObjectRecord<String,Object>> listenerContainer = StreamMessageListenerContainer.create(factory,options);
        // 设置消费手动提交配置
        Subscription subscription = listenerContainer.receive(
            // 设置消费者分组和名称
            Consumer.from("group1","consumer-1"),
            // 设置订阅Stream的key和获取偏移量，以及消费处理类
            StreamOffset.create("mystream",ReadOffset.lastConsumed()),
            myListener
        );
        // 监听容器启动
        listenerContainer.start();
        return subscription;
    }

    /**
     * 初始化分组
     */
    private void initGroup(String key,String group) {
        Boolean aBoolean = redisTemplate.hasKey(key);
        // 创建不存在的分组
        if (Boolean.FALSE.equals(aBoolean)) {
            redisTemplate.opsForStream().createGroup(key,group);
        }
    }

}
