/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.shardingsphere.data.pipeline.postgresql.sqlbuilder;

import org.apache.shardingsphere.data.pipeline.api.ingest.record.Column;
import org.apache.shardingsphere.data.pipeline.api.ingest.record.DataRecord;
import org.apache.shardingsphere.data.pipeline.postgresql.ingest.wal.WALPosition;
import org.apache.shardingsphere.data.pipeline.postgresql.ingest.wal.decode.PostgreSQLLogSequenceNumber;
import org.junit.Test;
import org.postgresql.replication.LogSequenceNumber;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public final class PostgreSQLPipelineSQLBuilderTest {
    
    private final PostgreSQLPipelineSQLBuilder sqlBuilder = new PostgreSQLPipelineSQLBuilder();
    
    @Test
    public void assertBuildInsertSQL() {
        String actual = sqlBuilder.buildInsertSQL("schema1", mockDataRecord());
        assertThat(actual, is("INSERT INTO schema1.t_order(order_id,user_id,status) VALUES(?,?,?) ON CONFLICT (order_id)"
                + " DO UPDATE SET user_id=EXCLUDED.user_id,status=EXCLUDED.status"));
    }
    
    private DataRecord mockDataRecord() {
        DataRecord result = new DataRecord(new WALPosition(new PostgreSQLLogSequenceNumber(LogSequenceNumber.valueOf(100L))), 2);
        result.setTableName("t_order");
        result.addColumn(new Column("order_id", 1, true, true));
        result.addColumn(new Column("user_id", 2, true, false));
        result.addColumn(new Column("status", "ok", true, false));
        return result;
    }
}
