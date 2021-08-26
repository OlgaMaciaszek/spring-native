/*
 * Copyright 2019-2021 the original author or authors.
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

package org.springframework.beans.factory.support;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.context.support.GenericApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link BeanDefinitionValueResolverAccessor}.
 *
 * @author Stephane Nicoll
 */
class BeanDefinitionValueResolverAccessorTests {

	@Test
	void resolveBeanReference() {
		GenericApplicationContext context = new GenericApplicationContext();
		context.registerBean("one", String.class, () -> "1");
		BeanDefinition beanDefinition = BeanDefinitionBuilder.rootBeanDefinition(TestComponent.class).getBeanDefinition();
		assertThat(BeanDefinitionValueResolverAccessor.get(context, "test", beanDefinition)
				.resolveValueIfNecessary("test", new RuntimeBeanReference("one"))).isEqualTo("1");
	}

	@SuppressWarnings("unused")
	private static class TestComponent {

		public TestComponent(String value) {
		}
	}

}
