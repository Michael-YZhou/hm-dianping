-- 1. 参数列表
-- 1.1 优惠券ID
local voucherId = ARGV[1]

-- 1.2 用户ID
local userId = ARGV[2]


-- 2. 数据key
-- 2.1 库存key
local stockKey = 'seckill:stock:' .. voucherId

-- 2.2 订单key（用于记录已下单用户）
local orderKey = 'seckill:order:' .. voucherId


-- 3. 脚本业务逻辑

-- 3.1 判断库存是否充足
-- redis.call('get', stockKey) 获取库存
-- tonumber() 将字符串转为数字
if (tonumber(redis.call('get', stockKey)) <= 0) then
    -- 库存不足，返回1
    return 1
end


-- 3.2 判断用户是否已经下单
-- SISMEMBER 判断 userId 是否在 set 中
if (redis.call('sismember', orderKey, userId) == 1) then
    -- 用户已经下单，返回2
    return 2
end


-- 3.3 扣减库存
-- 库存减1
redis.call('incrby', stockKey, -1)


-- 3.4 记录用户下单
-- 将 userId 加入 set 集合
redis.call('sadd', orderKey, userId)


-- 3.5 下单成功
return 0
