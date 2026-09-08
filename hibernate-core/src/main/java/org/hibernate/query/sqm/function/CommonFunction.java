/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.function;

import java.util.List;
import java.util.Locale;

/**
 * Enum of function names defined in
 * {@link org.hibernate.dialect.function.CommonFunctionFactory}.
 * Each constant maps to a lowercase HQL function name and can be
 * used for type-safe lazy registration via
 * {@link SqmFunctionRegistry#registerLazy(SqmFunctionRegistry.LazyFunctionFactory, FunctionName...)}.
 * <p>
 * Dialects can register all common functions lazily with a single call:
 * <pre>
 * functionRegistry.registerLazy(functionFactory, CommonFunction.all());
 * </pre>
 * and then eagerly override specific functions that need dialect-specific
 * construction arguments.
 *
 * @see FunctionName
 * @see SqmFunctionRegistry.LazyFunctionFactory
 */
public enum CommonFunction implements FunctionName {

	// trigonometric / geometric
	COT,
	RADIANS,
	DEGREES,
	LOG,
	LOG10,
	TANH,
	SINH,
	COSH,
	ACOSH,
	ASINH,
	ATANH,
	CBRT,
	PI,

	// string functions
	LTRIM,
	RTRIM,
	REPEAT,
	INITCAP,
	SUBSTR,
	SUBSTRING,
	REVERSE,
	TRANSLATE,
	SOUNDEX,
	ASCII,
	CHR,
	POSITION,
	LOCATE,

	// numeric / conversion
	MOD,
	TO_NUMBER,
	TO_CHAR,
	TO_DATE,
	TO_TIMESTAMP,

	// bitwise operators (binary, not aggregate)
	BITAND,
	BITOR,
	BITXOR,
	BITNOT,

	// bitwise aggregate functions
	BIT_AND,
	BIT_OR,

	// boolean aggregates
	BOOL_AND,
	BOOL_OR,

	// statistical aggregates
	CORR,
	REGR_AVGX,
	REGR_AVGY,
	REGR_COUNT,
	REGR_INTERCEPT,
	REGR_R2,
	REGR_SLOPE,
	REGR_SXX,
	REGR_SXY,
	REGR_SYY,

	// window functions
	ROW_NUMBER,
	LAG,
	LEAD,
	FIRST_VALUE,
	LAST_VALUE,
	NTH_VALUE,

	// aggregate
	LISTAGG,

	// date/time
	LOCALTIME,
	LOCALTIMESTAMP,
	LOCAL_TIME,
	LOCAL_DATETIME,
	DATE_TRUNC,

	// statistics
	MEDIAN,
	STDDEV,
	STDDEV_POP,
	STDDEV_SAMP,
	VARIANCE,
	VAR_POP,
	VAR_SAMP,
	COVAR_POP,
	COVAR_SAMP,

	// string manipulation
	INSERT,
	OVERLAY,

	// date construction
	MAKE_DATE,
	MAKE_TIME,
	MAKE_TIMESTAMP,
	MAKE_TIMESTAMPTZ,

	// ordered set aggregates
	MODE,
	PERCENTILE_CONT,
	PERCENTILE_DISC,

	// hypothetical set aggregates
	RANK,
	DENSE_RANK,
	PERCENT_RANK,
	CUME_DIST,

	// XML
	XMLELEMENT,
	XMLCOMMENT,
	XMLFOREST,
	XMLCONCAT,
	XMLPI,
	XMLQUERY,
	XMLEXISTS,
	XMLAGG,

	// array functions
	ARRAY,
	ARRAY_LIST,
	ARRAY_AGG,
	ARRAY_POSITION,
	ARRAY_POSITIONS,
	ARRAY_POSITIONS_LIST,
	ARRAY_LENGTH,
	ARRAY_CONCAT,
	ARRAY_PREPEND,
	ARRAY_APPEND,
	ARRAY_CONTAINS,
	ARRAY_CONTAINS_NULLABLE,
	ARRAY_INCLUDES,
	ARRAY_INCLUDES_NULLABLE,
	ARRAY_INTERSECTS,
	ARRAY_INTERSECTS_NULLABLE,
	ARRAY_GET,
	ARRAY_SET,
	ARRAY_REMOVE,
	ARRAY_REMOVE_INDEX,
	ARRAY_SLICE,
	ARRAY_REPLACE,
	ARRAY_TRIM,
	ARRAY_FILL,
	ARRAY_FILL_LIST,
	ARRAY_TO_STRING,

	// JSON mutation (version-independent)
	JSON_SET,
	JSON_REMOVE,
	JSON_REPLACE,
	JSON_INSERT,
	JSON_MERGEPATCH,
	JSON_ARRAY_APPEND,
	JSON_ARRAY_INSERT,

	/**
	 * Sentinel for dialect-specific functions not in the common set.
	 */
	UNKNOWN;

	private static final List<CommonFunction> ALL;

	static {
		final var values = values();
		final var list = new java.util.ArrayList<CommonFunction>( values.length - 1 );
		for ( CommonFunction f : values ) {
			if ( f != UNKNOWN ) {
				list.add( f );
			}
		}
		ALL = List.copyOf( list );
	}

	private final String lowerCaseName;

	CommonFunction() {
		this.lowerCaseName = name().toLowerCase( Locale.ROOT );
	}

	@Override
	public String lowerCaseName() {
		return lowerCaseName;
	}

	@Override
	public CommonFunction function() {
		return this;
	}

	/**
	 * Returns all common function constants, excluding {@link #UNKNOWN}.
	 * Use this to lazily register all common functions in one call:
	 * <pre>
	 * functionRegistry.registerLazy(functionFactory, CommonFunction.all());
	 * </pre>
	 */
	public static List<CommonFunction> all() {
		return ALL;
	}
}
