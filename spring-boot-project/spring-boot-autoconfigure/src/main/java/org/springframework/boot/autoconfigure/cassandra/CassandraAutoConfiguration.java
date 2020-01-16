/*
 * Copyright 2012-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.autoconfigure.cassandra;

import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.net.ssl.SSLContext;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.CqlSessionBuilder;
import com.datastax.oss.driver.api.core.config.DefaultDriverOption;
import com.datastax.oss.driver.api.core.config.DriverConfigLoader;
import com.datastax.oss.driver.api.core.config.DriverOption;
import com.datastax.oss.driver.api.core.config.ProgrammaticDriverConfigLoaderBuilder;
import com.datastax.oss.driver.internal.core.config.typesafe.DefaultDriverConfigLoader;
import com.datastax.oss.driver.internal.core.config.typesafe.DefaultProgrammaticDriverConfigLoaderBuilder;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Cassandra.
 *
 * @author Julien Dubois
 * @author Phillip Webb
 * @author Eddú Meléndez
 * @author Stephane Nicoll
 * @author Steffen F. Qvistgaard
 * @since 1.3.0
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass({ CqlSession.class })
@EnableConfigurationProperties(CassandraProperties.class)
public class CassandraAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	@Lazy
	public CqlSession cassandraSession(CqlSessionBuilder cqlSessionBuilder) {
		return cqlSessionBuilder.build();
	}

	@Bean
	@ConditionalOnMissingBean
	public CqlSessionBuilder cassandraSessionBuilder(CassandraProperties properties,
			DriverConfigLoader driverConfigLoader, ObjectProvider<CqlSessionBuilderCustomizer> builderCustomizers) {
		CqlSessionBuilder builder = CqlSession.builder().withConfigLoader(driverConfigLoader);
		configureSsl(properties, builder);
		builder.withKeyspace(properties.getKeyspaceName());
		builderCustomizers.orderedStream().forEach((customizer) -> customizer.customize(builder));
		return builder;
	}

	private void configureSsl(CassandraProperties properties, CqlSessionBuilder builder) {
		if (properties.isSsl()) {
			try {
				builder.withSslContext(SSLContext.getDefault());
			}
			catch (NoSuchAlgorithmException ex) {
				throw new IllegalStateException("Could not setup SSL default context for Cassandra", ex);
			}
		}
	}

	@Bean
	@ConditionalOnMissingBean
	public DriverConfigLoader cassandraDriverConfigLoader(CassandraProperties properties,
			ObjectProvider<DriverConfigLoaderBuilderCustomizer> builderCustomizers) {
		ProgrammaticDriverConfigLoaderBuilder builder = new DefaultProgrammaticDriverConfigLoaderBuilder(
				() -> cassandraConfiguration(properties), DefaultDriverConfigLoader.DEFAULT_ROOT_PATH);
		builderCustomizers.orderedStream().forEach((customizer) -> customizer.customize(builder));
		return builder.build();
	}

	private Config cassandraConfiguration(CassandraProperties properties) {
		CassandraDriverOptions options = new CassandraDriverOptions();
		PropertyMapper map = PropertyMapper.get();
		map.from(properties.getSessionName()).whenHasText()
				.to((sessionName) -> options.add(DefaultDriverOption.SESSION_NAME, sessionName));
		map.from(properties::getUsername).whenNonNull()
				.to((username) -> options.add(DefaultDriverOption.AUTH_PROVIDER_USER_NAME, username)
						.add(DefaultDriverOption.AUTH_PROVIDER_PASSWORD, properties.getPassword()));
		map.from(properties::getCompression).whenNonNull()
				.to((compression) -> options.add(DefaultDriverOption.PROTOCOL_COMPRESSION, compression));
		mapQueryOptions(properties, options);
		mapSocketOptions(properties, options);
		mapPoolingOptions(properties, options);
		map.from(properties::getContactPoints)
				.to((contactPoints) -> options.add(DefaultDriverOption.CONTACT_POINTS, contactPoints));
		ConfigFactory.invalidateCaches();
		return ConfigFactory.defaultOverrides().withFallback(options.build())
				.withFallback(ConfigFactory.defaultReference()).resolve();
	}

	private void mapQueryOptions(CassandraProperties properties, CassandraDriverOptions options) {
		PropertyMapper map = PropertyMapper.get();
		map.from(properties::getConsistencyLevel).whenNonNull()
				.to(((consistency) -> options.add(DefaultDriverOption.REQUEST_CONSISTENCY, consistency)));
		map.from(properties::getSerialConsistencyLevel).whenNonNull().to(
				(serialConsistency) -> options.add(DefaultDriverOption.REQUEST_SERIAL_CONSISTENCY, serialConsistency));
		map.from(properties::getPageSize)
				.to((pageSize) -> options.add(DefaultDriverOption.REQUEST_PAGE_SIZE, pageSize));
	}

	private void mapSocketOptions(CassandraProperties properties, CassandraDriverOptions options) {
		PropertyMapper map = PropertyMapper.get();
		map.from(properties::getConnectTimeout).whenNonNull().asInt(Duration::toMillis)
				.to((connectTimeout) -> options.add(DefaultDriverOption.CONNECTION_INIT_QUERY_TIMEOUT, connectTimeout));
		map.from(properties::getReadTimeout).whenNonNull().asInt(Duration::toMillis)
				.to((readTimeout) -> options.add(DefaultDriverOption.REQUEST_TIMEOUT, readTimeout));
	}

	private void mapPoolingOptions(CassandraProperties properties, CassandraDriverOptions options) {
		PropertyMapper map = PropertyMapper.get();
		CassandraProperties.Pool poolProperties = properties.getPool();
		map.from(poolProperties::getIdleTimeout).whenNonNull().asInt(Duration::getSeconds)
				.to((idleTimeout) -> options.add(DefaultDriverOption.HEARTBEAT_TIMEOUT, idleTimeout));
		map.from(poolProperties::getHeartbeatInterval).whenNonNull().asInt(Duration::getSeconds)
				.to((heartBeatInterval) -> options.add(DefaultDriverOption.HEARTBEAT_INTERVAL, heartBeatInterval));
		map.from(poolProperties::getMaxQueueSize)
				.to((maxQueueSize) -> options.add(DefaultDriverOption.REQUEST_THROTTLER_MAX_QUEUE_SIZE, maxQueueSize));
	}

	private static class CassandraDriverOptions {

		private final Map<String, String> options = new LinkedHashMap<>();

		private CassandraDriverOptions add(DriverOption option, String value) {
			String key = createKeyFor(option);
			this.options.put(key, value);
			return this;
		}

		private CassandraDriverOptions add(DriverOption option, int value) {
			return add(option, String.valueOf(value));
		}

		private CassandraDriverOptions add(DriverOption option, Enum<?> value) {
			return add(option, value.name());
		}

		private CassandraDriverOptions add(DriverOption option, List<String> values) {
			for (int i = 0; i < values.size(); i++) {
				this.options.put(String.format("%s.%s", createKeyFor(option), i), values.get(i));
			}
			return this;
		}

		private Config build() {
			return ConfigFactory.parseMap(this.options, "Environment");
		}

		private static String createKeyFor(DriverOption option) {
			return String.format("%s.%s", DefaultDriverConfigLoader.DEFAULT_ROOT_PATH, option.getPath());
		}

	}

}
