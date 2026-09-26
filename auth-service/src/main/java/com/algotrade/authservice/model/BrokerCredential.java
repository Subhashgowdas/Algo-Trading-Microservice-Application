package com.algotrade.authservice.model;

import java.util.Map;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "broker_credentials",
uniqueConstraints = {
		@UniqueConstraint(name = "uq_broker_user", columnNames = {"fk_user_id", "broker_name"})
})

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BrokerCredential extends BaseEntity{

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "fk_user_id", nullable = false)
	private User user;

	@Column(name = "broker_name", nullable = false, length = 50)
	private String brokerName;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "broker_api_config", columnDefinition = "json", nullable = false)
	private Map<String, Object> brokerApiConfig;

}