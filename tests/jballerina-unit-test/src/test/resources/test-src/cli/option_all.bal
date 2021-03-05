// Copyright (c) 2021 WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

import ballerina/lang.'xml;
import ballerina/test;

public type Student record {|
    string name;
    boolean good;
    int score;
    float height;
    decimal energy;
    xml books;
    int[] ratings;
    boolean[] friends;
    string? val;
|};

public function main(*Student student) {
    test:assertEquals(student.name, "Riyafa=Riyafa");
    test:assertEquals(student.good, true);
    test:assertEquals(student.score, 100);
    test:assertEquals(student.height, 5.5);
    decimal val = 1e100;
    test:assertEquals(student.energy, val);
    string xmlString = "<?xml version=\"1.0\"?><catalog><book id=\"bk101\"><author>Gambardella, " +
        "Matthew</author><title>XML Developer's Guide</title></book><book id=\"bk102\"><author>Ralls, " +
        "Kim</author><title>Midnight Rain</title></book></catalog>";
    test:assertEquals(student.books, checkpanic 'xml:fromString(xmlString));
    int[] ratings = [5, 3];
    test:assertEquals(student.ratings, ratings);
    boolean[] friends = [false, true];
    test:assertEquals(student.friends, friends);
}
