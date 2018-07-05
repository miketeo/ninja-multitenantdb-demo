
CREATE TABLE Task
(
  id INT AUTO_INCREMENT PRIMARY KEY,
  account_id VARCHAR(100) NOT NULL,
  description TEXT,
  has_completed BIT NOT NULL,
  create_time DATETIME NOT NULL,
  completion_time DATETIME
);

CREATE INDEX Task_accountid_idx ON Task(account_id);
CREATE INDEX Task_hascompleted_idx ON Task(has_completed);

ALTER TABLE Task ADD CONSTRAINT Task_account_fk 
  FOREIGN KEY (account_id) 
  REFERENCES Account(id)
  ON DELETE CASCADE
  ON UPDATE CASCADE;
