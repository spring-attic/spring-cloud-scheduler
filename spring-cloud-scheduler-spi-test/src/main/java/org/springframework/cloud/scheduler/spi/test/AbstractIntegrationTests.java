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

package org.springframework.cloud.scheduler.spi.test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.deployer.resource.maven.MavenProperties;
import org.springframework.cloud.deployer.resource.maven.MavenResource;
import org.springframework.cloud.deployer.spi.core.AppDefinition;
import org.springframework.cloud.scheduler.spi.core.CreateScheduleException;
import org.springframework.cloud.scheduler.spi.core.ScheduleInfo;
import org.springframework.cloud.scheduler.spi.core.Scheduler;
import org.springframework.cloud.scheduler.spi.core.ScheduleRequest;
import org.springframework.cloud.scheduler.spi.core.SchedulerException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.NONE;
import static org.springframework.cloud.scheduler.spi.test.EventuallyMatcher.eventually;

/**
 * Contains base set of tests that are required for each implementation of
 * Spring Cloud Scheduler to pass.
 *
 * @author Glenn Renfro
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = NONE)
@ContextConfiguration(classes = AbstractIntegrationTests.Config.class)
public abstract class AbstractIntegrationTests {

	@Autowired
	protected MavenProperties mavenProperties;

	private SchedulerWrapper schedulerWrapper;

	protected final Logger log = LoggerFactory.getLogger(this.getClass());

	@Rule
	public TestName name = new TestName();

	@After
	public void tearDown() {
		List<ScheduleRequest> scheduleRequests = new ArrayList<>(schedulerWrapper.getScheduledTasks().values());

		for (ScheduleRequest scheduleRequest : scheduleRequests) {
			unschedule(scheduleRequest.getScheduleName());
		}

	}

	/**
	 * Subclasses should call this method to interact with the Scheduler under test.
	 * Returns a wrapper around the scheduler returned by {@link #provideScheduler()}, that keeps
	 * track of which tasks have been scheduled and unscheduled.
	 */
	protected Scheduler taskScheduler() {
		return this.schedulerWrapper;
	}

	/**
	 * To be implemented by subclasses, which should return the instance of Scheduler that needs
	 * to be tested. Can be used if subclasses decide to add additional implementation-specific tests.
	 */
	protected abstract Scheduler provideScheduler();

	/**
	 * To be implemented by subclasses, which should return the commandLineArgs that
	 * will be used for the tests.
	 */
	protected abstract List<String> testCommandLineArgs();

	/**
	 * To be implemented by subclasses, which should return the schedulerProperties that
	 * will be used for the tests.
	 */
	protected abstract Map<String, String> testSchedulerProperties();

	/**
	 * To be implemented by subclasses, which should return the deploymentProperties that
	 * will be used for the tests.
	 */
	protected abstract Map<String, String> testDeploymentProperties();

	/**
	 * To be implemented by subclasses, which should return the appProperties that
	 * will be used for the tests.
	 */
	protected abstract Map<String, String> testAppProperties();

	@Before
	public void wrapScheduler() {
		this.schedulerWrapper = new SchedulerWrapper(provideScheduler());
	}

	@Test
	public void testSimpleSchedule() {
		createAndVerifySchedule();
	}

	@Test
	public void testUnschedule() {
		ScheduleInfo scheduleInfo = createAndVerifySchedule();
		unschedule(scheduleInfo.getScheduleName());
	}

	@Test
	public void testDuplicateSchedule() {
		boolean exceptionFired = false;
		ScheduleRequest request = createScheduleRequest();
		schedule(request, request.getDefinition());
		ScheduleInfo scheduleInfo = new ScheduleInfo();
		scheduleInfo.setScheduleName(request.getScheduleName());

		verifySchedule(scheduleInfo);
		try {
			schedule(request, request.getDefinition());
		}
		catch (CreateScheduleException cse) {
			assertEquals("Invalid exception message", String.format("Failed to create schedule %s", request.getScheduleName()), cse.getMessage());
			exceptionFired = true;
		}
		assertTrue(exceptionFired);
	}

	@Test
	public void testUnScheduleNoEntry() {
		boolean exceptionFired = false;
		String definitionName = randomName();
		String scheduleName = "ScheduleName_" + definitionName;

		try {
			unschedule(scheduleName);
		}catch (SchedulerException cse) {
			assertEquals("Invalid exception message",
					String.format("Failed to unschedule, schedule %s does not exist.",
							scheduleName), cse.getMessage());
			exceptionFired = true;
		}
		assertTrue(exceptionFired);
	}

	@Test
	public void testMultipleSchedule() {
		String definitionName = randomName();
		String scheduleName = "Schedule_Name " + definitionName;
		for (int i = 0; i < 4; i++) {
			ScheduleRequest request = createScheduleRequest(scheduleName + i, definitionName + i);
			schedule(request, request.getDefinition());
		}
		List<ScheduleInfo> scheduleInfos = taskScheduler().list();

		for (ScheduleInfo scheduleInfo : scheduleInfos) {
			verifySchedule(scheduleInfo);
		}
	}

	@Test
	public void testListFilter() {
		String definitionName = randomName();
		String scheduleName = "Schedule_Name " + definitionName;
		for (int i = 0; i < 4; i++) {
			ScheduleRequest request = createScheduleRequest(scheduleName + i, definitionName + i%2);
			schedule(request, request.getDefinition());
		}
		Timeout timeout = scheduleTimeout();
		ScheduleInfo scheduleInfo = new ScheduleInfo();
		scheduleInfo.setScheduleName(scheduleName+0);
		scheduleInfo.setTaskDefinitionName(definitionName+0);
		assertThat(scheduleInfo, eventually(hasSpecifiedSchedulesByTaskDefinitionName(taskScheduler().list(definitionName+0), scheduleInfo.getTaskDefinitionName(), 2), timeout.maxAttempts, timeout.pause));
	}

	private ScheduleInfo createAndVerifySchedule() {
		ScheduleRequest request = createScheduleRequest();
		schedule(request, request.getDefinition());
		ScheduleInfo scheduleInfo = new ScheduleInfo();
		scheduleInfo.setScheduleName(request.getScheduleName());
		verifySchedule(scheduleInfo);
		return scheduleInfo;
	}

	private ScheduleRequest createScheduleRequest() {
		String definitionName = randomName();
		String scheduleName = "ScheduleName_" + definitionName;
		return createScheduleRequest(scheduleName, definitionName);
	}

	private ScheduleRequest createScheduleRequest(String scheduleName, String definitionName) {
		AppDefinition definition = new AppDefinition(definitionName, testAppProperties());
		return new ScheduleRequest(definition, testSchedulerProperties(), testDeploymentProperties(), testCommandLineArgs(), scheduleName, testApplication());
	}

	private void verifySchedule(ScheduleInfo scheduleInfo) {
		Timeout timeout = scheduleTimeout();
		assertThat(scheduleInfo, eventually(hasSpecifiedSchedule(taskScheduler().list(), scheduleInfo.getScheduleName()), timeout.maxAttempts, timeout.pause));
	}

	private  void schedule(ScheduleRequest request, AppDefinition appDefinition) {
		log.info("Scheduling {}...", request.getScheduleName());
		taskScheduler().schedule(request);

	}

	private void unschedule(String scheduleName) {
		log.info("unscheduling {}...", scheduleName);

		Timeout timeout = unscheduleTimeout();
		taskScheduler().unschedule(scheduleName);

		ScheduleInfo scheduleInfo = new ScheduleInfo();
		scheduleInfo.setScheduleName(scheduleName);
		assertThat(scheduleInfo, eventually(specifiedScheduleNotPresent(taskScheduler().list(), scheduleName), timeout.maxAttempts, timeout.pause));

	}

	protected String randomName() {
		return name.getMethodName() + "-" + UUID.randomUUID().toString();
	}

	/**
	 * Return the timeout to use for repeatedly querying that a task has been scheduled.
	 * Default value is one minute, being queried every 5 seconds.
	 */
	protected Timeout scheduleTimeout() {
		return new Timeout(12, 5000);
	}

	/**
	 * Return the timeout to use for repeatedly querying whether a task has been unscheduled.
	 * Default value is one minute, being queried every 5 seconds.
	 */
	protected Timeout unscheduleTimeout() {
		return new Timeout(12, 5000);
	}

	/**
	 * Return the time to wait between reusing scheduler requests. This could be necessary to give
	 * some platforms time to clean up after unscheduling.
	 */
	protected int redeploymentPause() {
		return 0;
	}


	/**
	 * A Hamcrest Matcher that queries the schedule list for a schedule name.
	 *
	 * @author Glenn Renfro
	 */
	protected Matcher<ScheduleInfo> hasSpecifiedSchedule(final List<ScheduleInfo> schedules, String scheduleName) {
		return new BaseMatcher<ScheduleInfo>() {

			@Override
			public boolean matches(Object item) {
				boolean result = false;
				for (ScheduleInfo scheduleInfo : schedules) {
					if (scheduleInfo.getScheduleName().equals(scheduleName)) {
						result = true;
						break;
					}
				}
				return result;
			}

			@Override
			public void describeMismatch(Object item, Description mismatchDescription) {
				mismatchDescription.appendText("unable to find specified scheduleName ").appendValue(item).appendText(" ");
			}


			@Override
			public void describeTo(Description description) {
				description.appendText("unable to find specified scheduleName ");
			}
		};
	}

	/**
	 * A Hamcrest Matcher that queries the schedule list for a task definition name.
	 *
	 * @author Glenn Renfro
	 */
	protected Matcher<ScheduleInfo> hasSpecifiedSchedulesByTaskDefinitionName(final List<ScheduleInfo> schedules, String taskDefinitionName, int expectedScheduleCount) {
		return new BaseMatcher<ScheduleInfo>() {

			@Override
			public boolean matches(Object item) {
				boolean result = false;
				if(schedules.size() == expectedScheduleCount) {
					for (ScheduleInfo scheduleInfo : schedules) {
						if (scheduleInfo.getTaskDefinitionName().equals(taskDefinitionName)) {
							result = true;
							break;
						}
					}
				}
				return result;
			}

			@Override
			public void describeMismatch(Object item, Description mismatchDescription) {
				mismatchDescription.appendText("unable to find specified taskDefinitionName ").appendValue(item).appendText(" ");
			}


			@Override
			public void describeTo(Description description) {
				description.appendText("unable to find specified taskDefinitionName ");
			}
		};
	}

	/**
	 * A Hamcrest Matcher that queries the schedule list for a definition name.
	 *
	 * @author Glenn Renfro
	 */
	protected Matcher<ScheduleInfo> specifiedScheduleNotPresent(final List<ScheduleInfo> schedules, String scheduleName) {
		return new BaseMatcher<ScheduleInfo>() {

			@Override
			public boolean matches(Object item) {
				boolean result = true;
				for (ScheduleInfo schedule : schedules) {
					if (schedule.getScheduleName().equals(scheduleName)) {
						result = false;
						break;
					}
				}
				return result;
			}

			@Override
			public void describeMismatch(Object item, Description mismatchDescription) {
				mismatchDescription.appendText("unable to find specified scheduleName ").appendValue(item).appendText(" ");
			}


			@Override
			public void describeTo(Description description) {
				description.appendText("unable to find specified scheduleName ");
			}
		};
	}

	/**
	 * Return a resource corresponding to the spring-cloud-deployer-spi-test-app app suitable for the target runtime.
	 *
	 * The default implementation returns an uber-jar fetched via Maven. Subclasses may override.
	 */
	protected Resource testApplication() {
		Properties properties = new Properties();
		try {
			properties.load(new ClassPathResource("integration-test-app.properties").getInputStream());
		}
		catch (IOException e) {
			throw new RuntimeException("Failed to determine which version of spring-cloud-scheduler-spi-test-app to use", e);
		}
		return new MavenResource.Builder(mavenProperties)
				.groupId("org.springframework.cloud")
				.artifactId("spring-cloud-scheduler-spi-test-app")
				.classifier("exec")
				.version(properties.getProperty("version"))
				.extension("jar")
				.build();
	}

	/**
	 * A decorator for Scheduler that keeps track of scheduled/unscheduled tasks.
	 *
	 * @author Glenn Renfro
	 */
	protected static class SchedulerWrapper implements Scheduler {
		private final Scheduler wrapped;

		private final Map<String,ScheduleRequest> scheduledTasks = new HashMap<>();


		public SchedulerWrapper(Scheduler wrapped) {
			this.wrapped = wrapped;
		}

		@Override
		public void schedule(ScheduleRequest scheduleRequest) {
			wrapped.schedule(scheduleRequest);
			scheduledTasks.put(scheduleRequest.getScheduleName(), scheduleRequest);
		}

		@Override
		public void unschedule(String scheduleName) {
			wrapped.unschedule(scheduleName);
			scheduledTasks.remove(scheduleName);
		}

		@Override
		public List<ScheduleInfo> list(String taskDefinitionName) {
			return wrapped.list(taskDefinitionName);
		}

		@Override
		public List<ScheduleInfo> list() {
			return wrapped.list();
		}

		public Map<String, ScheduleRequest> getScheduledTasks() {
			return  Collections.unmodifiableMap(scheduledTasks);
		}
	}

	@Configuration
	public static class Config {
		@Bean
		@ConfigurationProperties("maven")
		public MavenProperties mavenProperties() {
			return new MavenProperties();
		}
	}
}
