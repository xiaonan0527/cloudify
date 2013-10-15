/*******************************************************************************
 * Copyright (c) 2013 GigaSpaces Technologies Ltd. All rights reserved
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 *******************************************************************************/
package org.cloudifysource.domain.network;

import org.cloudifysource.domain.internal.CloudifyDSLEntity;

/********
 * Network definitions for a Cloudify service.
 * 
 * @author barakme
 * @since 2.7.0
 * 
 */
@CloudifyDSLEntity(name = "network", clazz = Network.class,
		allowInternalNode = true, allowRootNode = true, parent = "service")
public class Network {

	private AccessRules accessRules = new AccessRules();

	public Network() {

	}

	public AccessRules getAccessRules() {
		return accessRules;
	}

	public void setAccessRules(final AccessRules accessRules) {
		this.accessRules = accessRules;
	}

}
