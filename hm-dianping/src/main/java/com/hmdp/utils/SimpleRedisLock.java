package com.hmdp.utils;

import cn.hutool.core.lang.UUID;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

public class SimpleRedisLock implements ILock{

    private final StringRedisTemplate stringRedisTemplate;
    private final String name;

    public SimpleRedisLock(String name, StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.name = name;
    }

    private static final String KEY_PREFIX = "lock:";
    private static final String ID_PREFIX = UUID.randomUUID().toString(true) + "-";
    private static final DefaultRedisScript<Long> UNLOCK_SCRIPT;
    // 初始化时读取lua脚本
    static {
        UNLOCK_SCRIPT = new DefaultRedisScript<>();
        UNLOCK_SCRIPT.setLocation(new ClassPathResource("unlock.lua"));
        UNLOCK_SCRIPT.setResultType(Long.class);
    }
    @Override
    public boolean tryLock(long timeoutSec) {
        // 获取线程标识(UUID拼接线程id)
        String threadId = ID_PREFIX + Thread.currentThread().getId();
        // 获取锁
        Boolean success = stringRedisTemplate.opsForValue().setIfAbsent(KEY_PREFIX + name, threadId, timeoutSec, TimeUnit.SECONDS);
        return Boolean.TRUE.equals(success); // 避免自动拆箱null导致空指针
    }

    @Override
    public void unlock() {
        // 调用lua脚本（判断和删除分布式锁会在脚本中执行，满足原子性）
        stringRedisTemplate.execute(
                UNLOCK_SCRIPT, // lua脚本
                Collections.singletonList(KEY_PREFIX + name), // keys
                ID_PREFIX + Thread.currentThread().getId()); // argv
    }
//    @Override
//    public void unlock() {
//        // 获取线程表示
//        String threadId = ID_PREFIX + Thread.currentThread().getId();
//        // 判断标识是否一致
//        String id = stringRedisTemplate.opsForValue().get(KEY_PREFIX + name);
//        // 释放锁
//        if(threadId.equals(id)) {
//            stringRedisTemplate.delete(KEY_PREFIX + name);
//        }
//    }
}
