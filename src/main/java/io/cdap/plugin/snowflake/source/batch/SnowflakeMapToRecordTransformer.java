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

package io.cdap.plugin.snowflake.source.batch;

import com.google.common.base.Strings;
import io.cdap.cdap.api.common.Bytes;
import io.cdap.cdap.api.data.format.StructuredRecord;
import io.cdap.cdap.api.data.format.UnexpectedFormatException;
import io.cdap.cdap.api.data.schema.Schema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Transforms Snowflake row into {@link StructuredRecord}.
 */
public class SnowflakeMapToRecordTransformer {
  private static final Logger LOG = LoggerFactory.getLogger(SnowflakeMapToRecordTransformer.class);

  private final Schema schema;

  public SnowflakeMapToRecordTransformer(Schema schema) {
    this.schema = schema;
  }

  public StructuredRecord transform(Map<String, String> row) {
    return getStructuredRecord(row, schema);
  }

  private StructuredRecord getStructuredRecord(Map<String, String> row, Schema schema) {
    StructuredRecord.Builder builder = StructuredRecord.builder(schema);
    row.entrySet().stream()
      .filter(entry -> schema.getField(entry.getKey()) != null) // filter absent fields in the schema
      .forEach(entry -> builder.set(
        entry.getKey(),
        convertValue(entry.getKey(), entry.getValue(), schema.getField(entry.getKey()).getSchema())));
    return builder.build();
  }

  private Object convertValue(String fieldName, Object value, Schema fieldSchema) {
    if (fieldSchema.isNullable()) {
      return convertValue(fieldName, value, fieldSchema.getNonNullable());
    }

    Schema.Type fieldSchemaType = fieldSchema.getType();

    // empty string is considered null in csv
    if (value == null || (value instanceof String && Strings.isNullOrEmpty((String) value))) {
      return null;
    }

    Schema.LogicalType logicalType = fieldSchema.getLogicalType();
    if (logicalType != null) {
      switch (logicalType) {
        case DATE:
          // date will be in yyyy-mm-dd format
          return Math.toIntExact(LocalDate.parse(String.valueOf(value)).toEpochDay());
        case TIMESTAMP_MICROS:
          Instant instant = DateTimeFormatter.ISO_OFFSET_DATE_TIME.parse(String.valueOf(value), Instant::from);
          return TimeUnit.MILLISECONDS.toMicros(instant.toEpochMilli());
        case TIME_MICROS:
          return TimeUnit.NANOSECONDS.toMicros(LocalTime.parse(String.valueOf(value)).toNanoOfDay());
        case DECIMAL:
          return new BigDecimal(String.valueOf(value)).setScale(fieldSchema.getScale()).unscaledValue().toByteArray();
        default:
          throw new IllegalArgumentException(
            String.format("Field '%s' is of unsupported type '%s'", fieldName, logicalType.getToken()));
      }
    }

    switch (fieldSchemaType) {
      case NULL:
        return null;
      case BYTES:
        return Bytes.toBytes(castValue(value, fieldName, String.class));
      case BOOLEAN:
        return Boolean.parseBoolean(castValue(value, fieldName, String.class));
      case DOUBLE:
        return Double.parseDouble(castValue(value, fieldName, String.class));
      case STRING:
        return value;
    }

    throw new UnexpectedFormatException(
      String.format("Unsupported schema type: '%s' for field: '%s'. Supported types are 'bytes, boolean, "
                      + "double, string'.", fieldSchema, fieldName));
  }

  private <T> T castValue(Object value, String fieldName, Class<T> clazz) {
    if (clazz.isAssignableFrom(value.getClass())) {
      return clazz.cast(value);
    }
    throw new UnexpectedFormatException(
      String.format("Field '%s' is not of expected type '%s'", fieldName, clazz.getSimpleName()));
  }
}
