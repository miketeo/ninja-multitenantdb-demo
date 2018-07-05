
CREATE TABLE TenantInfo (
  tenant_id VARCHAR(120) PRIMARY KEY,  -- in lowercase
  db_host VARCHAR(40) NOT NULL,
  db_name VARCHAR(40) NOT NULL,
  db_user VARCHAR(40) NOT NULL,
  db_password VARCHAR(40) NOT NULL
);

INSERT INTO TenantInfo(tenant_id,db_host,db_name,db_user,db_password) VALUES ('ninja-tenant1','localhost','tenant1','tenantuser1','tenantpw1');
INSERT INTO TenantInfo(tenant_id,db_host,db_name,db_user,db_password) VALUES ('ninja-tenant2','localhost','tenant2','tenantuser2','tenantpw2');
