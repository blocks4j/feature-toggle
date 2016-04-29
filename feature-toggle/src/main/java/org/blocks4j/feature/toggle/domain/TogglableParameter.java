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

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class TogglableParameter<T> {
    public enum AccessMethod {DIRECT, FIELD, METHOD}

    private int index;
    private String id;
    private T accessibleObject;
    private AccessMethod accessMethod;

    private TogglableParameter(int index, String id, T accessibleObject, AccessMethod accessMethod) {
        this.index = index;
        this.id = id;
        this.accessibleObject = accessibleObject;
        this.accessMethod = accessMethod;
    }

    public static TogglableParameter<?> createTogglableParameter(int index, String id) {
        return new TogglableParameter<Object>(index, id, null, AccessMethod.DIRECT);
    }

    public static TogglableParameter<Field> createTogglableParameter(int index, String id, Field field) {
        return new TogglableParameter<Field>(index, id, field, AccessMethod.FIELD);
    }

    public static TogglableParameter<Method> createTogglableParameter(int index, String id, Method method) {
        return new TogglableParameter<Method>(index, id, method, AccessMethod.METHOD);
    }

    public int getIndex() {
        return this.index;
    }

    public String getId() {
        return this.id;
    }

    public T getAccessibleObject() {
        return this.accessibleObject;
    }

    public AccessMethod getAccessMethod() {
        return this.accessMethod;
    }
}