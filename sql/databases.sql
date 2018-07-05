
CREATE DATABASE tenant_catalog;
CREATE DATABASE tenant1;
CREATE DATABASE tenant2;

GRANT all privileges ON tenant_catalog.* to cataloguser@localhost identified by 'catalogpw';
GRANT all privileges ON tenant1.* to tenantuser1@localhost identified by 'tenantpw1';
GRANT all privileges ON tenant2.* to tenantuser2@localhost identified by 'tenantpw2';

FLUSH PRIVILEGES;
