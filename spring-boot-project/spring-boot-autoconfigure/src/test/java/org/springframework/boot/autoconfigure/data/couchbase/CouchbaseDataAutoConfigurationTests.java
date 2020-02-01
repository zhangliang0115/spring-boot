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

package org.springframework.boot.autoconfigure.data.couchbase;

import java.util.Collections;
import java.util.Set;

import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.couchbase.CouchbaseAutoConfiguration;
import org.springframework.boot.autoconfigure.couchbase.CouchbaseProperties;
import org.springframework.boot.autoconfigure.couchbase.CouchbaseTestConfigurer;
import org.springframework.boot.autoconfigure.data.couchbase.city.City;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.couchbase.config.AbstractCouchbaseDataConfiguration;
import org.springframework.data.couchbase.config.BeanNames;
import org.springframework.data.couchbase.config.CouchbaseConfigurer;
import org.springframework.data.couchbase.core.CouchbaseTemplate;
import org.springframework.data.couchbase.core.convert.CouchbaseCustomConversions;
import org.springframework.data.couchbase.core.convert.DefaultCouchbaseTypeMapper;
import org.springframework.data.couchbase.core.mapping.CouchbaseMappingContext;
import org.springframework.data.couchbase.core.mapping.event.ValidatingCouchbaseEventListener;
import org.springframework.data.couchbase.core.query.Consistency;
import org.springframework.data.couchbase.repository.support.IndexManager;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link CouchbaseDataAutoConfiguration}.
 *
 * @author Stephane Nicoll
 */
class CouchbaseDataAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(ValidationAutoConfiguration.class,
					CouchbaseAutoConfiguration.class, CouchbaseDataAutoConfiguration.class));

	@Test
	void disabledIfCouchbaseIsNotConfigured() {
		this.contextRunner.run((context) -> assertThat(context).doesNotHaveBean(IndexManager.class));
	}

	@Test
	void customConfiguration() {
		this.contextRunner.withUserConfiguration(CustomCouchbaseConfiguration.class).run((context) -> {
			CouchbaseTemplate couchbaseTemplate = context.getBean(CouchbaseTemplate.class);
			assertThat(couchbaseTemplate.getDefaultConsistency()).isEqualTo(Consistency.STRONGLY_CONSISTENT);
		});
	}

	@Test
	void validatorIsPresent() {
		this.contextRunner.withUserConfiguration(CouchbaseTestConfigurer.class)
				.run((context) -> assertThat(context).hasSingleBean(ValidatingCouchbaseEventListener.class));
	}

	@Test
	void autoIndexIsDisabledByDefault() {
		this.contextRunner.withUserConfiguration(CouchbaseTestConfigurer.class).run((context) -> {
			IndexManager indexManager = context.getBean(IndexManager.class);
			assertThat(indexManager.isIgnoreViews()).isTrue();
			assertThat(indexManager.isIgnoreN1qlPrimary()).isTrue();
			assertThat(indexManager.isIgnoreN1qlSecondary()).isTrue();
		});
	}

	@Test
	void enableAutoIndex() {
		this.contextRunner.withUserConfiguration(CouchbaseTestConfigurer.class)
				.withPropertyValues("spring.data.couchbase.auto-index=true").run((context) -> {
					IndexManager indexManager = context.getBean(IndexManager.class);
					assertThat(indexManager.isIgnoreViews()).isFalse();
					assertThat(indexManager.isIgnoreN1qlPrimary()).isFalse();
					assertThat(indexManager.isIgnoreN1qlSecondary()).isFalse();
				});
	}

	@Test
	void changeConsistency() {
		this.contextRunner.withUserConfiguration(CouchbaseTestConfigurer.class)
				.withPropertyValues("spring.data.couchbase.consistency=eventually-consistent").run((context) -> {
					SpringBootCouchbaseDataConfiguration configuration = context
							.getBean(SpringBootCouchbaseDataConfiguration.class);
					assertThat(configuration.getDefaultConsistency()).isEqualTo(Consistency.EVENTUALLY_CONSISTENT);
				});
	}

	@Test
	@SuppressWarnings("unchecked")
	void entityScanShouldSetInitialEntitySet() {
		this.contextRunner.withUserConfiguration(EntityScanConfig.class).run((context) -> {
			CouchbaseMappingContext mappingContext = context.getBean(CouchbaseMappingContext.class);
			Set<Class<?>> initialEntitySet = (Set<Class<?>>) ReflectionTestUtils.getField(mappingContext,
					"initialEntitySet");
			assertThat(initialEntitySet).containsOnly(City.class);
		});
	}

	@Test
	void typeKeyDefault() {
		this.contextRunner.withUserConfiguration(CouchbaseTestConfigurer.class)
				.run((context) -> assertThat(context.getBean(AbstractCouchbaseDataConfiguration.class).typeKey())
						.isEqualTo(DefaultCouchbaseTypeMapper.DEFAULT_TYPE_KEY));
	}

	@Test
	void typeKeyCanBeCustomized() {
		this.contextRunner.withUserConfiguration(CouchbaseTestConfigurer.class)
				.withPropertyValues("spring.data.couchbase.type-key=_custom")
				.run((context) -> assertThat(context.getBean(AbstractCouchbaseDataConfiguration.class).typeKey())
						.isEqualTo("_custom"));
	}

	@Test
	void customConversions() {
		this.contextRunner.withUserConfiguration(CustomConversionsConfig.class).run((context) -> {
			CouchbaseTemplate template = context.getBean(CouchbaseTemplate.class);
			assertThat(
					template.getConverter().getConversionService().canConvert(CouchbaseProperties.class, Boolean.class))
							.isTrue();
		});
	}

	@Configuration
	static class CustomCouchbaseConfiguration extends AbstractCouchbaseDataConfiguration {

		@Override
		protected CouchbaseConfigurer couchbaseConfigurer() {
			return new CouchbaseTestConfigurer();
		}

		@Override
		protected Consistency getDefaultConsistency() {
			return Consistency.STRONGLY_CONSISTENT;
		}

	}

	@Configuration(proxyBeanMethods = false)
	@Import(CouchbaseTestConfigurer.class)
	static class CustomConversionsConfig {

		@Bean(BeanNames.COUCHBASE_CUSTOM_CONVERSIONS)
		CouchbaseCustomConversions myCustomConversions() {
			return new CouchbaseCustomConversions(Collections.singletonList(new MyConverter()));
		}

	}

	@Configuration(proxyBeanMethods = false)
	@EntityScan("org.springframework.boot.autoconfigure.data.couchbase.city")
	@Import(CustomCouchbaseConfiguration.class)
	static class EntityScanConfig {

	}

	static class MyConverter implements Converter<CouchbaseProperties, Boolean> {

		@Override
		public Boolean convert(CouchbaseProperties value) {
			return true;
		}

	}

}
