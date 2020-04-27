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
package org.apache.beam.sdk.extensions.ml;

import static org.junit.Assert.assertEquals;

import com.google.privacy.dlp.v2.CharacterMaskConfig;
import com.google.privacy.dlp.v2.DeidentifyConfig;
import com.google.privacy.dlp.v2.Finding;
import com.google.privacy.dlp.v2.InfoType;
import com.google.privacy.dlp.v2.InfoTypeTransformations;
import com.google.privacy.dlp.v2.InspectConfig;
import com.google.privacy.dlp.v2.Likelihood;
import com.google.privacy.dlp.v2.PrimitiveTransformation;
import java.util.ArrayList;
import java.util.List;
import org.apache.beam.sdk.extensions.gcp.options.GcpOptions;
import org.apache.beam.sdk.testing.PAssert;
import org.apache.beam.sdk.testing.TestPipeline;
import org.apache.beam.sdk.transforms.Create;
import org.apache.beam.sdk.transforms.SerializableFunction;
import org.apache.beam.sdk.values.KV;
import org.apache.beam.sdk.values.PCollection;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class DLPTextOperationsIT {
  @Rule public TestPipeline testPipeline = TestPipeline.create();

  private static final String IDENTIFYING_TEXT = "mary.sue@example.com";
  private static InfoType emailAddress = InfoType.newBuilder().setName("EMAIL_ADDRESS").build();;
  private static InspectConfig inspectConfig =
      InspectConfig.newBuilder()
          .addInfoTypes(emailAddress)
          .setMinLikelihood(Likelihood.LIKELY)
          .build();

  @Test
  public void inspectsText() {
    String projectId = testPipeline.getOptions().as(GcpOptions.class).getProject();
    PCollection<List<Finding>> inspectionResult =
        testPipeline
            .apply(Create.of(KV.of("", IDENTIFYING_TEXT)))
            .apply(
                DLPInspectText.newBuilder()
                    .setBatchSize(52400)
                    .setProjectId(projectId)
                    .setInspectConfig(inspectConfig)
                    .build());
    PAssert.that(inspectionResult).satisfies(new VerifyInspectionResult());
    testPipeline.run().waitUntilFinish();
  }

  @Test
  public void deidentifiesText() {
    emailAddress = InfoType.newBuilder().setName("EMAIL_ADDRESS").build();
    String projectId = testPipeline.getOptions().as(GcpOptions.class).getProject();

    PCollection<KV<String, String>> deidentificationResult =
        testPipeline
            .apply(Create.of(KV.of("", IDENTIFYING_TEXT)))
            .apply(
                DLPDeidentifyText.newBuilder()
                    .setBatchSize(52400)
                    .setProjectId(projectId)
                    .setInspectConfig(inspectConfig)
                    .setDeidentifyConfig(getDeidentifyConfig())
                    .build());
    PAssert.that(deidentificationResult).containsInAnyOrder(KV.of("", "####################"));
    testPipeline.run().waitUntilFinish();
  }

  private DeidentifyConfig getDeidentifyConfig() {
    CharacterMaskConfig characterMaskConfig =
        CharacterMaskConfig.newBuilder().setMaskingCharacter("#").build();
    PrimitiveTransformation primitiveTransformation =
        PrimitiveTransformation.newBuilder().setCharacterMaskConfig(characterMaskConfig).build();
    InfoTypeTransformations.InfoTypeTransformation infoTypeTransformation =
        InfoTypeTransformations.InfoTypeTransformation.newBuilder()
            .addInfoTypes(emailAddress)
            .setPrimitiveTransformation(primitiveTransformation)
            .build();
    return DeidentifyConfig.newBuilder()
        .setInfoTypeTransformations(
            InfoTypeTransformations.newBuilder().addTransformations(infoTypeTransformation).build())
        .build();
  }

  private static class VerifyInspectionResult
      implements SerializableFunction<Iterable<List<Finding>>, Void> {
    @Override
    public Void apply(Iterable<List<Finding>> input) {
      List<Boolean> matches = new ArrayList<>();
      input.forEach(
          resultList ->
              matches.add(
                  resultList.stream()
                      .anyMatch(finding -> finding.getInfoType().equals(emailAddress))));
      assertEquals(Boolean.TRUE, matches.contains(Boolean.TRUE));
      return null;
    }
  }
}
