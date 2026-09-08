/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.sqm.function;

import java.util.Set;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.query.sqm.function.SqmFunctionRegistry;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Integration test for lazy function registration via {@link SqmFunctionRegistry.LazyFunctionFactory}.
 * Verifies that lazily registered functions are properly promoted to eager on first lookup
 * and that all registered function keys resolve without error.
 */
@DomainModel(annotatedClasses = LazyFunctionRegistrationTest.TestEntity.class)
@SessionFactory
@JiraKey("HHH-20839")
public class LazyFunctionRegistrationTest {

	/**
	 * Verify that every function key in the registry resolves to a non-null descriptor.
	 * This catches name mismatches between the LazyFunctionFactory switch cases and
	 * the function names registered by the dialect.
	 */
	@Test
	void allRegisteredFunctionKeysResolve(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			SessionFactoryImplementor sfi = session.getSessionFactory();
			SqmFunctionRegistry registry = sfi.getQueryEngine().getSqmFunctionRegistry();

			Set<String> keys = registry.getValidFunctionKeys();
			assertTrue( keys.size() > 50, "Dialect should register many functions, got: " + keys.size() );

			for ( String key : keys ) {
				var descriptor = registry.findFunctionDescriptor( key );
				if ( descriptor == null ) {
					// Some keys may be set-returning functions, not regular functions
					var srDescriptor = registry.findSetReturningFunctionDescriptor( key );
					if ( srDescriptor == null ) {
						fail( "Function key '" + key + "' resolved to null for both regular and set-returning lookup" );
					}
				}
			}
		} );
	}

	/**
	 * Verify that looking up one function from a multi-name registration method
	 * (e.g., stddevPopSamp registers stddev_pop and stddev_samp) does not prevent
	 * the sibling functions from being looked up afterwards.
	 * Only tests groups where all members are registered by the current dialect.
	 */
	@Test
	void multiNameMethodSiblingsResolve(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			SessionFactoryImplementor sfi = session.getSessionFactory();
			SqmFunctionRegistry registry = sfi.getQueryEngine().getSqmFunctionRegistry();
			Set<String> keys = registry.getValidFunctionKeys();

			// These are all registered by common multi-name methods.
			// Verify each group resolves independently (if the dialect registers them).
			String[][] groups = {
					// stddevPopSamp registers both
					{ "stddev_pop", "stddev_samp" },
					// varPopSamp registers both
					{ "var_pop", "var_samp" },
					// covarPopSamp registers both
					{ "covar_pop", "covar_samp" },
			};

			for ( String[] group : groups ) {
				// Only test groups where all members are registered
				boolean allPresent = true;
				for ( String name : group ) {
					if ( !keys.contains( name ) ) {
						allPresent = false;
						break;
					}
				}
				if ( allPresent ) {
					for ( String name : group ) {
						var descriptor = registry.findFunctionDescriptor( name );
						assertNotNull( descriptor, "Function '" + name + "' should resolve" );
					}
				}
			}
		} );
	}

	/**
	 * Verify that alternate keys pointing to lazily registered functions
	 * resolve correctly, triggering lazy promotion of the target.
	 */
	@Test
	void alternateKeysForLazyFunctionsResolve(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			SessionFactoryImplementor sfi = session.getSessionFactory();
			SqmFunctionRegistry registry = sfi.getQueryEngine().getSqmFunctionRegistry();

			// "every" is an alternate key for "bool_and" (or equivalent)
			// "any" is an alternate key for "bool_or" (or equivalent)
			// These may or may not be lazily registered depending on the dialect,
			// but if they exist they must resolve.
			String[][] alternateKeys = {
					{ "every", "any" },
					{ "truncate" },
			};

			for ( String[] group : alternateKeys ) {
				for ( String name : group ) {
					if ( registry.getValidFunctionKeys().contains( name ) ) {
						var descriptor = registry.findFunctionDescriptor( name );
						assertNotNull( descriptor,
								"Alternate key '" + name + "' should resolve to a descriptor" );
					}
				}
			}
		} );
	}

	/**
	 * Verify that repeated lookups of the same lazy function return the same
	 * descriptor instance (promotion is stable).
	 */
	@Test
	void lazyFunctionPromotionIsStable(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			SessionFactoryImplementor sfi = session.getSessionFactory();
			SqmFunctionRegistry registry = sfi.getQueryEngine().getSqmFunctionRegistry();

			// Pick a function likely to be lazily registered on most dialects
			String functionName = "coalesce";
			var first = registry.findFunctionDescriptor( functionName );
			assertNotNull( first, functionName + " should be available" );

			var second = registry.findFunctionDescriptor( functionName );
			assertTrue( first == second,
					"Repeated lookup of '" + functionName + "' should return the same instance" );
		} );
	}

	@Entity(name = "TestEntity")
	@Table(name = "lazy_fn_test_entity")
	public static class TestEntity {
		@Id
		private Long id;
		private String name;

		public TestEntity() {
		}

		public TestEntity(Long id, String name) {
			this.id = id;
			this.name = name;
		}
	}
}
