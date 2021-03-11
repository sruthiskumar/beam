/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.beam.validate.runner.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.sf.json.JSONObject;
import org.apache.beam.validate.runner.model.CaseResult;
import org.apache.beam.validate.runner.model.Configuration;
import org.apache.beam.validate.runner.model.TestResult;
import org.apache.beam.validate.runner.util.FileReaderUtil;

import org.apache.commons.lang3.tuple.Pair;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class BatchTestService implements TestService {

    // Stores all the tests which are run across runners in batch mode
    private static Set<Pair<String, String>> batchTests = new HashSet<>();

    //Stores the tests which are run for the particular runner.
    private HashMap<String, Set<CaseResult>> map = new HashMap<>();

    public JSONObject getBatchTests() {
        try {
            Configuration configuration = FileReaderUtil.readConfiguration();
            for(Map<String, String> job : configuration.getBatch()) {
                try {
                    TestResult testResult = new ObjectMapper().readValue(getUrl(job,configuration), TestResult.class);
                    batchTests.addAll(getTestNames(testResult));
                    map.put((String) job.keySet().toArray()[0], getAllTests(testResult));
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        JSONObject outputDetails = new JSONObject();
        outputDetails.put("batch", process(batchTests, map));
        return outputDetails;
    }
}