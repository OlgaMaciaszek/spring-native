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

package org.springframework.context.bootstrap.generator.sample.factory;

import org.springframework.beans.factory.FactoryBean;

public class NumberHolderFactoryBean<T extends Number> implements FactoryBean<NumberHolder<T>> {

	private T number;

	public void setNumber(T number) {
		this.number = number;
	}

	@Override
	public NumberHolder<T> getObject() {
		return new NumberHolder<>(this.number);
	}

	@Override
	public Class<?> getObjectType() {
		return NumberHolder.class;
	}

}
