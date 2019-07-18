// Copyright (c) 2019 WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

# Configuration related to simple topic subscriber endpoint
#
# + initialContextFactory - JMS provider specific inital context factory
# + providerUrl - JMS provider specific provider URL used to configure a connection
# + connectionFactoryName - JMS connection factory to be used in creating JMS connections
# + acknowledgementMode - Specifies the `SessionAcknowledgementMode` that will be used.
# + properties - Additional properties used when initializing the initial context
public type EndpointConfiguration record {|
    string initialContextFactory;
    string providerUrl;
    string connectionFactoryName = "ConnectionFactory";
    SessionAcknowledgementMode acknowledgementMode = AUTO_ACKNOWLEDGE;
    string username?;
    string password?;
    map<any> properties = {};
|};

function getMessage(public string | byte[] |
    map<string | byte | int | float | boolean | byte[] |()> | Message payload, CustomHeaders? headers = (), 
    map<string | int | float | boolean | byte | json | xml>? properties = ()) returns Message|error? {
    Message | error msg = new Message(self.session, MESSAGE);
        if (message is string) {
            msg = new Message(self.session, TEXT_MESSAGE);
            check msg.setPayload(payload);
        } else if (message is byte[]) {
            msg = new Message(self.session, BYTES_MESSAGE);
            check msg.setPayload(payload);
        } else if (message is map<string | byte | int | float | boolean | byte[] |()>) {
            msg = new Message(self.session, MAP_MESSAGE);
            check msg.setPayload(payload);
        } else {
            msg = message;
        }
        if (msg is Message) {
            if (headers != ()) {
                check msg.setCustomHeaders(headers);
            } 
            if (properties != ()) {
                foreach var [key,prop] in properties {
                    check msg.setProperty(key, prop);
                }
            }
            return msg;
        }
        return msg;
}

# The two types of delivery modes in JMS.
public type DeliveryMode PERSISTENT | NON_PERSISTENT;

# A persistent message is delivered once-and-only-once which means that if the JMS provider fails,
# the message is not lost; it will be delivered after the server recovers.
public const PERSISTENT = "PERSISTENT";
# A non-persistent message is delivered at-most-once which means that it can be lost permanently if the JMS
# provider fails.
public const NON_PERSISTENT = "NON_PERSISTENT";

public type SendConfiguration record {|
    DeliveryMode deliveryMode = PERSISTENT;
    int priority = 4;
    int timeToLiveInMilliSeconds = 0;
|};

function validateQueue(Destination destination) returns error? {
    validate(destination);
    if (destination.getType() != QUEUE) {
        string errorMessage = "Destination should should be a queue";
        error err = error(JMS_ERROR_CODE, message = errorMessage);
        panic err;
    }
}
function validateTopic(Destination destination) returns error? {
    validate(destination);
     if (destination.getType() != TOPIC) {
        string errorMessage = "Destination should should be a topic";
        error err = error(JMS_ERROR_CODE, message = errorMessage);
        return err;
    }
}

function validate(Destination destination) returns error? {
    if (destination.getName() == "") {
        string errorMessage = "Destination name cannot be empty";
        error queueReceiverConfigError = error(JMS_ERROR_CODE, message = errorMessage);
        return queueReceiverConfigError;
    }
}

function getQueueDestination(Destination|string queue) returns Destination | error {
    if (queue is string) {
        return new Destination(self.session, queueName, QUEUE);
    } else {
        check validateQueue(queue);
        return queue;
    }
}

function getTopicDestination(Destination|string topic) returns Destination | error {
    if (topic is string) {
        return new Destination(self.session, topic, TOPIC);
    } else {
        check validateTopic(topic);
        return topic;
    }
}

