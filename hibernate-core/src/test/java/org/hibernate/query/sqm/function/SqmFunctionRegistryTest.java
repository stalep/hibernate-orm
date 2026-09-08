/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.function;

import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SqmFunctionRegistryTest {

	private SqmFunctionRegistry registry;
	private SqmFunctionDescriptor countFn;
	private SqmFunctionDescriptor sumFn;
	private SqmFunctionDescriptor avgFn;
	private SqmFunctionDescriptor jsonObjectFn;
	private SqmFunctionDescriptor arrayAggFn;

	@BeforeEach
	void setUp() {
		registry = new SqmFunctionRegistry();
		countFn = Mockito.mock( SqmFunctionDescriptor.class );
		sumFn = Mockito.mock( SqmFunctionDescriptor.class );
		avgFn = Mockito.mock( SqmFunctionDescriptor.class );
		jsonObjectFn = Mockito.mock( SqmFunctionDescriptor.class );
		arrayAggFn = Mockito.mock( SqmFunctionDescriptor.class );

		registry.register( "count", countFn );
		registry.register( "sum", sumFn );
		registry.register( "avg", avgFn );
		registry.register( "json_object", jsonObjectFn );
		registry.register( "array_agg", arrayAggFn );
	}

	@Test
	void retainOnly_retainsSpecifiedFunctions() {
		registry.retainOnly( Set.of( "count", "sum" ) );

		assertNotNull( registry.findFunctionDescriptor( "count" ) );
		assertNotNull( registry.findFunctionDescriptor( "sum" ) );
		assertNull( registry.findFunctionDescriptor( "avg" ) );
		assertNull( registry.findFunctionDescriptor( "json_object" ) );
		assertNull( registry.findFunctionDescriptor( "array_agg" ) );
	}

	@Test
	void retainOnly_isCaseInsensitive() {
		registry.retainOnly( Set.of( "COUNT", "SUM" ) );

		assertNotNull( registry.findFunctionDescriptor( "count" ) );
		assertNotNull( registry.findFunctionDescriptor( "sum" ) );
		assertNull( registry.findFunctionDescriptor( "avg" ) );
	}

	@Test
	void retainOnly_withEmptySet_removesAll() {
		registry.retainOnly( Set.of() );

		assertNull( registry.findFunctionDescriptor( "count" ) );
		assertNull( registry.findFunctionDescriptor( "sum" ) );
		assertNull( registry.findFunctionDescriptor( "avg" ) );
		assertTrue( registry.getValidFunctionKeys().isEmpty() );
	}

	@Test
	void retainOnly_withAllKeys_isNoOp() {
		registry.retainOnly( Set.of( "count", "sum", "avg", "json_object", "array_agg" ) );

		assertNotNull( registry.findFunctionDescriptor( "count" ) );
		assertNotNull( registry.findFunctionDescriptor( "sum" ) );
		assertNotNull( registry.findFunctionDescriptor( "avg" ) );
		assertNotNull( registry.findFunctionDescriptor( "json_object" ) );
		assertNotNull( registry.findFunctionDescriptor( "array_agg" ) );
		assertEquals( 5, registry.getValidFunctionKeys().size() );
	}

	@Test
	void retainOnly_retainsAlternateKeyWhenTargetIsRetained() {
		// "character_length" is the primary key, "length" is an alias
		SqmFunctionDescriptor charLengthFn = Mockito.mock( SqmFunctionDescriptor.class );
		registry.register( "character_length", charLengthFn );
		registry.registerAlternateKey( "length", "character_length" );

		registry.retainOnly( Set.of( "character_length", "count" ) );

		// Primary key retained
		assertNotNull( registry.findFunctionDescriptor( "character_length" ) );
		// Alias should still work because its target is retained
		assertNotNull( registry.findFunctionDescriptor( "length" ) );
		// Other functions removed
		assertNull( registry.findFunctionDescriptor( "sum" ) );
		assertNull( registry.findFunctionDescriptor( "json_object" ) );
	}

	@Test
	void retainOnly_removesAlternateKeyWhenTargetIsPruned() {
		SqmFunctionDescriptor charLengthFn = Mockito.mock( SqmFunctionDescriptor.class );
		registry.register( "character_length", charLengthFn );
		registry.registerAlternateKey( "length", "character_length" );

		// Retain only "count" — neither "character_length" nor "length" are in the set
		registry.retainOnly( Set.of( "count" ) );

		assertNotNull( registry.findFunctionDescriptor( "count" ) );
		assertNull( registry.findFunctionDescriptor( "character_length" ) );
		// Alias should also be removed since its target was pruned
		assertNull( registry.findFunctionDescriptor( "length" ) );
	}

	@Test
	void retainOnly_prunesSetReturningFunctions() {
		SqmSetReturningFunctionDescriptor unnestFn = Mockito.mock( SqmSetReturningFunctionDescriptor.class );
		SqmSetReturningFunctionDescriptor generateSeriesFn = Mockito.mock( SqmSetReturningFunctionDescriptor.class );
		registry.register( "unnest", unnestFn );
		registry.register( "generate_series", generateSeriesFn );

		registry.retainOnly( Set.of( "count", "unnest" ) );

		assertNotNull( registry.findFunctionDescriptor( "count" ) );
		assertNotNull( registry.findSetReturningFunctionDescriptor( "unnest" ) );
		assertNull( registry.findSetReturningFunctionDescriptor( "generate_series" ) );
	}

	@Test
	void retainOnly_preservesDescriptorInstances() {
		registry.retainOnly( Set.of( "count", "sum" ) );

		// Verify the exact same descriptor instances are returned
		assertEquals( countFn, registry.findFunctionDescriptor( "count" ) );
		assertEquals( sumFn, registry.findFunctionDescriptor( "sum" ) );
	}

	@Test
	void retainOnly_multipleAlternateKeysForSameTarget() {
		SqmFunctionDescriptor charLengthFn = Mockito.mock( SqmFunctionDescriptor.class );
		registry.register( "character_length", charLengthFn );
		registry.registerAlternateKey( "length", "character_length" );
		registry.registerAlternateKey( "char_length", "character_length" );

		registry.retainOnly( Set.of( "character_length" ) );

		assertNotNull( registry.findFunctionDescriptor( "character_length" ) );
		assertNotNull( registry.findFunctionDescriptor( "length" ) );
		assertNotNull( registry.findFunctionDescriptor( "char_length" ) );
		// Others pruned
		assertNull( registry.findFunctionDescriptor( "count" ) );
	}

	@Test
	void retainOnly_calledTwice_furtherPrunes() {
		registry.retainOnly( Set.of( "count", "sum", "avg" ) );

		assertEquals( 3, registry.getValidFunctionKeys().size() );

		registry.retainOnly( Set.of( "count" ) );

		assertEquals( 1, registry.getValidFunctionKeys().size() );
		assertNotNull( registry.findFunctionDescriptor( "count" ) );
		assertNull( registry.findFunctionDescriptor( "sum" ) );
		assertNull( registry.findFunctionDescriptor( "avg" ) );
	}

	// --- Lazy registration tests ---

	/**
	 * Helper: creates a LazyFunctionFactory that registers a mock descriptor
	 * and counts how many times it was called.
	 */
	private SqmFunctionRegistry.LazyFunctionFactory countingFactory(
			AtomicInteger callCount, SqmFunctionDescriptor descriptor) {
		return name -> {
			callCount.incrementAndGet();
			registry.register( name.lowerCaseName(), descriptor );
		};
	}

	@Test
	void registerLazy_notInstantiatedUntilLookup() {
		AtomicInteger callCount = new AtomicInteger( 0 );
		SqmFunctionDescriptor lazyFn = Mockito.mock( SqmFunctionDescriptor.class );

		registry.registerLazy( "lazy_func", countingFactory( callCount, lazyFn ) );

		// Factory should not be called yet
		assertEquals( 0, callCount.get() );

		// Lookup should trigger factory
		SqmFunctionDescriptor result = registry.findFunctionDescriptor( "lazy_func" );
		assertSame( lazyFn, result );
		assertEquals( 1, callCount.get() );
	}

	@Test
	void registerLazy_promotedToEagerAfterFirstLookup() {
		AtomicInteger callCount = new AtomicInteger( 0 );
		SqmFunctionDescriptor lazyFn = Mockito.mock( SqmFunctionDescriptor.class );

		registry.registerLazy( "lazy_func", countingFactory( callCount, lazyFn ) );

		// First lookup triggers factory
		registry.findFunctionDescriptor( "lazy_func" );
		assertEquals( 1, callCount.get() );

		// Second lookup should NOT call factory again (promoted to eager)
		SqmFunctionDescriptor result = registry.findFunctionDescriptor( "lazy_func" );
		assertSame( lazyFn, result );
		assertEquals( 1, callCount.get() );
	}

	@Test
	void registerLazy_includedInValidFunctionKeys() {
		SqmFunctionDescriptor lazyFn = Mockito.mock( SqmFunctionDescriptor.class );
		registry.registerLazy( "lazy_func", name -> registry.register( name.lowerCaseName(), lazyFn ) );

		assertTrue( registry.getValidFunctionKeys().contains( "lazy_func" ) );
	}

	@Test
	void registerLazy_eagerTakesPrecedence() {
		SqmFunctionDescriptor eagerFn = Mockito.mock( SqmFunctionDescriptor.class );
		SqmFunctionDescriptor lazyFn = Mockito.mock( SqmFunctionDescriptor.class );

		registry.register( "func", eagerFn );
		registry.registerLazy( "func", name -> registry.register( name.lowerCaseName(), lazyFn ) );

		// Eager registration should win (since find checks eager map first)
		assertSame( eagerFn, registry.findFunctionDescriptor( "func" ) );
	}

	@Test
	void registerLazy_alternateKeyResolvesLazyTarget() {
		SqmFunctionDescriptor lazyFn = Mockito.mock( SqmFunctionDescriptor.class );

		registry.registerLazy( "char_length", name -> registry.register( name.lowerCaseName(), lazyFn ) );
		registry.registerAlternateKey( "length", "char_length" );

		// Looking up the alias should trigger lazy promotion of the target
		SqmFunctionDescriptor result = registry.findFunctionDescriptor( "length" );
		assertSame( lazyFn, result );

		// Direct lookup should also work (now promoted)
		assertSame( lazyFn, registry.findFunctionDescriptor( "char_length" ) );
	}

	@Test
	void registerLazy_retainOnlyKeepsLazyEntries() {
		SqmFunctionDescriptor keptFn = Mockito.mock( SqmFunctionDescriptor.class );
		SqmFunctionDescriptor prunedFn = Mockito.mock( SqmFunctionDescriptor.class );

		registry.registerLazy( "lazy_kept", name -> registry.register( name.lowerCaseName(), keptFn ) );
		registry.registerLazy( "lazy_pruned", name -> registry.register( name.lowerCaseName(), prunedFn ) );

		registry.retainOnly( Set.of( "count", "lazy_kept" ) );

		assertNotNull( registry.findFunctionDescriptor( "count" ) );
		assertNotNull( registry.findFunctionDescriptor( "lazy_kept" ) );
		assertNull( registry.findFunctionDescriptor( "lazy_pruned" ) );
	}

	@Test
	void registerLazy_mixedEagerAndLazy() {
		SqmFunctionDescriptor lazyFn = Mockito.mock( SqmFunctionDescriptor.class );

		registry.registerLazy( "lazy_func", name -> registry.register( name.lowerCaseName(), lazyFn ) );

		// Eager functions still work
		assertNotNull( registry.findFunctionDescriptor( "count" ) );
		assertNotNull( registry.findFunctionDescriptor( "sum" ) );

		// Lazy function works
		assertSame( lazyFn, registry.findFunctionDescriptor( "lazy_func" ) );

		// Total key count includes both
		assertTrue( registry.getValidFunctionKeys().contains( "count" ) );
		assertTrue( registry.getValidFunctionKeys().contains( "lazy_func" ) );
	}

	@Test
	void registerLazy_sharedFactoryForMultipleFunctions() {
		AtomicInteger callCount = new AtomicInteger( 0 );
		SqmFunctionDescriptor sinFn = Mockito.mock( SqmFunctionDescriptor.class );
		SqmFunctionDescriptor cosFn = Mockito.mock( SqmFunctionDescriptor.class );

		// A single factory instance shared across registrations (like CommonFunctionFactory)
		SqmFunctionRegistry.LazyFunctionFactory sharedFactory = name -> {
			callCount.incrementAndGet();
			switch ( name.lowerCaseName() ) {
				case "sin" -> registry.register( name.lowerCaseName(), sinFn );
				case "cos" -> registry.register( name.lowerCaseName(), cosFn );
			}
		};

		registry.registerLazy( "sin", sharedFactory );
		registry.registerLazy( "cos", sharedFactory );

		assertEquals( 0, callCount.get() );

		// Looking up "sin" should only trigger factory for "sin"
		assertSame( sinFn, registry.findFunctionDescriptor( "sin" ) );
		assertEquals( 1, callCount.get() );

		// Looking up "cos" should trigger factory for "cos"
		assertSame( cosFn, registry.findFunctionDescriptor( "cos" ) );
		assertEquals( 2, callCount.get() );

		// Second lookup of "sin" should not trigger factory again
		assertSame( sinFn, registry.findFunctionDescriptor( "sin" ) );
		assertEquals( 2, callCount.get() );
	}

	@Test
	void registerLazy_varargsBulkRegistration() {
		AtomicInteger callCount = new AtomicInteger( 0 );
		SqmFunctionDescriptor sinFn = Mockito.mock( SqmFunctionDescriptor.class );
		SqmFunctionDescriptor cosFn = Mockito.mock( SqmFunctionDescriptor.class );
		SqmFunctionDescriptor tanFn = Mockito.mock( SqmFunctionDescriptor.class );

		SqmFunctionRegistry.LazyFunctionFactory factory = name -> {
			callCount.incrementAndGet();
			switch ( name.lowerCaseName() ) {
				case "sin" -> registry.register( name.lowerCaseName(), sinFn );
				case "cos" -> registry.register( name.lowerCaseName(), cosFn );
				case "tan" -> registry.register( name.lowerCaseName(), tanFn );
			}
		};

		// Bulk registration via single-name calls
		registry.registerLazy( "sin", factory );
		registry.registerLazy( "cos", factory );
		registry.registerLazy( "tan", factory );

		// All names should be in valid keys
		assertTrue( registry.getValidFunctionKeys().contains( "sin" ) );
		assertTrue( registry.getValidFunctionKeys().contains( "cos" ) );
		assertTrue( registry.getValidFunctionKeys().contains( "tan" ) );

		// Factory should not be called yet
		assertEquals( 0, callCount.get() );

		// Each lookup triggers factory independently
		assertSame( sinFn, registry.findFunctionDescriptor( "sin" ) );
		assertEquals( 1, callCount.get() );

		assertSame( tanFn, registry.findFunctionDescriptor( "tan" ) );
		assertEquals( 2, callCount.get() );

		// "cos" still lazy, not yet triggered
		assertSame( cosFn, registry.findFunctionDescriptor( "cos" ) );
		assertEquals( 3, callCount.get() );
	}

	@Test
	void registerLazy_commonFunctionEnum() {
		AtomicInteger callCount = new AtomicInteger( 0 );
		SqmFunctionDescriptor cotFn = Mockito.mock( SqmFunctionDescriptor.class );
		SqmFunctionDescriptor piFn = Mockito.mock( SqmFunctionDescriptor.class );

		SqmFunctionRegistry.LazyFunctionFactory factory = name -> {
			callCount.incrementAndGet();
			registry.register( name.lowerCaseName(), name.function() == CommonFunction.COT ? cotFn : piFn );
		};

		// Register using CommonFunction enum constants
		registry.registerLazy( factory, CommonFunction.COT, CommonFunction.PI );

		assertEquals( 0, callCount.get() );
		assertTrue( registry.getValidFunctionKeys().contains( "cot" ) );
		assertTrue( registry.getValidFunctionKeys().contains( "pi" ) );

		// Lookup triggers factory with correct FunctionName
		assertSame( cotFn, registry.findFunctionDescriptor( "cot" ) );
		assertEquals( 1, callCount.get() );

		assertSame( piFn, registry.findFunctionDescriptor( "pi" ) );
		assertEquals( 2, callCount.get() );
	}

	@Test
	void registerLazy_commonFunctionAll() {
		SqmFunctionDescriptor mockFn = Mockito.mock( SqmFunctionDescriptor.class );

		SqmFunctionRegistry.LazyFunctionFactory factory = name ->
				registry.register( name.lowerCaseName(), mockFn );

		// Register all common functions at once
		registry.registerLazy( factory, CommonFunction.all() );

		// All common functions should be in valid keys
		for ( CommonFunction cf : CommonFunction.all() ) {
			assertTrue( registry.getValidFunctionKeys().contains( cf.lowerCaseName() ),
					cf.lowerCaseName() + " should be in valid keys" );
		}

		// UNKNOWN should not be registered
		assertTrue( !registry.getValidFunctionKeys().contains( "unknown" ) );
	}

	@Test
	void registerLazy_closeClears() {
		SqmFunctionDescriptor lazyFn = Mockito.mock( SqmFunctionDescriptor.class );
		registry.registerLazy( "lazy_func", name -> registry.register( name.lowerCaseName(), lazyFn ) );

		registry.close();

		assertNull( registry.findFunctionDescriptor( "lazy_func" ) );
		assertNull( registry.findFunctionDescriptor( "count" ) );
	}
}
