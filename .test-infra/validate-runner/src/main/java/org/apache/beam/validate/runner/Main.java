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
package org.apache.beam.validate.runner;

import net.sf.json.JSONArray;
import org.apache.beam.validate.runner.service.BatchTestService;
import org.apache.beam.validate.runner.service.StreamTestService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.io.FileWriter;
import java.io.IOException;

public class Main {
    public static void main(String args[]) {
        try {
            final Logger logger = LoggerFactory.getLogger(Main.class);

            String outputFile;
            if (args.length == 0) {
                logger.info("Output file name missing. Output will be saved to capability.json");
                outputFile = "capability";
            } else {
                outputFile = args[0];
                logger.info("Output will be saved to {}.json", outputFile);
            }
            JSONArray outputDetails = new JSONArray();

            logger.info("Processing Batch Jobs:");
            BatchTestService batchTestService = new BatchTestService();
            outputDetails.add(batchTestService.getBatchTests());

            logger.info("Processing Stream Jobs:");
            StreamTestService streamTestService = new StreamTestService();
            outputDetails.add(streamTestService.getStreamTests());

            try (FileWriter file = new FileWriter(outputFile + ".json")) {
                file.write(outputDetails.toString(3));
                file.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}