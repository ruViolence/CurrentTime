-- CurrentTime SQLite Schema

CREATE TABLE IF NOT EXISTS `time`
(
    `id`         INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT UNIQUE,
    `creator_id` INTEGER NOT NULL,
    `time`       INTEGER NOT NULL,
    `code`       TEXT    NOT NULL
);
CREATE INDEX IF NOT EXISTS `time_creator_id` ON `time` (`creator_id`);
CREATE UNIQUE INDEX IF NOT EXISTS `time_code` ON `time` (`code`);