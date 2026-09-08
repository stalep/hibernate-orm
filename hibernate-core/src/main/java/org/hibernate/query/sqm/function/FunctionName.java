/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.function;

/**
 * Represents the name of a function that can be registered in the
 * {@link SqmFunctionRegistry}. Implementations include the
 * {@link CommonFunction} enum for functions defined in
 * {@link org.hibernate.dialect.function.CommonFunctionFactory},
 * and dialect-specific implementations for functions not covered
 * by the common set.
 *
 * @see CommonFunction
 * @see SqmFunctionRegistry#registerLazy(SqmFunctionRegistry.LazyFunctionFactory, FunctionName...)
 */
public interface FunctionName {

	/**
	 * The lowercase name used for registration and lookup.
	 */
	String lowerCaseName();

	/**
	 * The common function constant, or {@link CommonFunction#UNKNOWN}
	 * if this is a dialect-specific function not in the common set.
	 */
	CommonFunction function();
}
