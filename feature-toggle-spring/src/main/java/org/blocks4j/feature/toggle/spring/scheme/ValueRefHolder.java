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

package org.blocks4j.feature.toggle.spring.scheme;


public class ValueRefHolder {
    private String held;
    private HoldingType type;

    public ValueRefHolder(HoldingType type, String held) {
        this.type = type;
        this.held = held;
    }

    public boolean hasRef() {
        return this.type == HoldingType.REF;
    }

    public boolean hasValue() {
        return this.type == HoldingType.VALUE;
    }

    public String getValue() {
        return (this.type == HoldingType.REF) ? null : this.held;
    }

    public String getRef() {
        return (this.type == HoldingType.REF) ? this.held : null;
    }

    enum HoldingType {
        VALUE, REF
    }

    @Override
    public String toString() {
        return String.format("[hasValue: '%s', hasRef: '%s', value: '%s', ref: '%s']", this.hasValue(), this.hasRef(), this.getValue(), this.getRef());
    }
}