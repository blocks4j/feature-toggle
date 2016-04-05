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

package org.blocks4j.feature.toggle.domain;

import java.lang.reflect.Method;

public class TogglableParameter {

    private Integer paramIndex;
    private String id;
    private Method methodGetField;

    public TogglableParameter(Integer paramIndex, String id, Method methodGetField) {
        this.methodGetField = methodGetField;
        this.paramIndex = paramIndex;
        this.id = id;
    }

    public TogglableParameter(Integer paramIndex, String id) {
        this.paramIndex = paramIndex;
        this.id = id;
    }

    public Integer getIndex() {
        return this.paramIndex;
    }

    public boolean isAnnotatedOnField() {
        return (this.methodGetField == null) ? false : true;
    }

    public void setIndex(Integer index) {
        this.paramIndex = index;
    }

    public String getId() {
        return this.id;
    }

    public void setId(String value) {
        this.id = value;
    }

    public Method getMethod() {
        return this.methodGetField;
    }

    public void setMethod(Method method) {
        this.methodGetField = method;
    }
}