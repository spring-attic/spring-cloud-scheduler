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

import org.junit.Before;
import org.junit.Test;

import org.springframework.cloud.scheduler.spi.core.Scheduler;

public class ScheduleStubTests {

	private Scheduler scheduler;
	@Before
	public void setup() {
		this.scheduler = new SchedulerStub();
	}

	@Test(expected = UnsupportedOperationException.class)
	public void testSchedule() {
		this.scheduler.schedule(null);
	}

	@Test(expected = UnsupportedOperationException.class)
	public void testUnschedule() {
		this.scheduler.unschedule(null);
	}

	@Test(expected = UnsupportedOperationException.class)
	public void testList() {
		this.scheduler.list();
	}

	@Test(expected = UnsupportedOperationException.class)
	public void testFilteredList() {
		this.scheduler.list(null);
	}
}
