/*
 * -
 * #%L
 * Pipeline: AWS Steps
 * %%
 * Copyright (C) 2017 Taimos GmbH
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

package de.taimos.pipeline.aws.cloudformation.stacksets;

import com.amazonaws.services.cloudformation.model.Parameter;
import com.amazonaws.services.cloudformation.model.StackSetStatus;
import com.amazonaws.services.cloudformation.model.Tag;
import com.amazonaws.services.cloudformation.model.UpdateStackSetResult;
import hudson.Extension;
import hudson.FilePath;
import org.jenkinsci.plugins.workflow.steps.AbstractStepDescriptorImpl;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepExecution;
import org.kohsuke.stapler.DataBoundConstructor;

import javax.annotation.Nonnull;
import java.util.Collection;

public class CFNUpdateStackSetStep extends AbstractCFNCreateStackSetStep {

	@DataBoundConstructor
	public CFNUpdateStackSetStep(String stackSet) {
		super(stackSet);
	}

	@Extension
	public static class DescriptorImpl extends AbstractStepDescriptorImpl {

		public DescriptorImpl() {
			super(Execution.class);
		}

		@Override
		public String getFunctionName() {
			return "cfnUpdateStackSet";
		}

		@Override
		public String getDisplayName() {
			return "Create or Update CloudFormation Stack Set";
		}
	}

	@Override
	public StepExecution start(StepContext context) throws Exception {
		return new CFNUpdateStackSetStep.Execution(this, context);
	}

	public static class Execution extends AbstractCFNCreateStackSetStep.Execution<CFNUpdateStackSetStep> {

		protected Execution(CFNUpdateStackSetStep step, @Nonnull StepContext context) {
			super(step, context);
		}

		@Override
		public void checkPreconditions() {
		}

		@Override
		public String getThreadName() {
			return "cfnUpdateStackSet-" + getStep().getStackSet();
		}

		@Override
		public Object whenStackSetExists(Collection<Parameter> parameters, Collection<Tag> tags) throws Exception {
			final String url = this.getStep().getUrl();
			CloudFormationStackSet cfnStackSet = this.getCfnStackSet();
			UpdateStackSetResult operation = cfnStackSet.update(this.getStep().readTemplate(this), url, parameters, tags);
			cfnStackSet.waitForOperationToComplete(operation.getOperationId(), getStep().getPollInterval());
			return cfnStackSet.describe();
		}


		@Override
		public Object whenStackSetMissing(Collection<Parameter> parameters, Collection<Tag> tags) throws Exception {
			final String url = getStep().getUrl();
			CloudFormationStackSet cfnStack = this.getCfnStackSet();
			cfnStack.create(this.getStep().readTemplate(this), url, parameters, tags);
			return cfnStack.waitForStackState(StackSetStatus.ACTIVE, getStep().getPollInterval());
		}

		private static final long serialVersionUID = 1L;

	}

}
