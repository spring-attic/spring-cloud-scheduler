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

package org.springframework.cloud.scheduler.spi.test;


import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.cloud.scheduler.spi.core.CreateScheduleException;
import org.springframework.cloud.scheduler.spi.core.ScheduleInfo;
import org.springframework.cloud.scheduler.spi.core.Scheduler;
import org.springframework.cloud.scheduler.spi.core.ScheduleRequest;
import org.springframework.cloud.scheduler.spi.core.SchedulerException;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

public class TestInfrastructureTests extends AbstractIntegrationTests{


	@Override
	protected Scheduler provideScheduler() {
		return new TestSchedulerImpl();
	}

	@Override
	protected Resource testApplication() {
		return new FileSystemResource("woof");
	}


	@Override
	protected List<String> getCommandLineArgs() {
		return null;
	}

	@Override
	protected Map<String, String> getSchedulerProperties() {
		return null;
	}

	@Override
	protected Map<String, String> getDeploymentProperties() {
		return null;
	}

	@Override
	protected Map<String, String> getAppProperties() {
		return null;
	}

	public static class TestSchedulerImpl implements Scheduler {

		private List<ScheduleInfo> schedules = new ArrayList<>();


		@Override
		public void schedule(ScheduleRequest scheduleRequest) {
			ScheduleInfo schedule = new ScheduleInfo();
			schedule.setScheduleName(scheduleRequest.getScheduleName());
			schedule.setTaskDefinitionName(scheduleRequest.getDefinition().getName());
			schedules.stream().forEach(s -> {
				if (s.getScheduleName().equals(scheduleRequest.getScheduleName())) {
					throw new CreateScheduleException(scheduleRequest.getScheduleName(),null);
				}
			});
			schedules.add(schedule);
		}

		@Override
		public void unschedule(String scheduleName) {
			boolean isScheduleRemoved = false;
			List<ScheduleInfo> resultSchedules = new ArrayList<>();
			for(ScheduleInfo scheduleCandidate : this.schedules) {
				if(!scheduleCandidate.getScheduleName().equals(scheduleName)) {
					resultSchedules.add(scheduleCandidate);
				} else {
					isScheduleRemoved = true;
				}
			}
			if(!isScheduleRemoved) {
				throw new SchedulerException(String.format(
						"Failed to unschedule, schedule %s does not exist.",
						scheduleName), null);
			}
			this.schedules = resultSchedules;
		}

		@Override
		public List<ScheduleInfo> list(String taskDefinitionName) {
			List<ScheduleInfo> resultSchedules = new ArrayList<>();
			for(ScheduleInfo scheduleInfo : this.schedules) {
				if(scheduleInfo.getTaskDefinitionName().equals(taskDefinitionName)) {
					resultSchedules.add(scheduleInfo);
				}
 			}
			return resultSchedules;
		}

		@Override
		public List<ScheduleInfo> list() {
			return this.schedules;
		}
	}
}
