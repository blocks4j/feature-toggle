/*
 *   Copyright 2013-2016 Blocks4J Team (www.blocks4j.org)
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package org.blocks4j.feature.toggle.test.domain;

import org.blocks4j.feature.toggle.annotation.parameters.ParameterToggle;

public class OperationParameter {

    @ParameterToggle("paramTestField")
    private int parameterField;

    private Long parameterMethod;

    public OperationParameter(int parameterField, Long parameterMethod) {
        this.parameterField = parameterField;
        this.parameterMethod = parameterMethod;
    }

    @ParameterToggle("paramTestMethod")
    public Long getParameter() {
        return this.parameterMethod;
    }

}
