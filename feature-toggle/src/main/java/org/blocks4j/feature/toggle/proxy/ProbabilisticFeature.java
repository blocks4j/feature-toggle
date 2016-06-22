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

package org.blocks4j.feature.toggle.proxy;

import java.lang.reflect.Method;
import java.util.Random;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ProbabilisticFeature<T> extends Feature<T> {

    private static final Pattern PROBABILISTIC_VALUE_PATTERN = Pattern.compile("^(\\d+)/(\\d+)$");

    private static final String PROBABILISTIC_VALUE_PROPERTY_FORMAT = "%s#probability";

    private Random random;

    public ProbabilisticFeature() {
        this.random = new Random();
    }

    @Override
    protected boolean isOn(Method method, Object[] args) {
        return super.isOn(method, args) && this.probabilisticAssertion();
    }

    private boolean probabilisticAssertion() {
        boolean isOn = false;

        Set<String> probValue = this.getConfig().getEnabledParameters().get(String.format(PROBABILISTIC_VALUE_PROPERTY_FORMAT, this.getFeatureName()));

        if (probValue.size() == 1) {
            Matcher matcher = PROBABILISTIC_VALUE_PATTERN.matcher(probValue.iterator().next());
            if (matcher.find()) {
                int accept = Integer.valueOf(matcher.group(1));
                int total = Integer.valueOf(matcher.group(2));

                isOn = this.random.nextInt(total) < accept;
            }
        }

        return isOn;
    }
}