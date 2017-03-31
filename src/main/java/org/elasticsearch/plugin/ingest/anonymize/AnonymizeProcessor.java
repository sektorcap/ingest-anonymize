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

import org.elasticsearch.ingest.AbstractProcessor;
import org.elasticsearch.ingest.IngestDocument;
import org.elasticsearch.ingest.Processor;

import java.io.IOException;
import java.util.Map;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import static org.elasticsearch.ingest.ConfigurationUtils.readStringProperty;

public class AnonymizeProcessor extends AbstractProcessor {

    public static final String TYPE = "anonymize";

    private final String field;
    private final String targetField;
    private final String key;
    private final String method;

    public AnonymizeProcessor(String tag, String field, String targetField, String key, String method) throws IOException {
        super(tag);
        this.field = field;
        this.targetField = targetField;
        this.key = key;
        this.method = method;
    }

    @Override
    public void execute(IngestDocument ingestDocument) throws Exception {
      List<?> list = null;
      try {
        String in = ingestDocument.getFieldValue(field, String.class);
        list = new ArrayList<>(Arrays.asList(in));
      } catch (IllegalArgumentException e) {
        list = ingestDocument.getFieldValue(field, ArrayList.class, true);
        if (list == null)
            throw new IllegalArgumentException("field [" + field + "] is null, cannot do fingerprint.");
      }

      Mac mac = Mac.getInstance(method);
      SecretKeySpec signingKey = new SecretKeySpec(key.getBytes(Charset.defaultCharset()), method);
      mac.init(signingKey);
      ArrayList<String> outcontent = new ArrayList<>(list.size());
      for (Object o : list) {
          String s = o.toString();
          byte[] b = mac.doFinal(s.getBytes(Charset.defaultCharset()));
          String hash = new BigInteger(1, b).toString(16);
          outcontent.add(hash);
      }
      if (outcontent.size() == 1)
        ingestDocument.setFieldValue(targetField, outcontent.get(0));
      else
        ingestDocument.setFieldValue(targetField, outcontent);
    }

    @Override
    public String getType() {
        return TYPE;
    }

    public static final class Factory implements Processor.Factory {

        @Override
        public AnonymizeProcessor create(Map<String, Processor.Factory> factories, String tag, Map<String, Object> config)
            throws Exception {
            String field = readStringProperty(TYPE, tag, config, "field");
            String targetField = readStringProperty(TYPE, tag, config, "target_field", field);
            String key = readStringProperty(TYPE, tag, config, "key", "supersecrethere");
            String method = readStringProperty(TYPE, tag, config, "method", "HmacSHA1");

            return new AnonymizeProcessor(tag, field, targetField, key, method);
        }
    }
}
