-- Avatar storage migration: switch im_user.avatar_url from pre-signed URL to object key.
-- The backend now signs object keys into accessible URLs on read,
-- so only plain object keys should be persisted.
--
-- Converts legacy rows like:
--   https://<bucket>.s3.<region>.<s3-host>/chat/images/xxx.jpg?X-Amz-...
-- into:
--   chat/images/xxx.jpg
--
-- Run once (fill in your own public base URL first):
--   mysql -u <user> -p liteim < db-migrate-avatar-key.sql

SET @oss_public_base = 'https://<bucket>.s3.<region>.<s3-host>/';

UPDATE im_user
SET avatar_url = REPLACE(SUBSTRING_INDEX(avatar_url, '?', 1),
                         @oss_public_base, '')
WHERE avatar_url LIKE 'http%';

-- Verify: no row should start with http afterwards
-- SELECT id, avatar_url FROM im_user;
