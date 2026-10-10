-- BPM application tables. Flowable ACT_* tables are created by its supported schema updater.
CREATE TABLE IF NOT EXISTS `bpm_user_group` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `name` varchar(63) NOT NULL,
    `description` varchar(255) NOT NULL,
    `status` tinyint NOT NULL,
    `user_ids` varchar(255) NOT NULL,
    `creator` varchar(64) DEFAULT '',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updater` varchar(64) DEFAULT '',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `deleted` bit NOT NULL DEFAULT FALSE,
    `tenant_id` bigint NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT '用户组';

CREATE TABLE IF NOT EXISTS `bpm_category` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `name` varchar(63) NOT NULL,
    `code` varchar(63) NOT NULL,
    `description` varchar(255) NOT NULL,
    `status` tinyint NOT NULL,
    `sort` int NOT NULL,
    `creator` varchar(64) DEFAULT '',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updater` varchar(64) DEFAULT '',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `deleted` bit NOT NULL DEFAULT FALSE,
    `tenant_id` bigint NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT '分类';

CREATE TABLE IF NOT EXISTS `bpm_form` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `name` varchar(63) NOT NULL,
    `status` tinyint NOT NULL,
    `fields` varchar(255) NOT NULL,
    `conf` varchar(255) NOT NULL,
    `remark` varchar(255),
    `creator` varchar(64) DEFAULT '',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updater` varchar(64) DEFAULT '',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `deleted` bit NOT NULL DEFAULT FALSE,
    `tenant_id` bigint NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT '动态表单';

CREATE TABLE IF NOT EXISTS `bpm_process_definition_info` (
 `id` bigint NOT NULL AUTO_INCREMENT,
 `process_definition_id` varchar(255),
 `model_id` varchar(255),
 `model_type` int,
 `category` varchar(255),
 `icon` varchar(255),
 `description` LONGTEXT,
 `form_type` int,
 `form_id` bigint,
 `form_conf` LONGTEXT,
 `form_fields` LONGTEXT,
 `form_custom_create_path` varchar(255),
 `form_custom_view_path` varchar(255),
 `simple_model` LONGTEXT,
 `visible` bit,
 `sort` bigint,
 `start_user_ids` LONGTEXT,
 `start_dept_ids` LONGTEXT,
 `manager_user_ids` LONGTEXT,
 `allow_cancel_running_process` bit,
 `allow_withdraw_task` bit,
 `process_id_rule` LONGTEXT,
 `auto_approval_type` int,
 `title_setting` LONGTEXT,
 `summary_setting` LONGTEXT,
 `process_before_trigger_setting` LONGTEXT,
 `process_after_trigger_setting` LONGTEXT,
 `task_before_trigger_setting` LONGTEXT,
 `task_after_trigger_setting` LONGTEXT,
 `print_template_setting` LONGTEXT,
 `creator` varchar(64) DEFAULT '',
 `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
 `updater` varchar(64) DEFAULT '',
 `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
 `deleted` bit NOT NULL DEFAULT FALSE,
 `tenant_id` bigint NOT NULL DEFAULT 0,
 PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `bpm_process_expression` (
 `id` bigint NOT NULL AUTO_INCREMENT,
 `name` varchar(255),
 `status` int,
 `expression` LONGTEXT,
 `creator` varchar(64) DEFAULT '',
 `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
 `updater` varchar(64) DEFAULT '',
 `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
 `deleted` bit NOT NULL DEFAULT FALSE,
 `tenant_id` bigint NOT NULL DEFAULT 0,
 PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `bpm_process_listener` (
 `id` bigint NOT NULL AUTO_INCREMENT,
 `name` varchar(255),
 `status` int,
 `type` varchar(255),
 `event` varchar(255),
 `value_type` varchar(255),
 `value` LONGTEXT,
 `creator` varchar(64) DEFAULT '',
 `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
 `updater` varchar(64) DEFAULT '',
 `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
 `deleted` bit NOT NULL DEFAULT FALSE,
 `tenant_id` bigint NOT NULL DEFAULT 0,
 PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `bpm_oa_leave` (
 `id` bigint NOT NULL AUTO_INCREMENT,
 `user_id` bigint,
 `type` int,
 `reason` LONGTEXT,
 `start_time` datetime,
 `end_time` datetime,
 `day` bigint,
 `status` int,
 `process_instance_id` varchar(255),
 `creator` varchar(64) DEFAULT '',
 `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
 `updater` varchar(64) DEFAULT '',
 `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
 `deleted` bit NOT NULL DEFAULT FALSE,
 `tenant_id` bigint NOT NULL DEFAULT 0,
 PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `bpm_process_instance_copy` (
 `id` bigint NOT NULL AUTO_INCREMENT,
 `start_user_id` bigint,
 `process_instance_name` varchar(255),
 `process_instance_id` varchar(255),
 `process_definition_id` varchar(255),
 `category` varchar(255),
 `activity_id` varchar(255),
 `activity_name` varchar(255),
 `task_id` varchar(255),
 `user_id` bigint,
 `reason` LONGTEXT,
 `creator` varchar(64) DEFAULT '',
 `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
 `updater` varchar(64) DEFAULT '',
 `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
 `deleted` bit NOT NULL DEFAULT FALSE,
 `tenant_id` bigint NOT NULL DEFAULT 0,
 PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
