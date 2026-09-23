local count = redis.call('INCR', KEYS[1])
if count == 1 then
  redis.call('EXPIRE', KEYS[1], ARGV[2])
end
local capacity = tonumber(ARGV[1])
local remaining = math.max(0, capacity - count)
if count <= capacity then
  return {1, remaining}
end
return {0, 0}
