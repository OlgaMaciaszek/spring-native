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

package org.springframework.aot.factories;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javax.lang.model.element.Modifier;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import org.springframework.core.NativeDetector;
import org.springframework.nativex.AotOptions;
import org.springframework.util.ClassUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

/**
 * Generate a {@code org.springframework.aot.StaticSpringFactories} class
 * that will be used by a {@link org.springframework.core.io.support.SpringFactoriesLoader} override
 * shipped with this module.
 * <p>Also generates static factory classes for instantiating factories with package private constructors.
 * 
 * @author Brian Clozel
 */
class CodeGenerator {

	private final CodeBlock.Builder staticBlock = CodeBlock.builder();

	private final Map<String, TypeSpec> staticFactoryClasses = new HashMap<>();



	public CodeGenerator(AotOptions aotOptions) {
		// System properties related to AOT options
		if (aotOptions.isRemoveYamlSupport()) {
			staticBlock.addStatement("System.setProperty(\"spring.native.remove-yaml-support\", \"true\")");
		}
		if (aotOptions.isRemoveXmlSupport()) {
			staticBlock.addStatement("System.setProperty(\"spring.xml.ignore\", \"true\")");
		}
		if (aotOptions.isRemoveSpelSupport()) {
			staticBlock.addStatement("System.setProperty(\"spring.spel.ignore\", \"true\")");
		}

		// Make sure we take the native codepath even on the JVM, can be useful with the tracing agent
		// TODO Allow to disable it via a flag
		CodeBlock imageCodeBlock = CodeBlock.builder()
				.beginControlFlow("if (!$T.inNativeImage())", NativeDetector.class)
				.addStatement("System.setProperty(\"org.graalvm.nativeimage.imagecode\", \"runtime\")")
				.endControlFlow()
				.build();
		staticBlock.add(imageCodeBlock);

		CodeBlock hibernateBlock = CodeBlock.builder()
				.beginControlFlow("if ($T.isPresent(\"org.hibernate.Session\", null))", ClassUtils.class)
				.addStatement("System.setProperty(\"hibernate.bytecode.provider\", \"none\")")
				.endControlFlow()
				.build();
		staticBlock.add(hibernateBlock);
	}

	public void writeToStaticBlock(Consumer<CodeBlock.Builder> consumer) {
		consumer.accept(this.staticBlock);
	}

	public TypeSpec getStaticFactoryClass(String packageName) {
		return this.staticFactoryClasses.getOrDefault(packageName, createStaticFactoryClass());
	}

	public void writeToStaticFactoryClass(String packageName, Consumer<TypeSpec.Builder> consumer) {
		TypeSpec staticFactoryClass = this.staticFactoryClasses.getOrDefault(packageName, createStaticFactoryClass());
		TypeSpec.Builder builder = staticFactoryClass.toBuilder();
		consumer.accept(builder);
		this.staticFactoryClasses.put(packageName, builder.build());
	}

	public JavaFile generateStaticSpringFactories() {
		TypeSpec springFactoriesType = createSpringFactoriesType(this.staticBlock.build());
		return JavaFile.builder("org.springframework.aot", springFactoriesType).build();
	}

	public List<JavaFile> generateStaticFactoryClasses() {
		return this.staticFactoryClasses.entrySet().stream()
				.map((specEntry) -> JavaFile.builder(specEntry.getKey(), specEntry.getValue()).build())
				.collect(Collectors.toList());
	}

	private TypeSpec createStaticFactoryClass() {
		return TypeSpec.classBuilder("_FactoryProvider")
				.addModifiers(javax.lang.model.element.Modifier.PUBLIC, javax.lang.model.element.Modifier.ABSTRACT)
				.build();
	}

	private TypeSpec createSpringFactoriesType(CodeBlock staticBlock) {
		ParameterizedTypeName factoriesType = ParameterizedTypeName.get(ClassName.get(MultiValueMap.class),
				TypeName.get(Class.class), ParameterizedTypeName.get(Supplier.class, Object.class));
		FieldSpec factories = FieldSpec.builder(factoriesType, "factories")
				.addModifiers(Modifier.PUBLIC, Modifier.STATIC)
				.initializer("new $T()", LinkedMultiValueMap.class)
				.build();
		ParameterizedTypeName namesType = ParameterizedTypeName.get(MultiValueMap.class, Class.class, String.class);
		FieldSpec names = FieldSpec.builder(namesType, "names")
				.addModifiers(Modifier.PUBLIC, Modifier.STATIC)
				.initializer("new $T()", LinkedMultiValueMap.class)
				.build();
		return TypeSpec.classBuilder("StaticSpringFactories")
				.addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
				.addField(factories)
				.addField(names)
				.addStaticBlock(staticBlock)
				.addJavadoc("Class generated - do not edit this file")
				.build();
	}
}
