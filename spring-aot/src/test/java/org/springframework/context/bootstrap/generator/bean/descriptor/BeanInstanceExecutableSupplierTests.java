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

package org.springframework.context.bootstrap.generator.bean.descriptor;

import java.lang.reflect.Executable;
import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.bootstrap.generator.sample.constructor.SampleBeanWithConstructors;
import org.springframework.context.bootstrap.generator.sample.factory.NumberHolder;
import org.springframework.context.bootstrap.generator.sample.factory.NumberHolderFactoryBean;
import org.springframework.context.bootstrap.generator.sample.factory.SampleFactory;
import org.springframework.core.ResolvableType;
import org.springframework.util.ReflectionUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link BeanInstanceExecutableSupplier}.
 *
 * @author Stephane Nicoll
 */
class BeanInstanceExecutableSupplierTests {

	@Test
	void detectBeanInstanceExecutableWithFactoryMethodName() {
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		beanFactory.registerSingleton("testBean", "test");
		BeanDefinition beanDefinition = BeanDefinitionBuilder.rootBeanDefinition(SampleFactory.class.getName())
				.setFactoryMethod("create").addConstructorArgReference("testBean").getBeanDefinition();
		Executable executable = detectBeanInstanceExecutable(beanFactory, beanDefinition);
		assertThat(executable).isNotNull().isEqualTo(ReflectionUtils.findMethod(SampleFactory.class, "create", String.class));
	}

	@Test
	void beanDefinitionWithFactoryMethodNameAndAssignableConstructorArg() {
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		beanFactory.registerSingleton("testNumber", 1L);
		beanFactory.registerSingleton("testBean", "test");
		BeanDefinition beanDefinition = BeanDefinitionBuilder.rootBeanDefinition(SampleFactory.class.getName())
				.setFactoryMethod("create").addConstructorArgReference("testNumber")
				.addConstructorArgReference("testBean").getBeanDefinition();
		Executable executable = detectBeanInstanceExecutable(beanFactory, beanDefinition);
		assertThat(executable).isNotNull().isEqualTo(ReflectionUtils.findMethod(SampleFactory.class, "create", Number.class, String.class));
	}

	@Test
	void beanDefinitionWithFactoryMethodNameAndMatchingMethodNamesThatShouldBeIgnored() {
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		BeanDefinition beanDefinition = BeanDefinitionBuilder.rootBeanDefinition(DummySampleFactory.class)
				.setFactoryMethod("of").addConstructorArgValue(42).getBeanDefinition();
		Executable executable = detectBeanInstanceExecutable(beanFactory, beanDefinition);
		assertThat(executable).isNotNull().isEqualTo(ReflectionUtils.findMethod(DummySampleFactory.class, "of", Integer.class));
	}

	@Test
	void beanDefinitionWithConstructorArgsForMultipleConstructors() throws Exception {
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		beanFactory.registerSingleton("testNumber", 1L);
		beanFactory.registerSingleton("testBean", "test");
		BeanDefinition beanDefinition = BeanDefinitionBuilder.rootBeanDefinition(SampleBeanWithConstructors.class.getName())
				.addConstructorArgReference("testNumber")
				.addConstructorArgReference("testBean").getBeanDefinition();
		Executable executable = detectBeanInstanceExecutable(beanFactory, beanDefinition);
		assertThat(executable).isNotNull().isEqualTo(SampleBeanWithConstructors.class.getDeclaredConstructor(Number.class, String.class));
	}

	@Test
	void genericBeanDefinitionWithConstructorArgsForMultipleConstructors() throws Exception {
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		beanFactory.registerSingleton("testNumber", 1L);
		beanFactory.registerSingleton("testBean", "test");
		BeanDefinition beanDefinition = BeanDefinitionBuilder.genericBeanDefinition(SampleBeanWithConstructors.class.getName())
				.addConstructorArgReference("testNumber")
				.addConstructorArgReference("testBean").getBeanDefinition();
		Executable executable = detectBeanInstanceExecutable(beanFactory, beanDefinition);
		assertThat(executable).isNotNull().isEqualTo(SampleBeanWithConstructors.class.getDeclaredConstructor(Number.class, String.class));
	}

	@Test
	void beanDefinitionWithMultiArgConstructorAndMatchingValue() throws NoSuchMethodException {
		BeanDefinition beanDefinition = BeanDefinitionBuilder.rootBeanDefinition(MultiConstructorSample.class)
				.addConstructorArgValue(42).getBeanDefinition();
		Executable executable = detectBeanInstanceExecutable(new DefaultListableBeanFactory(), beanDefinition);
		assertThat(executable).isNotNull().isEqualTo(MultiConstructorSample.class.getDeclaredConstructor(Integer.class));
	}

	@Test
	void beanDefinitionWithMultiArgConstructorAndMatchingArrayValue() throws NoSuchMethodException {
		BeanDefinition beanDefinition = BeanDefinitionBuilder.rootBeanDefinition(MultiConstructorArraySample.class)
				.addConstructorArgValue(42).getBeanDefinition();
		Executable executable = detectBeanInstanceExecutable(new DefaultListableBeanFactory(), beanDefinition);
		assertThat(executable).isNotNull().isEqualTo(MultiConstructorArraySample.class.getDeclaredConstructor(Integer[].class));
	}

	@Test
	void beanDefinitionWithMultiArgConstructorAndMatchingListValue() throws NoSuchMethodException {
		BeanDefinition beanDefinition = BeanDefinitionBuilder.rootBeanDefinition(MultiConstructorListSample.class)
				.addConstructorArgValue(42).getBeanDefinition();
		Executable executable = detectBeanInstanceExecutable(new DefaultListableBeanFactory(), beanDefinition);
		assertThat(executable).isNotNull().isEqualTo(MultiConstructorListSample.class.getDeclaredConstructor(List.class));
	}

	@Test
	void beanDefinitionWithMultiArgConstructorAndMatchingValueAsInnerBean() throws NoSuchMethodException {
		BeanDefinition beanDefinition = BeanDefinitionBuilder.rootBeanDefinition(MultiConstructorSample.class)
				.addConstructorArgValue(BeanDefinitionBuilder.rootBeanDefinition(Integer.class, "valueOf")
						.addConstructorArgValue("42").getBeanDefinition())
				.getBeanDefinition();
		Executable executable = detectBeanInstanceExecutable(new DefaultListableBeanFactory(), beanDefinition);
		assertThat(executable).isNotNull().isEqualTo(MultiConstructorSample.class.getDeclaredConstructor(Integer.class));
	}

	@Test
	void beanDefinitionWithMultiArgConstructorAndMatchingValueAsInnerBeanFactory() throws NoSuchMethodException {
		BeanDefinition beanDefinition = BeanDefinitionBuilder.rootBeanDefinition(MultiConstructorSample.class)
				.addConstructorArgValue(BeanDefinitionBuilder.rootBeanDefinition(IntegerFactoryBean.class).getBeanDefinition())
				.getBeanDefinition();
		Executable executable = detectBeanInstanceExecutable(new DefaultListableBeanFactory(), beanDefinition);
		assertThat(executable).isNotNull().isEqualTo(MultiConstructorSample.class.getDeclaredConstructor(Integer.class));
	}

	@Test
	void beanDefinitionWithMultiArgConstructorAndNonMatchingValue() {
		BeanDefinition beanDefinition = BeanDefinitionBuilder.rootBeanDefinition(MultiConstructorSample.class)
				.addConstructorArgValue(Locale.ENGLISH).getBeanDefinition();
		Executable executable = detectBeanInstanceExecutable(new DefaultListableBeanFactory(), beanDefinition);
		assertThat(executable).isNull();
	}

	@Test
	void beanDefinitionWithMultiArgConstructorAndNonMatchingValueAsInnerBean() {
		BeanDefinition beanDefinition = BeanDefinitionBuilder.rootBeanDefinition(MultiConstructorSample.class)
				.addConstructorArgValue(BeanDefinitionBuilder.rootBeanDefinition(Locale.class, "getDefault").getBeanDefinition())
				.getBeanDefinition();
		Executable executable = detectBeanInstanceExecutable(new DefaultListableBeanFactory(), beanDefinition);
		assertThat(executable).isNull();
	}

	@Test
	void detectBeanInstanceExecutableWithFactoryBeanSetInBeanClass() {
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		RootBeanDefinition beanDefinition = new RootBeanDefinition();
		beanDefinition.setTargetType(ResolvableType.forClassWithGenerics(NumberHolder.class, Integer.class));
		beanDefinition.setBeanClass(NumberHolderFactoryBean.class);
		Executable executable = detectBeanInstanceExecutable(beanFactory, beanDefinition);
		assertThat(executable).isNotNull().isEqualTo(NumberHolderFactoryBean.class.getDeclaredConstructors()[0]);
	}

	@Test
	void detectBeanInstanceExecutableWithFactoryBeanSetInBeanClassAndNoResolvableType() {
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		RootBeanDefinition beanDefinition = new RootBeanDefinition();
		beanDefinition.setBeanClass(NumberHolderFactoryBean.class);
		Executable executable = detectBeanInstanceExecutable(beanFactory, beanDefinition);
		assertThat(executable).isNotNull().isEqualTo(NumberHolderFactoryBean.class.getDeclaredConstructors()[0]);
	}

	@Test
	void detectBeanInstanceExecutableWithFactoryBeanSetInBeanClassThatDoesNotMatchTargetType() {
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		RootBeanDefinition beanDefinition = new RootBeanDefinition();
		beanDefinition.setTargetType(ResolvableType.forClassWithGenerics(NumberHolder.class, String.class));
		beanDefinition.setBeanClass(NumberHolderFactoryBean.class);
		assertThatIllegalStateException().isThrownBy(() -> detectBeanInstanceExecutable(beanFactory, beanDefinition))
				.withMessageContaining("Incompatible target type").withMessageContaining(NumberHolder.class.getName())
				.withMessageContaining(NumberHolderFactoryBean.class.getName());
	}

	private Executable detectBeanInstanceExecutable(DefaultListableBeanFactory beanFactory, BeanDefinition beanDefinition) {
		return new BeanInstanceExecutableSupplier(beanFactory).detectBeanInstanceExecutable(beanDefinition);
	}

	static class IntegerFactoryBean implements FactoryBean<Integer> {

		@Override
		public Integer getObject() {
			return 42;
		}

		@Override
		public Class<?> getObjectType() {
			return Integer.class;
		}
	}

	@SuppressWarnings("unused")
	static class MultiConstructorSample {

		MultiConstructorSample(String name) {
		}

		MultiConstructorSample(Integer value) {
		}

	}

	@SuppressWarnings("unused")
	static class MultiConstructorArraySample {

		public MultiConstructorArraySample(String... names) {
		}

		public MultiConstructorArraySample(Integer... values) {
		}
	}

	@SuppressWarnings("unused")
	static class MultiConstructorListSample {

		public MultiConstructorListSample(String name) {
		}

		public MultiConstructorListSample(List<Integer> values) {
		}

	}

	interface DummyInterface {

		static String of(Object o) {
			return o.toString();
		}
	}

	static class DummySampleFactory implements DummyInterface {

		static String of(Integer value) {
			return value.toString();
		}

		private String of(String ignored) {
			return ignored;
		}
	}

}
