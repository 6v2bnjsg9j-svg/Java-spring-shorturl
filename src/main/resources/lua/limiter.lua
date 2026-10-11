

local key=KEYS[1]
local now=tonumber(ARGV[1])
local window=tonumber(ARGV[2])
local limit=tonumber(ARGV[3])
local value=ARGV[4]


redis.call("ZREMRANGEBYRANK",key,0,now-window)

local sum=redis.call("ZCARD",key)

if sum<limit then
    redis.call("ZADD",key,now,value)
    redis.call("EXPIRE",key,window)
    return 1
end

return 0