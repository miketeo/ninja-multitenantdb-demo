package models;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name="Account")
public class Account {

	@Id
	@Column(name="id")
	public String id;

	@Column(name="create_time")
	public Date createTime;

	@Column(name="last_login_time")
	public Date lastLoginTime;
}
