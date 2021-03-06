/**
 * Copyright © 2016-2017 Cloudera, Inc.
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
 */
package com.cloudera.labs.envelope.plan;

import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.apache.spark.sql.DataFrame;
import org.apache.spark.sql.functions;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.typesafe.config.Config;

import scala.Tuple2;

/**
 * A planner implementation for appending the stream to the storage table. Only plans insert mutations.
 */
public class AppendPlanner implements BulkPlanner {

  public final static String KEY_FIELD_NAMES_CONFIG_NAME = "fields.key";
  public final static String LAST_UPDATED_FIELD_NAME_CONFIG_NAME = "field.last.updated";
  public final static String UUID_KEY_CONFIG_NAME = "uuid.key.enabled";

  private Config config;

  @Override
  public void configure(Config config) {
    this.config = config;
  }

  @Override
  public List<Tuple2<MutationType, DataFrame>> planMutationsForSet(DataFrame arriving)
  {
    if (setsKeyToUUID()) {
      if (!hasKeyFields()) {
        throw new RuntimeException("Key columns must be specified to provide UUID keys");
      }

      arriving = arriving.withColumn(getKeyFieldNames().get(0), functions.lit(UUID.randomUUID().toString()));
    }

    if (hasLastUpdatedField()) {
      arriving = arriving.withColumn(getLastUpdatedFieldName(), functions.lit(currentTimestampString()));
    }

    List<Tuple2<MutationType, DataFrame>> planned = Lists.newArrayList();

    planned.add(new Tuple2<MutationType, DataFrame>(MutationType.INSERT, arriving));

    return planned;
  }

  @Override
  public Set<MutationType> getEmittedMutationTypes() {
    return Sets.newHashSet(MutationType.INSERT);
  }

  private String currentTimestampString() {
    return new Date(System.currentTimeMillis()).toString();
  }

  private boolean hasKeyFields() {
    return config.hasPath(KEY_FIELD_NAMES_CONFIG_NAME);
  }

  private List<String> getKeyFieldNames() {
    return config.getStringList(KEY_FIELD_NAMES_CONFIG_NAME);
  }

  private boolean hasLastUpdatedField() {
    return config.hasPath(LAST_UPDATED_FIELD_NAME_CONFIG_NAME);
  }

  private String getLastUpdatedFieldName() {
    return config.getString(LAST_UPDATED_FIELD_NAME_CONFIG_NAME);
  }

  private boolean setsKeyToUUID() {
    if (!config.hasPath(UUID_KEY_CONFIG_NAME)) return false;

    return Boolean.parseBoolean(config.getString(UUID_KEY_CONFIG_NAME));
  }

}
