-- Performance tuning: add covering/sort-aligned indexes for hot queries.
-- Run once:
--   mysql -u root -p liteim < db-optimize-index.sql

-- 1. Offline unread message scan: WHERE to_user_id=? AND status=0 ORDER BY created_at ASC
--    Composite index serves filter + sort in a single backward/range scan, no filesort.
ALTER TABLE im_message
    ADD INDEX idx_to_status_created (to_user_id, status, created_at);

-- 2. Friend request inbox: WHERE to_user_id=? ORDER BY created_at DESC
ALTER TABLE im_friend_request
    ADD INDEX idx_to_created (to_user_id, created_at);

-- 3. Friend request outbox: WHERE from_user_id=? ORDER BY created_at DESC
ALTER TABLE im_friend_request
    ADD INDEX idx_from_created (from_user_id, created_at);
