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

package org.blocks4j.feature.toggle.camel;

import org.apache.camel.AsyncCallback;
import org.apache.camel.AsyncProcessor;
import org.apache.camel.CamelContext;
import org.apache.camel.Endpoint;
import org.apache.camel.Exchange;
import org.apache.camel.ExchangePattern;
import org.apache.camel.Producer;
import org.apache.camel.ServicePoolAware;
import org.apache.camel.Traceable;
import org.apache.camel.impl.ProducerCache;
import org.apache.camel.support.ServiceSupport;
import org.apache.camel.util.AsyncProcessorConverterHelper;
import org.apache.camel.util.AsyncProcessorHelper;
import org.apache.camel.util.EventHelper;
import org.apache.camel.util.ObjectHelper;
import org.apache.camel.util.ServiceHelper;
import org.apache.camel.util.StopWatch;
import org.apache.camel.util.URISupport;
import org.blocks4j.feature.toggle.FeatureToggleConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Processor for forwarding exchanges to an endpoint featureOnDestination.
 */
class ToggledSendProcessor extends ServiceSupport implements AsyncProcessor, Traceable {
    static final Logger LOG = LoggerFactory.getLogger(ToggledSendProcessor.class);
    private final CamelContext camelContext;

    private final FeatureToggleConfiguration config;
    private final String featureName;
    private Endpoint featureOnDestination;
    protected Endpoint featureOffDestination;

    protected ProducerCache producerCache;
    private Map<Endpoint, AsyncProcessor> producers;

    public ToggledSendProcessor(FeatureToggleConfiguration config, String featureName, Endpoint featureOnDestination, Endpoint featureOffDestination) {
        ObjectHelper.notNull(featureOnDestination, "featureOnDestination");
        ObjectHelper.notNull(featureOffDestination, "featureOffDestination");
        ObjectHelper.notNull(config, "config");
        ObjectHelper.notNull(featureName, "featureName");

        this.config = config;
        this.featureName = featureName;

        this.featureOnDestination = featureOnDestination;
        this.featureOffDestination = featureOffDestination;
        this.producers = new HashMap<>();
        this.camelContext = featureOnDestination.getCamelContext();

        ObjectHelper.notNull(this.camelContext, "camelContext");
    }

    @Override
    public String toString() {
        return "sendTo(" + this.featureOnDestination + ")";
    }

    private Endpoint getEnabledFeatureEndpoint() {
        Endpoint enabledFeature = this.featureOffDestination;
        if (this.isFeatureOn()) {
            enabledFeature = this.featureOnDestination;
        }
        return enabledFeature;
    }

    private boolean isFeatureOn() {
        boolean isOn = false;
        if ((this.config.getEnabledFeatures() != null) && !this.config.getEnabledFeatures().isEmpty() && this.config.getEnabledFeatures().contains(this.featureName)) {
            isOn = true;
        }
        return isOn;
    }

    public String getTraceLabel() {
        return URISupport.sanitizeUri(this.featureOnDestination.getEndpointUri());
    }

    public void process(final Exchange exchange) throws Exception {
        AsyncProcessorHelper.process(this, exchange);
    }

    public boolean process(Exchange exchange, final AsyncCallback callback) {
        if (!this.isStarted()) {
            exchange.setException(new IllegalStateException("ToggledSendProcessor has not been started: " + this));
            callback.done(true);
            return true;
        }


        // we should preserve existing MEP so remember old MEP
        // if you want to permanently to change the MEP then use .setExchangePattern in the DSL
        final ExchangePattern existingPattern = exchange.getPattern();
        final Endpoint featureDestination = this.getEnabledFeatureEndpoint();

        AsyncProcessor producer = this.producers.get(featureDestination);

        // if we have a producer then use that as its optimized
        if (producer != null) {

            // record timing for sending the exchange using the producer
            final StopWatch watch = new StopWatch();

            final Exchange target = this.configureExchange(exchange, featureDestination);

            EventHelper.notifyExchangeSending(exchange.getContext(), target, featureDestination);
            LOG.debug(">>>> {} {}", featureDestination, exchange);

            boolean sync = true;
            try {
                sync = producer.process(exchange, doneSync -> {
                    try {
                        // restore previous MEP
                        target.setPattern(existingPattern);
                        // emit event that the exchange was sent to the endpoint
                        long timeTaken = watch.stop();
                        EventHelper.notifyExchangeSent(target.getContext(), target, featureDestination, timeTaken);
                    } finally {
                        callback.done(doneSync);
                    }
                });
            } catch (Throwable throwable) {
                exchange.setException(throwable);
            }

            return sync;
        }

        // send the exchange to the featureOnDestination using the producer cache for the non optimized producers
        return this.producerCache.doInAsyncProducer(featureDestination, exchange, null, callback, (producer1, asyncProducer, exchange1, pattern, callback1) -> {
            final Exchange target = ToggledSendProcessor.this.configureExchange(exchange1, featureDestination);
            LOG.debug(">>>> {} {}", featureDestination, exchange1);
            return asyncProducer.process(target, doneSync -> {
                // restore previous MEP
                target.setPattern(existingPattern);
                // signal we are done
                callback1.done(doneSync);
            });
        });
    }

    public Endpoint getFeatureOnDestination() {
        return this.featureOnDestination;
    }


    protected Exchange configureExchange(Exchange exchange, Endpoint destination) {
        // set property which endpoint we send to
        exchange.setProperty(Exchange.TO_ENDPOINT, destination.getEndpointUri());
        return exchange;
    }

    protected void doStart() throws Exception {
        ServiceHelper.startService(this.producerCache);

        // warm up the producer by starting it so we can fail fast if there was a problem
        // however must start endpoint first
        ServiceHelper.startService(this.featureOnDestination);
        this.cacheEndpointProducer(this.featureOnDestination);

        ServiceHelper.startService(this.featureOffDestination);
        this.cacheEndpointProducer(this.featureOffDestination);
    }

    private void cacheEndpointProducer(Endpoint featureDestination) throws Exception {
        if (this.producerCache == null) {
            // use a single producer cache as we need to only hold reference for one featureOnDestination
            // and use a regular HashMap as we do not want a soft reference store that may get re-claimed when low on memory
            // as we want to ensure the producer is kept around, to ensure its lifecycle is fully managed,
            // eg stopping the producer when we stop etc.
            this.producerCache = new ProducerCache(this, this.camelContext, new HashMap<>(2));
            // do not add as service as we do not want to manage the producer cache
        }

        Producer producer = this.producerCache.acquireProducer(featureDestination);
        if ((producer instanceof ServicePoolAware) || !producer.isSingleton()) {
            // no we cannot optimize it - so release the producer back to the producer cache
            // and use the producer cache for sending
            this.producerCache.releaseProducer(featureDestination, producer);
        } else {
            // yes we can optimize and use the producer directly for sending
            this.producers.put(featureDestination, AsyncProcessorConverterHelper.convert(producer));
        }

    }

    protected void doStop() throws Exception {
        for (AsyncProcessor producer : this.producers.values()) {
            ServiceHelper.stopServices(this.producerCache, producer);
        }
    }

    protected void doShutdown() throws Exception {
        for (AsyncProcessor producer : this.producers.values()) {
            ServiceHelper.stopAndShutdownServices(this.producerCache, producer);
        }
    }
}
