/*
 *  Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package org.ballerinalang.test.packaging;

import org.ballerinalang.test.BaseTest;
import org.ballerinalang.test.context.BServerInstance;
import org.ballerinalang.test.context.BallerinaTestException;
import org.ballerinalang.test.context.LogLeecher;
import org.testng.annotations.Test;

import java.nio.file.Path;
import java.nio.file.Paths;

public class ModuleExecutionFlowTests extends BaseTest {
    public static final int TIMEOUT = 10000;

    @Test
    public void testModuleExecutionOrder() throws BallerinaTestException {
        Path projectPath = Paths.get("src", "test", "resources", "packaging", "proj1");
        runAndAssert(projectPath);
    }

    @Test
    public void testModuleMainExecutionOrder() throws BallerinaTestException {
        Path projectPath = Paths.get("src", "test", "resources", "packaging", "proj6");
        runAndAssert(projectPath);
    }

    @Test
    public void testModuleDependencyChainForInit() throws BallerinaTestException, InterruptedException {
        Path projectPath = Paths.get("src", "test", "resources", "packaging", "ModuleInitInvocationProject");
        runAndAssert(projectPath);
    }

    private void runAndAssert(Path projectPath) throws BallerinaTestException {
        BServerInstance serverInstance = new BServerInstance(balServer);
        serverInstance.startServer(projectPath.toAbsolutePath().toString(), "c", null, null, null);
        LogLeecher errLeecherA = new LogLeecher("Stopped module A", LogLeecher.LeecherType.ERROR);
        LogLeecher errLeecherB = new LogLeecher("Stopped module B", LogLeecher.LeecherType.ERROR);
        LogLeecher errLeecherC = new LogLeecher("Stopped module C", LogLeecher.LeecherType.ERROR);
        serverInstance.addErrorLogLeecher(errLeecherA);
        serverInstance.addErrorLogLeecher(errLeecherB);
        serverInstance.addErrorLogLeecher(errLeecherC);
        serverInstance.shutdownServer();
        errLeecherA.waitForText(TIMEOUT);
        errLeecherB.waitForText(TIMEOUT);
        errLeecherC.waitForText(TIMEOUT);
        serverInstance.removeAllLeechers();
    }
}
