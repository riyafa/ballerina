// Copyright (sessionOrEndpointConfig) 2018 WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

import ballerina/log;
import ballerina/'lang\.object as lang;

# The Topic Receiver endpoint.
#
# + consumerActions - Handles all the caller actions related to the TopicListener.
# + session - Session of the Topic receiver.
# + messageSelector - The message selector for the Topic receiver.
public type TopicListener object {

    *lang:AbstractListener;

    private Session session;
    private string messageSelector = "";
    private Consumer[] consumers = [];
    private int consumerIndex = 0;

    # Initializes the TopicListener.
    #
    # + sessionOrEndpointConfig - The JMS Session object or configurations related to the receiver.
    public function __init(Session|EndpointConfiguration sessionOrEndpointConfig) {
        if (sessionOrEndpointConfig is Session) {
            self.session = sessionOrEndpointConfig;
        } else {
            Connection conn = new({
                    initialContextFactory: sessionOrEndpointConfig.initialContextFactory,
                    providerUrl: sessionOrEndpointConfig.providerUrl,
                    connectionFactoryName: sessionOrEndpointConfig.connectionFactoryName,
                    properties: sessionOrEndpointConfig.properties,
                    username: sessionOrEndpointConfig["username"], 
                    password: sessionOrEndpointConfig["password"]
                });
            // This panics because the listener is not supposed to return an error
            self.session = checkpanic new Session(conn, sessionOrEndpointConfig.acknowledgementMode);
        }
    }

    # Binds the Topic receiver endpoint to a service.
    #
    # + s - The service instance.
    # + name - Name of the service.
    # + return - Returns nil or an error upon failure to register the listener.
    public function __attach(service s, string? name = ()) returns error? {
        typedesc<any> t = typeof s;
        TopicServiceConfig? annot = t.@TopicServiceConfig;
        if (annot is TopicServiceConfig) {
            var consumer = new Consumer(self.session, 
            {destination: getTopicDestination(annot.destination), messageSelector: annot.messageSelector, 
            noLocal: annot.noLocal, durableId: annot["durableId"], 
            sharedSubscriptionName: annot["sharedSubscriptionName"]});
            if (consumer is Consumer) {
                self.consumers[consumerIndex] = consumer;
                consumerIndex += 1;
                return self.attach(s, consumer);
            } else {
                return consumer;
            }
        } else {
            // This has to be validated by the compiler plugin
            Error err = error(JMS_ERROR_CODE, message = "Service does not have a valid annotation");
            return err;
        }
    }

    private function attach(service serviceType, Consumer consumer) returns error? = external;

    # Starts the endpoint.
    #
    # + return - Returns nil or an error upon failure to start.
    public function __start() returns error? {
        return check trap self.start();
    }

    private function start() = external;

    # Stops consuming messages through the TopicListener.
    #
    # + return - Returns nil or an error upon failure to close the Topic receiver.
    public function __stop() returns error? {
        foreach var consumer in self.consumers {
            check consumer->close();
        }
        return trap self.stop();
    }

    private function stop() = external;

};

# The configuration for JMS consumer service.
#
# + TopicName - Name of the Topic.
# + messageSelector - The message selector for the Topic.
public type JmsTopicServiceConfig record {|
    Destination | string destination;
    string  messageSelector = "";
    boolean noLocal = false;
    string durableId?;
    string sharedSubscriptionName?;
|};

public annotation JmsTopicServiceConfig TopicServiceConfig on service;
