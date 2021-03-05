/*
 * Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.ballerinalang.test.cli;

import org.ballerinalang.test.BCompileUtil;
import org.ballerinalang.test.BRunUtil;
import org.ballerinalang.test.CompileResult;
import org.testng.annotations.Test;

public class OptionTest {
    @Test
    public void testCliOptionArg() {
        CompileResult compileResult = BCompileUtil.compile("test-src/cli/option_all.bal");
        String xml = "<?xml version=\"1.0\"?><catalog><book id=\"bk101\"><author>Gambardella, " +
                "Matthew</author><title>XML Developer's Guide</title></book><book id=\"bk102\"><author>Ralls, " +
                "Kim</author><title>Midnight Rain</title></book></catalog>";

        String[] args =
                {"--name", "Riyafa=Riyafa", "--good", "false", "--score", "100", "--height", "5.5", "--energy", "10e99",
                        "--books", xml, "--bad", "--ratings", "5", "--ratings","3", "--friends", "false",
                        "--friends", "--good"};
        BRunUtil.runMain(compileResult, args);
    }

    @Test
    public void testCliOptionNamedArg() {
        CompileResult compileResult = BCompileUtil.compile("test-src/cli/option_all.bal");
        String xml = "<?xml version=\"1.0\"?><catalog><book id=\"bk101\"><author>Gambardella, " +
                "Matthew</author><title>XML Developer's Guide</title></book><book id=\"bk102\"><author>Ralls, " +
                "Kim</author><title>Midnight Rain</title></book></catalog>";

        String[] args =
                {"--name=Riyafa=Riyafa", "--good=false", "--score=100", "--height=5.5", "--energy=10e99",
                        "--books=" + xml, "--friends=false", "--rating=5", "--ratings=3", "--friends", "--good"};
        BRunUtil.runMain(compileResult, args);
    }

}
