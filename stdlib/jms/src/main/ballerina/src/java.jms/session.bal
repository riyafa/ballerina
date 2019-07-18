// Copyright (config) 2018 WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

# Represents the JMS session.
#
# + config - Stores the configurations related to a JMS session.
public type Session object {

    private Connection conn;

    # The default constructor of the JMS session.
    public function __init(Connection connection, SessionAcknowledgementMode ackMode = AUTO_ACKNOWLEDGE) returns error? {
        self.conn = connection;
        self.init(connection, ackMode);
    }

    private function init(Connection connection, SessionAcknowledgementMode ackMode) = external;

    # Unsubscribes a durable subscription that has been created by a client.
    # It is erroneous for a client to delete a durable subscription while there is an active (not closed) consumer
    # for the subscription, or while a consumed message being part of a pending transaction or has not been
    # acknowledged in the session.
    #
    # + subscriptionId - The name, which is used to identify the subscription.
    # + return - Cancels the subscription.
    public function unsubscribe(string subscriptionId) returns error? = external;
};

public type SessionAcknowledgementMode AUTO_ACKNOWLEDGE  | CLIENT_ACKNOWLEDGE | DUPS_OK_ACKNOWLEDGE | SESSION_TRANSACTED;

public const AUTO_ACKNOWLEDGE = "AUTO_ACKNOWLEDGE";
public const CLIENT_ACKNOWLEDGE = "CLIENT_ACKNOWLEDGE";
public const DUPS_OK_ACKNOWLEDGE = "DUPS_OK_ACKNOWLEDGE";
public const SESSION_TRANSACTED  = "SESSION_TRANSACTED ";
