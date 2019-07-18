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

import ballerina/log;

# JMS Producer Endpoint
#
# + session - Session of the queue sender
public type QueueProducer client object {

    private Session session;
    private Destination? dest;

    # Initialize the Producer endpoint
    #
    # + c - The JMS Session object or Configurations related to the receiver
    # + queueName - Name of the target queue
    public function __init(Session | EndpointConfiguration sessionOrEndpointConfig, public string|Destination? queue = (), public SendConfiguration config = {}) returns error? {
        if (sessionOrEndpointConfig is Session) {
            self.session = sessionOrEndpointConfig;
        } else {
            Connection conn = check new({
                    initialContextFactory: sessionOrEndpointConfig.initialContextFactory,
                    providerUrl: sessionOrEndpointConfig.providerUrl,
                    connectionFactoryName: sessionOrEndpointConfig.connectionFactoryName,
                    properties: sessionOrEndpointConfig.properties,
                    username: sessionOrEndpointConfig["username"], 
                    password: sessionOrEndpointConfig["password"]
                });
            self.session = check new Session(conn, {
                    acknowledgementMode: sessionOrEndpointConfig.acknowledgementMode
                });
        }
        if (queue!=()) {
            dest = check getQueueDestination(queue);
        }
        return init(config);
    }

    private function init(SendConfiguration config) returns error? = external;

    # Sends a message to the JMS provider.
    # If the `payload` is a `string` then a `TEXT_MESSAGE` would be sent.
    # If the `payload` is a `byte[]` then a  `BYTES_MESSAGE` would be sent.
    # If the `payload` is a `map` then a MAP_MESSAGE would be senst.
    # If the `payload` is `Message` then it would be sent.
    # 
    # + payload - Message payload to be sent to the JMS provider
    # + return - Error if unable to create or send the message to the queue
    public remote function send(public string | byte[] |
    map<string | byte | int | float | boolean | byte[] |()> | Message payload, public SendConfiguration config = {},
     public CustomHeaders? headers = (), 
     public map<string | int | float | boolean | byte | json | xml>? properties = ()) returns error? {
        if(dest == ()) {
            Error err = error(JMS_ERROR_CODE, message = "The destination is not specified when creating the producer. Use sendTo function instead.");
            return err;
        } else {
            externSend(check getMessage(payload, headers, properties), config);
        }
    }

    public remote function externSend(Message message, SendConfiguration config) returns error? = external;

    # Sends a message to a given destination of the JMS provider
    #
    # + destination - Destination used for the message sender
    # + message - Message to be sent to the JMS provider
    # + return - Error if sending to the given destination fails
    public remote function sendTo(string|Destination queue, public string | byte[] |
    map<string | byte | int | float | boolean | byte[] |()> | Message payload, public SendConfiguration? config = {}, 
    public CustomHeaders? headers = (), 
    public map<string | int | float | boolean | byte | json | xml>? properties = ()) returns error? {
        self.externSendTo(check getQueueDestination(queue), check getMessage(payload, headers, properties), config);
    }

    public remote function externSendTo(Destination destination, Message message, SendConfiguration config) returns error? = external;

    public remote function close() returns error? = external;
};
    
