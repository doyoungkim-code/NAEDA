package com.ssafy.naeda.global.config;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {
    @Bean
    public RedisTemplate<String,String> redisTemplate(RedisConnectionFactory connectionFactory){
        RedisTemplate<String, String> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new StringRedisSerializer());
        return template;
    }
}



//Spring Boot가 자동으로 RedisConnectionFactory를 만들어줌 (application.yaml의 host, port 기반)
//하지만 기본 RedisTemplate은 키/값을 Java 직렬화로 저장해서 Redis CLI에서 읽을 수 없음
//StringRedisSerializer를 쓰면 사람이 읽을 수 있는 문자열로 저장됨
//Redis CLI에서 keys * 했을 때 refresh:hong123@ssafy.co.kr 이런 식으로 깔끔하게 보임