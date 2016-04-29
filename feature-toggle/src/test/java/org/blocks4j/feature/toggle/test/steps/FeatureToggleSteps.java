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

package org.blocks4j.feature.toggle.test.steps;

import cucumber.api.java.Before;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;
import org.apache.commons.lang3.StringUtils;
import org.blocks4j.feature.toggle.FeatureToggleConfiguration;
import org.blocks4j.feature.toggle.factory.FeatureToggleFactory;
import org.blocks4j.feature.toggle.test.config.TestingFeatureToggleConfiguration;
import org.blocks4j.feature.toggle.test.domain.OperationParameter;
import org.blocks4j.feature.toggle.test.domain.ParameterEnum;
import org.blocks4j.feature.toggle.test.domain.TestingFeature;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class FeatureToggleSteps {

    private Map<String, TestingFeature> features;

    private TestingFeature featureImplementation;

    private FeatureToggleConfiguration featureConfig;


    @Before
    public void beforeTest() {
        this.features = new HashMap<String, TestingFeature>();
        this.featureConfig = new TestingFeatureToggleConfiguration();
    }


    @Given("^a implementation of this interface called \"([^\"]*)\" with parameter toggle called 'paramTest'$")
    public void aImplementationOfThisInterfaceCalledWithParameterToggleCalledParamTest(String implementationName) throws Throwable {
        this.features.put(implementationName, Mockito.mock(TestingFeature.class));
    }

    @Given("^the feature toggle called \"([^\"]*)\" switching between:$")
    public void theFeatureToggleCalledSwitchingBetween(String featureName, Map<String, String> featureOption) throws Throwable {
        this.featureImplementation = FeatureToggleFactory.forFeature(this.featureConfig,
                                                                     featureName,
                                                                     TestingFeature.class)
                                                         .whenEnabled(this.features.get(featureOption.get("newFeature")))
                                                         .whenDisabled(this.features.get(featureOption.get("originalFeature")))
                                                         .build();
    }

    @Given("^the Switchable feature toggle for alternative of the implementation called \"([^\"]*)\" and this cases:$")
    public void theSwitchableFeatureToggleForAlternativeOfTheImplementationCalledAndThisCases(String defaultImplementationName, List<Map<String, String>> implementations) throws Throwable {
        final FeatureToggleFactory.SwitchableFeatureBuilder<TestingFeature> testingFeatureSwitchableFeatureBuilder =
                FeatureToggleFactory.forSwitchableFeaturesConfiguration(this.featureConfig,
                                                                        TestingFeature.class)
                                    .defaultFeature(this.features.get(defaultImplementationName));

        for (Map<String, String> feature : implementations) {
            testingFeatureSwitchableFeatureBuilder.when(feature.get("featureName"), this.features.get(feature.get("implementationName")));
        }

        this.featureImplementation = testingFeatureSwitchableFeatureBuilder.build();

    }

    @When("^those features are in the featureNames at configuration \"([^\"]*)\"$")
    public void thoseFeaturesAreInTheFeatureNamesAtConfiguration(String features) throws Throwable {
        String[] featuresSplit = features.split(",", -1);

        this.featureConfig.getEnabledFeatures().addAll(Arrays.asList(featuresSplit));
    }

    @When("^those features activates with those parameters \"([^\"]*)\"$")
    public void thoseFeaturesActivatesWithThoseParameters(String rawParameters) throws Throwable {
        String[] split = rawParameters.split(";", -1);

        Map<String, Set<String>> parameterMap = new HashMap<String, Set<String>>();

        for (String parameterAndValues : split) {
            if (StringUtils.isNotBlank(parameterAndValues)) {
                String[] parameterAndValuesSplit = parameterAndValues.split("=", -1);

                if (parameterAndValuesSplit.length > 0) {
                    String parameterName = parameterAndValuesSplit[0];
                    Set<String> parameterValues = new HashSet<String>();
                    if (parameterAndValuesSplit.length == 2 && StringUtils.isNotBlank(parameterAndValuesSplit[1])) {
                        String parameterValuesString = parameterAndValuesSplit[1];

                        for (String parameterValue : parameterValuesString.split(",", -1)) {
                            if (StringUtils.isNotBlank(parameterValue)) {
                                parameterValues.add(parameterValue);
                            }
                        }
                    }

                    parameterMap.put(parameterName, parameterValues);
                }
            }
        }

        this.featureConfig.getEnabledParameters().putAll(parameterMap);

    }

    @When("^the service is called with complex parameter \"([^\"]*)\" and \"([^\"]*)\"$")
    public void theServiceIsCalledWithClassParameter(int parameterField, ParameterEnum parameterMethod) throws Throwable {
        this.featureImplementation.operation(new OperationParameter(parameterField, parameterMethod));
    }

    @When("^the service is called with primitive parameter \"([^\"]*)\"$")
    public void theServiceIsCalledWithParameter(String parameter) throws Throwable {
        this.featureImplementation.operation(parameter);
    }

    @Then("^the implementation of operation\\(OperationParameter\\) \"([^\"]*)\" will be used$")
    public void theImplementationWillBeUsed(final String usedImplementationName) throws Throwable {
        for (Map.Entry<String, TestingFeature> testingFeatureEntry : this.features.entrySet()) {
            String implementationName = testingFeatureEntry.getKey();
            TestingFeature implementation = testingFeatureEntry.getValue();

            if (usedImplementationName.equalsIgnoreCase(implementationName)) {
                Mockito.verify(implementation, Mockito.only()).operation(Mockito.<OperationParameter>any());
                Mockito.verify(implementation, Mockito.times(1)).operation(Mockito.<OperationParameter>any());
            } else {
                Mockito.verify(implementation, Mockito.never()).operation(Mockito.<OperationParameter>any());
            }
        }

    }

    @Then("^the implementation of operation\\(String\\) \"([^\"]*)\" will be used$")
    public void theImplementationStringWillBeUsed(final String usedImplementationName) throws Throwable {
        for (Map.Entry<String, TestingFeature> testingFeatureEntry : this.features.entrySet()) {
            String implementationName = testingFeatureEntry.getKey();
            TestingFeature implementation = testingFeatureEntry.getValue();

            if (usedImplementationName.equalsIgnoreCase(implementationName)) {
                Mockito.verify(implementation, Mockito.only()).operation(Mockito.anyString());
                Mockito.verify(implementation, Mockito.times(1)).operation(Mockito.anyString());
            } else {
                Mockito.verify(implementation, Mockito.never()).operation(Mockito.anyString());
            }
        }
    }


}
