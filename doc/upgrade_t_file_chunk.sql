-- t_file_chunk 表结构升级脚本
-- 在已有数据库上执行，添加分片上传所需的字段

ALTER TABLE t_file_chunk
    ADD COLUMN upload_id VARCHAR(255) COMMENT '分片上传ID' AFTER chunk_identifier,
    ADD COLUMN etag VARCHAR(255) COMMENT '分片ETag' AFTER upload_id;

ALTER TABLE t_file
    ADD COLUMN file_md5 VARCHAR(32) COMMENT '文件MD5值' AFTER file_extension;
