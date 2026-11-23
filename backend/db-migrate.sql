-- DB schema migration: align liteim tables with backend mappers (executed 2026-08-14)
ALTER TABLE im_user CHANGE COLUMN is_deleted deleted tinyint NOT NULL DEFAULT 0 COMMENT 'soft delete flag: 0=normal, 1=cancelled';
ALTER TABLE im_chat_session CHANGE COLUMN is_pinned pinned tinyint NOT NULL DEFAULT 0 COMMENT 'pin flag: 0=no, 1=yes';
ALTER TABLE im_friend CHANGE COLUMN is_deleted deleted tinyint NOT NULL DEFAULT 0 COMMENT 'soft delete flag: 0=normal, 1=deleted';
ALTER TABLE im_friend ADD COLUMN updated_at bigint NOT NULL DEFAULT 0 COMMENT 'update time, epoch millis' AFTER created_at;
ALTER TABLE im_friend_log CHANGE COLUMN source_request_id request_id bigint DEFAULT NULL COMMENT 'related friend request id, references im_friend_request.id';
ALTER TABLE im_friend_request CHANGE COLUMN is_deleted deleted tinyint NOT NULL DEFAULT 0 COMMENT 'soft delete flag: 0=normal, 1=deleted';