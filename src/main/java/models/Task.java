package models;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name="Task")
public class Task {

	@Id
	@GeneratedValue
	@Column(name="id")
	public Integer id;

	@JsonIgnore
	@ManyToOne
	@JoinColumn(name="account_id")
	public Account account;

	@Column(name="description")
	public String description;

	@Column(name="has_completed")
	public boolean hasCompleted = false;

	@Column(name="create_time")
	public Date createTime;

	@Column(name="completion_time")
	public Date completionTime;
}
