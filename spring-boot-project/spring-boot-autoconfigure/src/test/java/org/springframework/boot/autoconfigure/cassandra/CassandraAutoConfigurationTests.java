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

import com.datastax.oss.driver.api.core.config.DefaultDriverOption;
import com.datastax.oss.driver.api.core.config.DriverConfigLoader;
import com.datastax.oss.driver.api.core.config.DriverExecutionProfile;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigParseOptions;
import com.typesafe.config.impl.Parseable;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link CassandraAutoConfiguration}
 *
 * @author Eddú Meléndez
 * @author Stephane Nicoll
 */
class CassandraAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(CassandraAutoConfiguration.class));

	@Test
	void driverConfigLoaderWithDefaultConfiguration() {
		this.contextRunner.run((context) -> {
			assertThat(context).hasSingleBean(DriverConfigLoader.class);
			assertThat(context.getBean(DriverConfigLoader.class).getInitialConfig().getDefaultProfile()
					.isDefined(DefaultDriverOption.SESSION_NAME)).isFalse();
		});
	}

	@Test
	void driverConfigLoaderWithCustomSessionName() {
		this.contextRunner.withPropertyValues("spring.data.cassandra.session-name=testcluster").run((context) -> {
			assertThat(context).hasSingleBean(DriverConfigLoader.class);
			assertThat(context.getBean(DriverConfigLoader.class).getInitialConfig().getDefaultProfile()
					.getString(DefaultDriverOption.SESSION_NAME)).isEqualTo("testcluster");
		});
	}

	@Test
	void driverConfigLoaderWithCustomSessionNameAndCustomizer() {
		this.contextRunner.withUserConfiguration(SimpleDriverConfigLoaderBuilderCustomizerConfig.class)
				.withPropertyValues("spring.data.cassandra.session-name=testcluster").run((context) -> {
					assertThat(context).hasSingleBean(DriverConfigLoader.class);
					assertThat(context.getBean(DriverConfigLoader.class).getInitialConfig().getDefaultProfile()
							.getString(DefaultDriverOption.SESSION_NAME)).isEqualTo("overridden-name");
				});
	}

	@Test
	void driverConfigLoaderApplyConsistentDefaults() {
		this.contextRunner.run((context) -> {
			Config defaultConfig = defaultConfig();
			DriverExecutionProfile config = context.getBean(DriverConfigLoader.class).getInitialConfig()
					.getDefaultProfile();
			// TODO
		});
	}

	@Test
	void driverConfigLoaderCustomizePoolOptions() {
		this.contextRunner.withPropertyValues("spring.data.cassandra.pool.idle-timeout=42",
				"spring.data.cassandra.pool.heartbeat-interval=62", "spring.data.cassandra.pool.max-queue-size=72")
				.run((context) -> {
					DriverExecutionProfile config = context.getBean(DriverConfigLoader.class).getInitialConfig()
							.getDefaultProfile();
					assertThat(config.getInt(DefaultDriverOption.HEARTBEAT_TIMEOUT)).isEqualTo(42);
					assertThat(config.getInt(DefaultDriverOption.HEARTBEAT_INTERVAL)).isEqualTo(62);
					assertThat(config.getInt(DefaultDriverOption.REQUEST_THROTTLER_MAX_QUEUE_SIZE)).isEqualTo(72);
				});
	}

	private static Config defaultConfig() {
		return Parseable.newResources("reference.conf", ConfigParseOptions.defaults()).parse().toConfig();
	}

	@Configuration(proxyBeanMethods = false)
	static class SimpleDriverConfigLoaderBuilderCustomizerConfig {

		@Bean
		DriverConfigLoaderBuilderCustomizer customizer() {
			return (builder) -> builder.withString(DefaultDriverOption.SESSION_NAME, "overridden-name");
		}

	}

}
