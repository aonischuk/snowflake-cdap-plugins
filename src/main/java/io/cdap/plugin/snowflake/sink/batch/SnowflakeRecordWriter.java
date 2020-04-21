/*
 * Copyright © 2020 Cask Data, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package io.cdap.plugin.snowflake.sink.batch;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.mapreduce.RecordWriter;
import org.apache.hadoop.mapreduce.TaskAttemptContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Writes csv records into batches and submits them to Snowflake.
 * Accepts <code>null</code> as a key, and CSVRecord as a value.
 */
public class SnowflakeRecordWriter extends RecordWriter<NullWritable, CSVRecord> {
  private static final Logger LOG = LoggerFactory.getLogger(SnowflakeRecordWriter.class);
  private static final Gson GSON = new GsonBuilder().create();

  private final CSVBuffer csvBuffer;
  private final CSVBuffer csvBufferSizeCheck;
  private final SnowflakeSinkConfig config;
  private final SnowflakeSinkAccessor snowflakeAccessor;
  private final String destinationStagePath;
  private long totalWriteTime = 0;

  public SnowflakeRecordWriter(TaskAttemptContext taskAttemptContext) throws IOException {
    LOG.info("SnowflakeRecordWriter a");
    Configuration conf = taskAttemptContext.getConfiguration();
    destinationStagePath = conf.get(SnowflakeOutputFormat.DESTINATION_STAGE_PATH_PROPERTY);
    String configJson = conf.get(
      SnowflakeOutputFormatProvider.PROPERTY_CONFIG_JSON);
    config = GSON.fromJson(
      configJson, SnowflakeSinkConfig.class);

    csvBuffer = new CSVBuffer(true);
    csvBufferSizeCheck = new CSVBuffer(false);
    snowflakeAccessor = new SnowflakeSinkAccessor(config);
    LOG.info("SnowflakeRecordWriter b");
  }

  @Override
  public void write(NullWritable key, CSVRecord csvRecord) throws IOException {
    long l = System.currentTimeMillis();
    csvBufferSizeCheck.reset();
    csvBufferSizeCheck.write(csvRecord);

    if (config.getMaxFileSize() > 0 && csvBuffer.size() + csvBufferSizeCheck.size() > config.getMaxFileSize()) {
      submitCurrentBatch();
    }

    csvBuffer.write(csvRecord);
    long diff = System.currentTimeMillis() - l;
    totalWriteTime += diff;
  }

  private void submitCurrentBatch() throws IOException {
    LOG.info("submitCurrentBatch a");
    if (csvBuffer.getRecordsCount() != 0) {
      InputStream csvInputStream = new ByteArrayInputStream(csvBuffer.getByteArray());
      snowflakeAccessor.uploadStream(csvInputStream, destinationStagePath);

      csvBuffer.reset();
    }
    LOG.info("submitCurrentBatch b");
  }

  @Override
  public void close(TaskAttemptContext taskAttemptContext) throws IOException {
    LOG.info(String.format("[Sink]Close a %d", totalWriteTime));
    submitCurrentBatch();
    LOG.info("[Sink]Close b");
  }
}
