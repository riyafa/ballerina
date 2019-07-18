// Copyright (c) 2018 WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
//
// WSO2 Inc. licenses this file to you under the Apache License,
// Version 2.0 (the "License"); you may not use this file except
// in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

# The Caller actions related to queue receiver endpoint.
#
# + consumer - the QueueListener
public type  Consumer client object {
    private Session session;
    private ConsumerConfiguration config;

    public function __init(public Session session, public ConsumerConfiguration config = ()) returns error? {
        self.session = session;
        self.createConsumer(session);
        if(config is ConsumerConfiguration) {
            self.config = config;
        } else {
           self.config = {};
        }
        self.config.freeze();
        if(config.destination != ()) {
            return self.init();
        }
    }

    private function init() returns error? = external;

    # Synchronously receives a message from the JMS provider.
    #
    # + timeoutInMilliSeconds - Time to wait until a message is received.
    # + return - Returns a message or nil if the timeout exceeds, or returns an error upon an internal error of the JMS
    #             provider.
    public remote function receive(int timeoutInMilliSeconds = 0) returns Message|error? = external;

    # Synchronously receives a message from a given destination.
    #
    # + destination - Destination to subscribe to.
    # + timeoutInMilliSeconds - Time to wait until a message is received.
    # + return - Returns a message or () if the timeout exceeds, or returns an error upon an internal error of the JMS
    #             provider.
    public remote function receiveFrom(Destination destination, int timeoutInMilliSeconds = 0) returns (Message|error)?
    {
        if (self.config["destination"] is ()) {
            Error err = error(JMS_ERROR_CODE, message = "Cannot receive from a different destination if the " + 
                "destination is already set for this Consumer");
            return err;
        }
        check validate(destination);
        any anyValue = self.config;
        var config = anyValue.clone();
        if (config is ConsumerConfiguration) {
            config["destination"] = destination;
            var consumer = new Consumer(self.session, config);
            if (consumer is Consumer) {
                var result = self->receive(timeoutInMilliSeconds = timeoutInMilliSeconds);
                var err = consumer.close(self); // Ignore error here
                return result;
            } else {
                return consumer;
            }
        } else {
            Error err = error(JMS_ERROR_CODE, message = "Error cloning the configuration");
            return err;
        }
    }

    public function getConfig() returns ConsumerConfiguration {
        return self.config;
    }

    public remote function close() returns error? = external;
};

public type ConsumerConfiguration record {|
    Destination destination?;
    string  messageSelector?;
    boolean noLocal = false;
    string durableId?;
    string sharedSubscriptionName?;
|};