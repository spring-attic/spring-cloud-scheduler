/*
 * Copyright 2018 the original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.springframework.cloud.scheduler.spi.implementation;

import org.junit.Test;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import org.springframework.cloud.scheduler.spi.junit.AbstractExternalResourceTestSupport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Verifies that the {@link AbstractExternalResourceTestSupport} provides the correct
 * messaging upon failure and succeeds where expected upon evaluation.
 *
 * @author Glenn Renfro
 */
public class ExternalResourceTestSupportTests {

	@Test
	public void testSkip() throws Throwable {
		boolean exceptionFired = false;
		AbstractExternalResourceTestSupport testSupport = new FailExternalResourceTestSupport();
		try {
			Statement statement = testSupport.apply(mock(Statement.class), mock(Description.class));
			statement.evaluate();
		}
		catch (org.junit.AssumptionViolatedException exception) {
			exceptionFired = true;
		}
		assertThat(exceptionFired).isTrue();
	}

	@Test
	public void testNoSkip() throws Throwable {
		boolean exceptionFired = false;
		System.setProperty("EXTERNAL_SERVERS_REQUIRED","true");
		AbstractExternalResourceTestSupport testSupport = new FailExternalResourceTestSupport();
		try {
			Statement statement = testSupport.apply(mock(Statement.class), mock(Description.class));
			statement.evaluate();
		}
		catch (AssertionError error) {
				assertThat(error.getMessage()).isEqualTo("TestFail IS NOT AVAILABLE");
				exceptionFired = true;
		}
		assertThat(exceptionFired).isTrue();

	}

	@Test
	public void testSuccess() throws Throwable {
		AbstractExternalResourceTestSupport result = new AbstractExternalResourceTestSupport<String>("value") {

			@Override
			protected void cleanupResource() throws Exception {
			}

			@Override
			protected void obtainResource() throws Exception {
			}
		};
		Statement statement = result.apply(mock(Statement.class), mock(Description.class));
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
