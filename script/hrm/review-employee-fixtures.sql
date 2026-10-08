-- Isolated synthetic verification only. Never apply to a business database.
-- QA login hashes are copied from the existing synthetic collection reader; no credentials are embedded.
INSERT INTO system_dept(id,name,parent_id,sort,status,tenant_id) VALUES(984100011,'复核合成部门 A',0,984,0,1),(984100022,'复核合成部门 B',0,985,0,1) ON DUPLICATE KEY UPDATE name=VALUES(name),status=0,tenant_id=1,deleted=0;
INSERT INTO system_role(id,name,code,sort,data_scope,data_scope_dept_ids,status,type,tenant_id)
VALUES(984000001,'合成核算发起人','reviewvrmaker',984,1,'[]',0,2,1),(984000002,'合成 HR 复核人','reviewvrhr',984,1,'[]',0,2,1),(984000003,'合成财务复核人','reviewvrfinance',984,1,'[]',0,2,1),(984000004,'合成复核只读人员','reviewvrreader',984,1,'[]',0,2,1),(984000005,'仅复核发起权限','reviewvrsubmitonly',984,1,'[]',0,2,1),(984000006,'仅冻结权限','reviewvrfreezeonly',984,1,'[]',0,2,1),(984000007,'非指定 HR 复核人','reviewvrwronghr',984,1,'[]',0,2,1),(984000008,'合成本人范围复核人','reviewvrself',984,5,'[]',0,2,1),(984000009,'合成异部门复核人','reviewvrdept',984,2,'[984100022]',0,2,1),(984000010,'合成缺资格查询人员','reviewvrnoelig',984,1,'[]',0,2,1),(984000011,'合成管理撤销人员','reviewvrmanager',984,1,'[]',0,2,1),(984000012,'合成停用财务人员','reviewvrdisabled',984,1,'[]',0,2,1)
ON DUPLICATE KEY UPDATE name=VALUES(name),data_scope=VALUES(data_scope),data_scope_dept_ids=VALUES(data_scope_dept_ids),status=0,tenant_id=1,deleted=0;
INSERT INTO system_users(id,username,password,nickname,dept_id,post_ids,status,tenant_id)
SELECT r.id,r.code,u.password,r.name,IF(r.id=984000009,984100022,984100011),'[]',IF(r.id=984000012,1,0),1 FROM system_role r CROSS JOIN system_users u WHERE r.id BETWEEN 984000001 AND 984000012 AND u.username='payrollvrreader' AND u.tenant_id=1 AND u.deleted=0
ON DUPLICATE KEY UPDATE password=VALUES(password),nickname=VALUES(nickname),dept_id=VALUES(dept_id),status=VALUES(status),deleted=0;
INSERT INTO system_user_role(user_id,role_id,tenant_id) SELECT id,id,1 FROM system_role WHERE id BETWEEN 984000001 AND 984000012 AND NOT EXISTS(SELECT 1 FROM system_user_role ur WHERE ur.user_id=system_role.id AND ur.role_id=system_role.id AND ur.tenant_id=1 AND ur.deleted=0);
INSERT INTO system_role_menu(role_id,menu_id,tenant_id)
SELECT r.id,m.id,1 FROM system_role r CROSS JOIN system_menu m WHERE r.id BETWEEN 984000001 AND 984000012 AND m.deleted=0 AND (
 (r.id NOT IN (984000005,984000006) AND m.permission IN ('hrm:employee:query','hrm:payroll:trial:query','hrm:payroll:calculation:query')) OR
 (r.id NOT IN (984000005,984000006,984000010) AND m.permission='hrm:payroll:eligibility:query') OR
 (r.id=984000001 AND m.permission IN ('hrm:payroll:trial:maintain','hrm:payroll:trial:execute','hrm:payroll:trial:review-submit')) OR
 (r.id=984000005 AND m.permission='hrm:payroll:trial:review-submit') OR
 (r.id IN (984000002,984000007,984000008,984000009,984000010) AND m.permission='hrm:payroll:trial:hr-review') OR
 (r.id IN (984000003,984000012) AND m.permission IN ('hrm:payroll:trial:finance-review','hrm:payroll:trial:freeze','hrm:payroll:trial:unfreeze')) OR
 (r.id=984000006 AND m.permission='hrm:payroll:trial:freeze') OR
 (r.id=984000011 AND m.permission='hrm:payroll:trial:admin-cancel') OR
 (r.id IN (984000001,984000002,984000003,984000007,984000008) AND m.permission IN ('bpm:task:query','bpm:task:update','bpm:process-instance:create','bpm:process-instance:cancel')) OR
 (r.id NOT IN (984000005,984000006) AND (m.path='/hrm' OR m.component='hrm/payroll/trial/index')))
AND NOT EXISTS(SELECT 1 FROM system_role_menu rm WHERE rm.role_id=r.id AND rm.menu_id=m.id AND rm.tenant_id=1 AND rm.deleted=0);
INSERT INTO hrm_employee(id,name,job_number,dept_id,user_id,status,tenant_id) VALUES(984100101,'复核合成人员 A','REVIEW-QA-001',984100011,984000008,20,1),(984100102,'复核合成人员 B','REVIEW-QA-002',984100022,984000020,30,1)
ON DUPLICATE KEY UPDATE name=VALUES(name),dept_id=VALUES(dept_id),user_id=VALUES(user_id),status=VALUES(status),deleted=0;
