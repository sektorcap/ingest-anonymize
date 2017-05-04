/*
 * Copyright [2017] [Ettore Caprella]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.elasticsearch.plugin.ingest.anonymize;

import org.elasticsearch.ingest.IngestDocument;
import org.elasticsearch.ingest.RandomDocumentPicks;
import org.elasticsearch.test.ESTestCase;

import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;
import java.io.IOException;


import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.anything;
import static org.hamcrest.Matchers.not;

public class AnonymizeProcessorTests extends ESTestCase {

    public void testThatProcessorWorks() throws Exception {
        Map<String, Object> document = new HashMap<>();
        document.put("source_field", "fancy source field content");
        IngestDocument ingestDocument = RandomDocumentPicks.randomIngestDocument(random(), document);

        AnonymizeProcessor processor = new AnonymizeProcessor(randomAsciiOfLength(10), "source_field",
                                                                                       "target_field",
                                                                                       "testkey",
                                                                                       "HmacSHA1",
                                                                                       true);
        processor.execute(ingestDocument);
        Map<String, Object> data = ingestDocument.getSourceAndMetadata();

        assertThat(data, hasKey("target_field"));
        assertThat(data.get("target_field"), is("d47e2517477afbfc29fc3f822cd5f2cf5c7a2e8b"));
    }

    public void testThatProcessorWorksWithArray() throws Exception {
        Map<String, Object> document = new HashMap<>();
        ArrayList<String> content = new ArrayList<>(Arrays.asList("xyz", "abc", "1234567890"));
        document.put("source_field", content);
        IngestDocument ingestDocument = RandomDocumentPicks.randomIngestDocument(random(), document);

        AnonymizeProcessor processor = new AnonymizeProcessor(randomAsciiOfLength(10), "source_field",
                                                                                       "target_field",
                                                                                       "testkey",
                                                                                       "HmacSHA1",
                                                                                       true);
        processor.execute(ingestDocument);
        Map<String, Object> data = ingestDocument.getSourceAndMetadata();

        assertThat(data, hasKey("target_field"));
        ArrayList<String> result = new ArrayList<>(Arrays.asList("7bab393a04bad0737d5ff52f82c8fcece6490c77",
                                                                 "e48edab11ade7662faafc534c33315118bdb795b",
                                                                 "a32ca25ccf1ff7dabb0d1bd594bda459fa59368f"));
        assertThat(data.get("target_field"), is(result));
    }

    public void testThatProcessorWorksWithIgnoreMissingAndThrowException() throws Exception {
        Map<String, Object> document = new HashMap<>();
        document.put("source_field", null);
        IngestDocument ingestDocument = RandomDocumentPicks.randomIngestDocument(random(), document);

        AnonymizeProcessor processor = new AnonymizeProcessor(randomAsciiOfLength(10), "source_field",
                                                                                       "target_field",
                                                                                       "testkey",
                                                                                       "HmacSHA1",
                                                                                       false);
        try {
            processor.execute(ingestDocument);
            assertThat("never here", is("")); // never here
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), is("field [source_field] is null, cannot do anonymize."));
        }
    }

}
