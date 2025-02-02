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

package org.springframework.context.bootstrap.generator.test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.assertj.core.api.AbstractPathAssert;
import org.assertj.core.api.MapAssert;
import org.assertj.core.api.PathAssert;

import org.springframework.nativex.domain.reflect.ClassDescriptor;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Assertions for a generated source structure.
 *
 * @author Stephane Nicoll
 */
public class ContextBootstrapAssert extends AbstractPathAssert<ContextBootstrapAssert> {

	private final String packageName;

	private final MapAssert<String, ClassDescriptor> classDescriptors;

	public ContextBootstrapAssert(Path projectDirectory, String packageName,
			List<ClassDescriptor> classDescriptors) {
		super(projectDirectory, ContextBootstrapAssert.class);
		this.packageName = packageName;
		this.classDescriptors = new MapAssert<>(classDescriptors.stream().collect(Collectors.toMap(ClassDescriptor::getName, (desc) -> desc)));
	}

	public ContextBootstrapAssert hasSource(String packageName, String name) {
		validateAndGetAsset(this.actual, packageName, name);
		return this.myself;
	}

	public ContextBootstrapAssert hasClassDescriptor(Class<?> type) {
		this.classDescriptors.containsKey(type.getName());
		return this.myself;
	}

	public ContextBootstrapAssert hasClassDescriptor(Class<?> type, Consumer<ClassDescriptor> assertions) {
		this.classDescriptors.hasEntrySatisfying(type.getName(), assertions);
		return this.myself;
	}

	public TextAssert contextBootstrapInitializer() {
		return contextBootstrapInitializer(this.packageName);
	}

	public TextAssert contextBootstrapInitializer(String packageName) {
		return source(packageName, "ContextBootstrapInitializer");
	}

	public TextAssert source(String packageName, String name) {
		return new TextAssert(readContent(validateAndGetAsset(this.actual, packageName, name)));
	}

	private Path validateAndGetAsset(Path baseDir, String packageName, String name) {
		Path source = resolveSource(baseDir, packageName, name);
		new PathAssert(source).as("Source '%s.java' not found in package '%s'", name, packageName).exists()
				.isRegularFile();
		return source;
	}

	private Path resolveSource(Path baseDir, String packageName, String name) {
		return baseDir.resolve(createSourceRelativePath(packageName, name));
	}

	private String createSourceRelativePath(String packageName, String name) {
		return packageToPath(packageName) + "/" + name + ".java";
	}

	private static String packageToPath(String packageName) {
		String packagePath = packageName.replace(".", "/");
		return StringUtils.trimTrailingCharacter(packagePath, '/');
	}

	public static String readContent(Path source) {
		assertThat(source).isRegularFile();
		try (InputStream stream = Files.newInputStream(source)) {
			return StreamUtils.copyToString(stream, StandardCharsets.UTF_8);
		}
		catch (IOException ex) {
			throw new IllegalStateException(ex);
		}
	}

}
