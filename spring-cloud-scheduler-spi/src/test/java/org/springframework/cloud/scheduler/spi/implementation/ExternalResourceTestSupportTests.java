/*
 * Copyright 2018 the original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *          https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.springframework.cloud.scheduler.spi.implementation;

import org.junit.AssumptionViolatedException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import org.springframework.cloud.scheduler.spi.junit.AbstractExternalResourceTestSupport;

import static org.mockito.Mockito.mock;

/**
 * Verifies that the {@link AbstractExternalResourceTestSupport} provides the correct
 * messaging upon failure and succeeds where expected upon evaluation.
 *
 * @author Glenn Renfro
 */
public class ExternalResourceTestSupportTests {

	@Rule
	public ExpectedException expectedException = ExpectedException.none();

	@Test
	public void testSkip() throws Throwable {
		this.expectedException.expect(AssumptionViolatedException.class);

		AbstractExternalResourceTestSupport testSupport = new FailExternalResourceTestSupport();
			Statement statement = testSupport.apply(mock(Statement.class), mock(Description.class));
			statement.evaluate();
	}

	@Test
	public void testNoSkip() throws Throwable {
		System.setProperty("EXTERNAL_SERVERS_REQUIRED","true");
		this.expectedException.expect(AssertionError.class);
		this.expectedException.expectMessage("TestFail IS NOT AVAILABLE");

		AbstractExternalResourceTestSupport testSupport = new FailExternalResourceTestSupport();
		Statement statement = testSupport.apply(mock(Statement.class), mock(Description.class));
		statement.evaluate();
	}


	public static class FailExternalResourceTestSupport extends AbstractExternalResourceTestSupport<String> {

		public FailExternalResourceTestSupport() {
			super("TestFail");
		}
		@Override
		protected void cleanupResource() throws Exception {
		}

		@Override
		protected void obtainResource() throws Exception {
			throw new IllegalStateException("This should fail test");
		}
	}
}
