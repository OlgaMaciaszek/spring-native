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

package org.springframework.context.bootstrap.generator;

import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.springframework.boot.autoconfigure.context.ConfigurationPropertiesAutoConfiguration;
import org.springframework.boot.autoconfigure.info.ProjectInfoAutoConfiguration;
import org.springframework.context.bootstrap.generator.sample.SimpleConfiguration;
import org.springframework.context.bootstrap.generator.sample.autoconfigure.AutoConfigurationPackagesConfiguration;
import org.springframework.context.bootstrap.generator.sample.generic.GenericConfiguration;
import org.springframework.context.bootstrap.generator.sample.generic.GenericObjectProviderConfiguration;
import org.springframework.context.bootstrap.generator.sample.generic.Repository;
import org.springframework.context.bootstrap.generator.sample.generic.RepositoryHolder;
import org.springframework.context.bootstrap.generator.sample.infrastructure.ArgumentValueRegistrarConfiguration;
import org.springframework.context.bootstrap.generator.sample.metadata.MetadataConfiguration;
import org.springframework.context.bootstrap.generator.sample.visibility.ProtectedConfigurationImport;
import org.springframework.context.bootstrap.generator.sample.visibility.ProtectedConstructorParameterConfiguration;
import org.springframework.context.bootstrap.generator.sample.visibility.ProtectedMethodParameterConfiguration;
import org.springframework.context.bootstrap.generator.sample.visibility.PublicInnerClassConfigurationImport;
import org.springframework.context.bootstrap.generator.sample.visibility.PublicOuterClassConfiguration;
import org.springframework.context.bootstrap.generator.test.ContextBootstrapGeneratorTester;
import org.springframework.context.bootstrap.generator.test.ContextBootstrapStructure;
import org.springframework.context.support.GenericApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ContextBootstrapGenerator}.
 *
 * @author Stephane Nicoll
 */
class ContextBootstrapGeneratorTests {

	private ContextBootstrapGeneratorTester generatorTester;

	@BeforeEach
	void setup(@TempDir Path directory) {
		this.generatorTester = new ContextBootstrapGeneratorTester(directory);
	}

	@Test
	void bootstrapClassGeneratesStructure() {
		ContextBootstrapStructure structure = this.generatorTester.generate();
		assertThat(structure).contextBootstrapInitializer().lines().containsSubsequence("public class ContextBootstrapInitializer implements ApplicationContextInitializer<GenericApplicationContext> {",
				"  @Override",
				"  public void initialize(GenericApplicationContext context) {", "  }", "}");
		assertThat(structure).contextBootstrapInitializer().contains("import " + GenericApplicationContext.class.getName() + ";");
	}

	@Test
	void bootstrapClassRegisterInfrastructure() {
		ContextBootstrapStructure structure = this.generatorTester.generate();
		assertThat(structure).contextBootstrapInitializer().contains("// infrastructure");
	}

	@Test
	void boostrapClassRegisterReflectionMetadata() {
		ContextBootstrapStructure structure = this.generatorTester.generate(SimpleConfiguration.class);
		assertThat(structure).hasClassDescriptor(SimpleConfiguration.class);
	}

	@Test
	void bootstrapClassWithBeanMethodAndNoParameter() {
		ContextBootstrapStructure structure = this.generatorTester.generate(SimpleConfiguration.class);
		assertThat(structure).contextBootstrapInitializer().removeIndent(2).lines().contains(
				"BeanDefinitionRegistrar.of(\"simpleConfiguration\", SimpleConfiguration.class)",
				"    .instanceSupplier(() -> new SimpleConfiguration()).register(context);",
				"BeanDefinitionRegistrar.of(\"stringBean\", String.class).withFactoryMethod(SimpleConfiguration.class, \"stringBean\")",
				"    .instanceSupplier(() -> context.getBean(SimpleConfiguration.class).stringBean()).register(context);",
				"BeanDefinitionRegistrar.of(\"integerBean\", Integer.class).withFactoryMethod(SimpleConfiguration.class, \"integerBean\")",
				"    .instanceSupplier(() -> context.getBean(SimpleConfiguration.class).integerBean()).register(context);");
	}

	@Test
	void bootstrapClassWithAutoConfiguration() {
		ContextBootstrapStructure structure = this.generatorTester.generate(ProjectInfoAutoConfiguration.class);
		// NOTE: application context runner does not register auto-config as FQNs
		assertThat(structure).contextBootstrapInitializer().contains(
				"BeanDefinitionRegistrar.of(\"projectInfoAutoConfiguration\", ProjectInfoAutoConfiguration.class).withConstructor(ProjectInfoProperties.class)",
				".instanceSupplier((instanceContext) -> instanceContext.create(context, (attributes) -> new ProjectInfoAutoConfiguration(attributes.get(0)))).register(context);",
				"BeanDefinitionRegistrar.of(\"spring.info-org.springframework.boot.autoconfigure.info.ProjectInfoProperties\", ProjectInfoProperties.class)",
				".instanceSupplier(() -> new ProjectInfoProperties()).register(context);");
	}

	@Test
	void bootstrapClassWithAutoConfigurationPackages() {
		ContextBootstrapStructure structure = this.generatorTester.generate(AutoConfigurationPackagesConfiguration.class);
		assertThat(structure).contextBootstrapInitializer()
				.contains("ContextBootstrapInitializer.registerAutoConfigurationPackages_BasePackages(context)");
		assertThat(structure).contextBootstrapInitializer("org.springframework.boot.autoconfigure").contains(
				"BeanDefinitionRegistrar.of(\"org.springframework.boot.autoconfigure.AutoConfigurationPackages\", AutoConfigurationPackages.BasePackages.class)",
				".instanceSupplier(() -> new AutoConfigurationPackages.BasePackages(new String[] { \"org.springframework.context.bootstrap.generator.sample.autoconfigure\" }))"
						+ ".customize((bd) -> bd.setRole(2)).register(context);");
	}

	@Test
	void bootstrapClassWithConfigurationProperties() {
		ContextBootstrapStructure structure = this.generatorTester.generate(ConfigurationPropertiesAutoConfiguration.class);
		assertThat(structure).contextBootstrapInitializer().removeIndent(2).lines().contains(
				"BeanDefinitionRegistrar.of(\"org.springframework.boot.context.properties.EnableConfigurationPropertiesRegistrar.methodValidationExcludeFilter\", MethodValidationExcludeFilter.class)",
				"    .instanceSupplier(() -> MethodValidationExcludeFilter.byAnnotation(ConfigurationProperties.class)).customize((bd) -> bd.setRole(2)).register(context);");
	}

	@Test
	void bootstrapClassWithPrimaryBean() {
		ContextBootstrapStructure structure = this.generatorTester.generate(MetadataConfiguration.class);
		assertThat(structure).contextBootstrapInitializer().contains(
				"BeanDefinitionRegistrar.of(\"primaryBean\", String.class).withFactoryMethod(MetadataConfiguration.class, \"primaryBean\")",
				"    .instanceSupplier(() -> context.getBean(MetadataConfiguration.class).primaryBean())"
						+ ".customize((bd) -> bd.setPrimary(true)).register(context);");
	}

	@Test
	void bootstrapClassWithRoleInfrastructureBean() {
		ContextBootstrapStructure structure = this.generatorTester.generate(MetadataConfiguration.class);
		assertThat(structure).contextBootstrapInitializer().contains(
				"BeanDefinitionRegistrar.of(\"infrastructureBean\", String.class).withFactoryMethod(MetadataConfiguration.class, \"infrastructureBean\")",
				"    .instanceSupplier(() -> context.getBean(MetadataConfiguration.class).infrastructureBean()).customize((bd) -> bd.setRole(2)).register(context);");
	}

	@Test
	void bootstrapClassWithPackageProtectedConfiguration() {
		ContextBootstrapStructure structure = this.generatorTester.generate(ProtectedConfigurationImport.class);
		assertThat(structure)
				.contextBootstrapInitializer("org.springframework.context.bootstrap.generator.sample.visibility")
				.removeIndent(1).lines().containsSequence(
				"public static void registerProtectedConfiguration_anotherStringBean(",
				"    GenericApplicationContext context) {",
				"  BeanDefinitionRegistrar.of(\"anotherStringBean\", String.class).withFactoryMethod(ProtectedConfiguration.class, \"anotherStringBean\")",
				"      .instanceSupplier(() -> context.getBean(ProtectedConfiguration.class).anotherStringBean()).register(context);",
				"}");
		assertThat(structure).contextBootstrapInitializer().contains(
				"ContextBootstrapInitializer.registerProtectedConfiguration(context);",
				"ContextBootstrapInitializer.registerProtectedConfiguration_anotherStringBean(context);");
	}

	@Test
	void bootstrapClassWithPublicInnerOnPackageProtectedOuterConfiguration() {
		ContextBootstrapStructure structure = this.generatorTester.generate(PublicInnerClassConfigurationImport.class);
		assertThat(structure)
				.contextBootstrapInitializer("org.springframework.context.bootstrap.generator.sample.visibility")
				.removeIndent(1).lines().containsSequence(
				"public static void registerPublicInnerClassConfiguration_InnerConfiguration(",
				"    GenericApplicationContext context) {",
				"  BeanDefinitionRegistrar.of(\"org.springframework.context.bootstrap.generator.sample.visibility.PublicInnerClassConfiguration$InnerConfiguration\", PublicInnerClassConfiguration.InnerConfiguration.class)",
				"      .instanceSupplier(() -> new PublicInnerClassConfiguration.InnerConfiguration()).register(context);",
				"}");
		assertThat(structure).contextBootstrapInitializer().contains(
				"ContextBootstrapInitializer.registerPublicInnerClassConfiguration(context);",
				"ContextBootstrapInitializer.registerPublicInnerClassConfiguration_InnerConfiguration(context);",
				"ContextBootstrapInitializer.registerInnerConfiguration_innerBean(context);");
	}

	@Test
	void bootstrapClassWithPackageProtectedInnerConfiguration() {
		ContextBootstrapStructure structure = this.generatorTester.generate(PublicOuterClassConfiguration.class);
		assertThat(structure)
				.contextBootstrapInitializer("org.springframework.context.bootstrap.generator.sample.visibility")
				.removeIndent(1).lines().containsSequence(
				"public static void registerProtectedInnerConfiguration_anotherInnerBean(",
				"    GenericApplicationContext context) {",
				"  BeanDefinitionRegistrar.of(\"anotherInnerBean\", String.class).withFactoryMethod(PublicOuterClassConfiguration.ProtectedInnerConfiguration.class, \"anotherInnerBean\")",
				"      .instanceSupplier(() -> context.getBean(PublicOuterClassConfiguration.ProtectedInnerConfiguration.class).anotherInnerBean()).register(context);",
				"}");
		assertThat(structure).contextBootstrapInitializer().contains(
				"BeanDefinitionRegistrar.of(\"publicOuterClassConfiguration\", PublicOuterClassConfiguration.class)",
				"    .instanceSupplier(() -> new PublicOuterClassConfiguration()).register(context);",
				"ContextBootstrapInitializer.registerPublicOuterClassConfiguration_ProtectedInnerConfiguration(context);",
				"ContextBootstrapInitializer.registerProtectedInnerConfiguration_anotherInnerBean(context);");
	}

	@Test
	void bootstrapClassWithProtectedConstructorParameter() {
		ContextBootstrapStructure structure = this.generatorTester.generate(ProtectedConstructorParameterConfiguration.class);
		assertThat(structure)
				.contextBootstrapInitializer("org.springframework.context.bootstrap.generator.sample.visibility")
				.removeIndent(1).lines().containsSequence(
				"public static void registerProtectedType(GenericApplicationContext context) {",
				"  BeanDefinitionRegistrar.of(\"org.springframework.context.bootstrap.generator.sample.visibility.ProtectedType\", ProtectedType.class)",
				"      .instanceSupplier(() -> new ProtectedType()).register(context);",
				"}");
  		assertThat(structure).contextBootstrapInitializer().contains(
				"BeanDefinitionRegistrar.of(\"protectedConstructorParameterConfiguration\", ProtectedConstructorParameterConfiguration.class)",
				"    .instanceSupplier(() -> new ProtectedConstructorParameterConfiguration()).register(context);",
				"ContextBootstrapInitializer.registerProtectedParameter(context);");
	}

	@Test
	void bootstrapClassWithProtectedMethodParameter() {
		ContextBootstrapStructure structure = this.generatorTester.generate(ProtectedMethodParameterConfiguration.class);
		assertThat(structure)
				.contextBootstrapInitializer("org.springframework.context.bootstrap.generator.sample.visibility")
				.removeIndent(1).lines().containsSequence(
				"public static void registerProtectedMethodParameterConfiguration_protectedParameter(",
				"    GenericApplicationContext context) {",
				"  BeanDefinitionRegistrar.of(\"protectedParameter\", ProtectedParameter.class).withFactoryMethod(ProtectedMethodParameterConfiguration.class, \"protectedParameter\", ProtectedType.class)",
				"      .instanceSupplier((instanceContext) -> instanceContext.create(context, (attributes) -> context.getBean(ProtectedMethodParameterConfiguration.class).protectedParameter(attributes.get(0)))).register(context);",
				"}");
		assertThat(structure).contextBootstrapInitializer().contains(
				"BeanDefinitionRegistrar.of(\"protectedMethodParameterConfiguration\", ProtectedMethodParameterConfiguration.class)",
				"    .instanceSupplier(() -> new ProtectedMethodParameterConfiguration()).register(context);",
				"ContextBootstrapInitializer.registerProtectedMethodParameterConfiguration_protectedParameter(context);");
	}

	@Test
	void bootstrapClassWithProtectedMethodGenericParameter() {
		ContextBootstrapStructure structure = this.generatorTester.generate(ProtectedMethodParameterConfiguration.class);
		assertThat(structure)
				.contextBootstrapInitializer("org.springframework.context.bootstrap.generator.sample.visibility")
				.removeIndent(1).lines().containsSequence(
				"public static void registerProtectedMethodParameterConfiguration_protectedGenericParameter(",
				"    GenericApplicationContext context) {",
				"  BeanDefinitionRegistrar.of(\"protectedGenericParameter\", ProtectedParameter.class).withFactoryMethod(ProtectedMethodParameterConfiguration.class, \"protectedGenericParameter\", ObjectProvider.class)",
				"      .instanceSupplier((instanceContext) -> instanceContext.create(context, (attributes) -> context.getBean(ProtectedMethodParameterConfiguration.class).protectedGenericParameter(attributes.get(0)))).register(context);",
				"}");
		assertThat(structure).contextBootstrapInitializer().contains(
				"BeanDefinitionRegistrar.of(\"protectedMethodParameterConfiguration\", ProtectedMethodParameterConfiguration.class)",
				"    .instanceSupplier(() -> new ProtectedMethodParameterConfiguration()).register(context);",
				"ContextBootstrapInitializer.registerProtectedMethodParameterConfiguration_protectedGenericParameter(context);");
	}

	@Test
	void bootstrapClassWithSimpleGeneric() {
		ContextBootstrapStructure structure = this.generatorTester.generate(GenericConfiguration.class);
		assertThat(structure).contextBootstrapInitializer().contains(
				"BeanDefinitionRegistrar.of(\"stringRepository\", ResolvableType.forClassWithGenerics(Repository.class, String.class)).withFactoryMethod(GenericConfiguration.class, \"stringRepository\")",
				"    .instanceSupplier(() -> context.getBean(GenericConfiguration.class).stringRepository()).register(context);");
	}

	@Test
	void bootstrapClassWithObjectProviderTargetGeneric() {
		ContextBootstrapStructure structure = this.generatorTester.generate(
				GenericConfiguration.class, GenericObjectProviderConfiguration.class);
		assertThat(structure).contextBootstrapInitializer().removeIndent(2).lines().contains(
				"BeanDefinitionRegistrar.of(\"repositoryId\", String.class).withFactoryMethod(GenericObjectProviderConfiguration.class, \"repositoryId\", ObjectProvider.class)",
				"    .instanceSupplier((instanceContext) -> instanceContext.create(context, (attributes) -> context.getBean(GenericObjectProviderConfiguration.class).repositoryId(attributes.get(0)))).register(context);");
	}

	@Test
	@Disabled("Need to resolve constructor based on argument values")
	void bootstrapClassWithArgumentValue() {
		ContextBootstrapStructure structure = this.generatorTester.generate(ArgumentValueRegistrarConfiguration.class);
		assertThat(structure).contextBootstrapInitializer().contains(
				"BeanDefinitionRegistrar.of(\"argumentValueString\", String.class, () -> new String(new char[] { 'a', ' ', 't', 'e', 's', 't' }, 2, 4));");
	}

	@Test
	void bootstrapClassWithExcludeDoesNotRegisterExcludedType() {
		ContextBootstrapStructure structure = this.generatorTester.withExcludeTypes(RepositoryHolder.class)
				.generate(Repository.class, RepositoryHolder.class);
		assertThat(structure).contextBootstrapInitializer()
				.contains("BeanDefinitionRegistrar.of(\"repository\"")
				.doesNotContain("RepositoryHolder");
	}

}
