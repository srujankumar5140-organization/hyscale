/**
 * Copyright 2019 Pramati Prism, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.hyscale.controller.commands;

import java.util.concurrent.Callable;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.Pattern;

import io.hyscale.controller.activity.ControllerActivity;
import io.hyscale.controller.constants.WorkflowConstants;
import io.hyscale.controller.model.WorkflowContext;
import io.hyscale.controller.util.CommandUtil;
import io.hyscale.controller.util.LoggerUtility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.hyscale.commons.constants.ToolConstants;
import io.hyscale.commons.constants.ValidationConstants;
import io.hyscale.commons.logger.WorkflowLogger;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

/**
 *  This class executes the 'hyscale get service logs' command
 *  It is a sub-command of the 'hyscale get service' command
 *  @see HyscaleGetServiceCommand .
 *  Every command/sub-command has to implement the {@link Callable} so that
 *  whenever the command is executed the {@link #call()}
 *  method will be invoked
 *
 * @option serviceName name of the service
 * @option namespace  namespace in which the app is deployed
 * @option appName   name of the app
 * @option tail  enable this option to tail the logs
 * @option line  last 'n' number of lines are retrieved from the service
 *
 * Eg: hyscale get service logs -s s1 -n dev -a sample
 *
 * Fetches the pod logs from the given cluster
 *
 */

@Command(name = "logs", aliases = { "log" }, description = "Displays the service logs")
@Component
public class HyscaleServiceLogsCommand implements Callable<Integer> {

	private static final Logger logger = LoggerFactory.getLogger(HyscaleServiceLogsCommand.class);

	@Option(names = { "-h", "--help" }, usageHelp = true, description = "Displays help information for the specified command")
	private boolean helpRequested = false;

	@Pattern(regexp = ValidationConstants.NAMESPACE_REGEX, message = ValidationConstants.INVALID_NAMESPACE_MSG)
	@Option(names = { "-n", "--namespace", "-ns" }, required = true, description = "Namespace of the service")
	private String namespace;

	@Pattern(regexp = ValidationConstants.APP_NAME_REGEX, message = ValidationConstants.INVALID_APP_NAME_MSG)
	@Option(names = { "-a", "--app" }, required = true, description = "Application name")
	private String appName;

	@Pattern(regexp = ValidationConstants.SERVICE_NAME_REGEX, message = ValidationConstants.INVALID_SERVICE_NAME_MSG)
	@Option(names = { "-s", "--service" }, required = true, description = "Service name")
	private String serviceName;
	
	@Option(names = { "-r", "--replica" }, required = false, description = "Replica name")
    private String replicaName;

	@Option(names = { "-t", "--tail" }, required = false, description = "Tail output of the service logs")
	private boolean tail = false;

	@Min(value = ValidationConstants.MIN_LOG_LINES, message = ValidationConstants.MIN_LOG_LINES_ERROR_MSG)
	@Option(names = { "-l", "--line" }, required = false, description = "Number of lines of logs")
	private Integer line = 100;

	@Autowired
	private LoggerUtility loggerUtility;

	@Override
	public Integer call() throws Exception {
		if (!CommandUtil.isInputValid(this)) {
			return ToolConstants.INVALID_INPUT_ERROR_CODE;
		}
		
		WorkflowContext workflowContext = new WorkflowContext();
		WorkflowLogger.header(ControllerActivity.SERVICE_NAME, serviceName);
		workflowContext.setAppName(appName.trim());
		workflowContext.setNamespace(namespace.trim());
		workflowContext.setServiceName(serviceName);
		workflowContext.addAttribute(WorkflowConstants.TAIL_LOGS, tail);
		workflowContext.addAttribute(WorkflowConstants.LINES, line);
		workflowContext.addAttribute(WorkflowConstants.REPLICA_NAME, replicaName);

		loggerUtility.getLogs(workflowContext);

		return workflowContext.isFailed() ? ToolConstants.HYSCALE_ERROR_CODE : 0;
	}

}
