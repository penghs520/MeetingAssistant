# 数据库设计文档

## 概述

Meeting Assistant使用PostgreSQL作为主数据库，存储会议、转录、说话人等信息。

## ER图

```
┌─────────────────┐
│    meetings     │
├─────────────────┤
│ id (PK)         │
│ title           │
│ start_time      │
│ end_time        │
│ status          │
│ summary         │
│ audio_file_url  │
│ created_at      │
│ updated_at      │
└────────┬────────┘
         │
         │ 1:N
         │
    ┌────┴────────────────┐
    │                     │
┌───▼──────────┐  ┌───────▼──────────┐
│   speakers   │  │   transcripts    │
├──────────────┤  ├──────────────────┤
│ id (PK)      │  │ id (PK)          │
│ meeting_id   │  │ meeting_id (FK)  │
│ name         │  │ speaker_id (FK)  │◄──┐
│ color        │  │ content          │   │
│ avatar_url   │  │ timestamp        │   │
│ created_at   │  │ sequence_order   │   │
└──────┬───────┘  │ confidence       │   │
       │          │ created_at       │   │
       │          └──────────────────┘   │
       │                                 │
       └─────────────────────────────────┘
                    1:N
```

## 数据表详细设计

### meetings - 会议表

存储会议的基本信息和状态。

| 字段名 | 类型 | 约束 | 说明 |
|--------|------|------|------|
| id | BIGSERIAL | PRIMARY KEY | 会议ID |
| title | VARCHAR(255) | NOT NULL | 会议标题 |
| start_time | TIMESTAMP | NOT NULL | 开始时间 |
| end_time | TIMESTAMP | NULL | 结束时间 |
| status | VARCHAR(50) | NOT NULL | 会议状态: RECORDING, COMPLETED, FAILED |
| summary | TEXT | NULL | AI生成的会议总结 |
| audio_file_url | VARCHAR(500) | NULL | 音频文件存储URL |
| created_at | TIMESTAMP | NOT NULL | 创建时间 |
| updated_at | TIMESTAMP | NOT NULL | 更新时间 |

**索引**:
- `idx_meetings_start_time`: (start_time DESC) - 按时间倒序查询
- `idx_meetings_status`: (status) - 按状态筛选

**业务规则**:
- `start_time` 在创建时自动设置为当前时间
- `status` 默认为 RECORDING
- `summary` 在会议完成后由AI生成

### speakers - 说话人表

存储会议中的说话人信息。

| 字段名 | 类型 | 约束 | 说明 |
|--------|------|------|------|
| id | BIGSERIAL | PRIMARY KEY | 说话人ID |
| meeting_id | BIGINT | NOT NULL, FK | 所属会议ID |
| name | VARCHAR(100) | NOT NULL | 说话人姓名 |
| color | VARCHAR(20) | NULL | UI显示颜色（十六进制） |
| avatar_url | VARCHAR(500) | NULL | 头像URL |
| created_at | TIMESTAMP | NOT NULL | 创建时间 |

**索引**:
- `idx_speakers_meeting_id`: (meeting_id) - 快速查询某会议的所有说话人

**外键**:
- `meeting_id` REFERENCES meetings(id) ON DELETE CASCADE

**业务规则**:
- 一个会议可以有多个说话人
- 说话人由用户手动标记创建
- 删除会议时级联删除所有说话人

### transcripts - 转录表

存储会议的逐段转录文本。

| 字段名 | 类型 | 约束 | 说明 |
|--------|------|------|------|
| id | BIGSERIAL | PRIMARY KEY | 转录ID |
| meeting_id | BIGINT | NOT NULL, FK | 所属会议ID |
| speaker_id | BIGINT | NULL, FK | 说话人ID |
| content | TEXT | NOT NULL | 转录文本内容 |
| timestamp | TIMESTAMP | NOT NULL | 说话时间戳 |
| sequence_order | INT | NOT NULL | 序列号（保证顺序） |
| confidence | DECIMAL(5,2) | NULL | 转录置信度 0.00-1.00 |
| created_at | TIMESTAMP | NOT NULL | 创建时间 |

**索引**:
- `idx_transcripts_meeting_id`: (meeting_id) - 查询某会议的转录
- `idx_transcripts_timestamp`: (timestamp) - 按时间排序
- `idx_transcripts_sequence`: (meeting_id, sequence_order) - 保证顺序查询性能

**外键**:
- `meeting_id` REFERENCES meetings(id) ON DELETE CASCADE
- `speaker_id` REFERENCES speakers(id) ON DELETE SET NULL

**业务规则**:
- `sequence_order` 自动递增，保证转录顺序
- `speaker_id` 可为空（未标记说话人）
- 删除说话人时，转录的speaker_id设为NULL
- `confidence` 由AI模型返回，表示转录准确度

## SQL DDL

```sql
-- 创建数据库
CREATE DATABASE meeting_assistant
    WITH
    ENCODING = 'UTF8'
    LC_COLLATE = 'en_US.UTF-8'
    LC_CTYPE = 'en_US.UTF-8';

\c meeting_assistant;

-- 会议表
CREATE TABLE meetings (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP,
    status VARCHAR(50) NOT NULL DEFAULT 'RECORDING',
    summary TEXT,
    audio_file_url VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),

    CONSTRAINT chk_status CHECK (status IN ('RECORDING', 'COMPLETED', 'FAILED'))
);

CREATE INDEX idx_meetings_start_time ON meetings(start_time DESC);
CREATE INDEX idx_meetings_status ON meetings(status);

-- 说话人表
CREATE TABLE speakers (
    id BIGSERIAL PRIMARY KEY,
    meeting_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    color VARCHAR(20),
    avatar_url VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_speakers_meeting
        FOREIGN KEY (meeting_id)
        REFERENCES meetings(id)
        ON DELETE CASCADE
);

CREATE INDEX idx_speakers_meeting_id ON speakers(meeting_id);

-- 转录表
CREATE TABLE transcripts (
    id BIGSERIAL PRIMARY KEY,
    meeting_id BIGINT NOT NULL,
    speaker_id BIGINT,
    content TEXT NOT NULL,
    timestamp TIMESTAMP NOT NULL,
    sequence_order INT NOT NULL,
    confidence DECIMAL(5,2),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_transcripts_meeting
        FOREIGN KEY (meeting_id)
        REFERENCES meetings(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_transcripts_speaker
        FOREIGN KEY (speaker_id)
        REFERENCES speakers(id)
        ON DELETE SET NULL,

    CONSTRAINT chk_confidence
        CHECK (confidence IS NULL OR (confidence >= 0.00 AND confidence <= 1.00))
);

CREATE INDEX idx_transcripts_meeting_id ON transcripts(meeting_id);
CREATE INDEX idx_transcripts_timestamp ON transcripts(timestamp);
CREATE INDEX idx_transcripts_sequence ON transcripts(meeting_id, sequence_order);

-- 触发器：自动更新 meetings.updated_at
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_meetings_updated_at
    BEFORE UPDATE ON meetings
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();
```

## 示例查询

### 获取最近的会议列表

```sql
SELECT
    id,
    title,
    start_time,
    end_time,
    status,
    CASE
        WHEN summary IS NOT NULL THEN SUBSTRING(summary, 1, 100) || '...'
        ELSE NULL
    END as summary_preview
FROM meetings
ORDER BY start_time DESC
LIMIT 20;
```

### 获取会议完整信息（含转录和说话人）

```sql
SELECT
    m.id as meeting_id,
    m.title,
    m.start_time,
    s.id as speaker_id,
    s.name as speaker_name,
    t.content,
    t.timestamp
FROM meetings m
LEFT JOIN transcripts t ON t.meeting_id = m.id
LEFT JOIN speakers s ON t.speaker_id = s.id
WHERE m.id = 1
ORDER BY t.sequence_order ASC;
```

### 统计会议说话人发言次数

```sql
SELECT
    s.name,
    COUNT(t.id) as transcript_count,
    SUM(LENGTH(t.content)) as total_words
FROM speakers s
LEFT JOIN transcripts t ON t.speaker_id = s.id
WHERE s.meeting_id = 1
GROUP BY s.id, s.name
ORDER BY transcript_count DESC;
```

### 查找包含关键词的转录

```sql
SELECT
    m.title,
    s.name,
    t.content,
    t.timestamp
FROM transcripts t
JOIN meetings m ON t.meeting_id = m.id
LEFT JOIN speakers s ON t.speaker_id = s.id
WHERE t.content ILIKE '%关键词%'
ORDER BY t.timestamp DESC;
```

## 数据迁移

### 初始化脚本

```bash
# 创建数据库和表
psql -U postgres -f schema.sql

# 创建测试数据
psql -U postgres -d meeting_assistant -f seed.sql
```

### 备份与恢复

```bash
# 备份
pg_dump -U postgres meeting_assistant > backup_$(date +%Y%m%d).sql

# 恢复
psql -U postgres -d meeting_assistant < backup_20250107.sql
```

## 性能优化建议

### 1. 分区表（大数据量场景）

当`transcripts`表数据量超过百万级，可按月份分区：

```sql
CREATE TABLE transcripts_2025_01 PARTITION OF transcripts
FOR VALUES FROM ('2025-01-01') TO ('2025-02-01');
```

### 2. 全文搜索索引

为转录内容添加全文搜索：

```sql
ALTER TABLE transcripts ADD COLUMN content_tsv tsvector;

CREATE INDEX idx_transcripts_fts ON transcripts USING GIN(content_tsv);

CREATE TRIGGER tsvector_update
BEFORE INSERT OR UPDATE ON transcripts
FOR EACH ROW EXECUTE FUNCTION
tsvector_update_trigger(content_tsv, 'pg_catalog.simple', content);
```

### 3. 物化视图（统计数据）

```sql
CREATE MATERIALIZED VIEW meeting_stats AS
SELECT
    m.id,
    m.title,
    COUNT(DISTINCT s.id) as speaker_count,
    COUNT(t.id) as transcript_count,
    m.start_time,
    m.end_time,
    EXTRACT(EPOCH FROM (m.end_time - m.start_time))/60 as duration_minutes
FROM meetings m
LEFT JOIN speakers s ON s.meeting_id = m.id
LEFT JOIN transcripts t ON t.meeting_id = m.id
GROUP BY m.id;

-- 刷新物化视图
REFRESH MATERIALIZED VIEW meeting_stats;
```

## 数据保留策略

### 自动归档旧会议

```sql
-- 归档90天前的会议
CREATE TABLE meetings_archive (LIKE meetings INCLUDING ALL);

INSERT INTO meetings_archive
SELECT * FROM meetings
WHERE start_time < NOW() - INTERVAL '90 days'
AND status = 'COMPLETED';

DELETE FROM meetings
WHERE start_time < NOW() - INTERVAL '90 days'
AND status = 'COMPLETED';
```

---

**文档版本**: v1.0
**最后更新**: 2025-10-07
