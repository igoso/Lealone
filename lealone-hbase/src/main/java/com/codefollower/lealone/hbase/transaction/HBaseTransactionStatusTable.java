/*
 * Copyright 2011 The Apache Software Foundation
 *
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
package com.codefollower.lealone.hbase.transaction;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Set;

import org.apache.hadoop.hbase.HColumnDescriptor;
import org.apache.hadoop.hbase.HTableDescriptor;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.HBaseAdmin;
import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.util.Bytes;
import com.codefollower.lealone.hbase.util.HBaseUtils;
import com.codefollower.lealone.message.DbException;
import com.codefollower.lealone.transaction.DistributedTransaction;

public class HBaseTransactionStatusTable {
    private final static byte[] TABLE_NAME = Bytes.toBytes("LEALONE_TRANSACTION_STATUS_TABLE");
    final static byte[] FAMILY = Bytes.toBytes("f");
    //final static byte[] START_TIMESTAMP = Bytes.toBytes("t");
    final static byte[] SERVER = Bytes.toBytes("s");
    final static byte[] COMMIT_TIMESTAMP = Bytes.toBytes("c");

    public synchronized static void createTableIfNotExists() throws Exception {
        HBaseAdmin admin = HBaseUtils.getHBaseAdmin();
        HColumnDescriptor hcd = new HColumnDescriptor(FAMILY);
        hcd.setMaxVersions(Integer.MAX_VALUE);

        HTableDescriptor htd = new HTableDescriptor(TABLE_NAME);
        htd.addFamily(hcd);
        if (!admin.tableExists(TABLE_NAME)) {
            admin.createTable(htd);
        }
    }

    public synchronized static void dropTableIfExists() throws Exception {
        HBaseAdmin admin = HBaseUtils.getHBaseAdmin();
        if (admin.tableExists(TABLE_NAME)) {
            admin.disableTable(TABLE_NAME);
            admin.deleteTable(TABLE_NAME);
        }
    }

    private final static HBaseTransactionStatusTable st = new HBaseTransactionStatusTable();

    public static HBaseTransactionStatusTable getInstance() {
        return st;
    }

    private final HTable table;

    public HBaseTransactionStatusTable() {
        try {
            createTableIfNotExists();
            table = new HTable(HBaseUtils.getConfiguration(), TABLE_NAME);
        } catch (Exception e) {
            throw DbException.convert(e);
        }
    }

    public void addRecord(DistributedTransaction transaction) {
        Set<DistributedTransaction> distributedTransactions = transaction.getChildren();
        if (distributedTransactions != null && !distributedTransactions.isEmpty()) {
            StringBuilder buff = new StringBuilder();
            for (DistributedTransaction dt : distributedTransactions) {
                if (buff.length() > 0)
                    buff.append(',');

                buff.append(dt.getHostAndPort()).append(':').append(dt.getTransactionId());
            }

            ArrayList<Put> list = new ArrayList<Put>(distributedTransactions.size());
            String serverStr = buff.toString();

            byte[] rowKey;
            Put put;

            for (DistributedTransaction dt : distributedTransactions) {
                buff.setLength(0);
                rowKey = Bytes.toBytes(buff.append(dt.getHostAndPort()).append(':').append(dt.getTransactionId()).toString());
                put = new Put(rowKey);
                put.add(FAMILY, SERVER, dt.getTransactionId(), Bytes.toBytes(serverStr));
                put.add(FAMILY, COMMIT_TIMESTAMP, dt.getTransactionId(), Bytes.toBytes(dt.getCommitTimestamp()));
                list.add(put);
            }

            try {
                table.put(list);
            } catch (IOException e) {
                throw DbException.convert(e);
            }
        }
    }

    public long query(String hostAndPort, long queryTimestamp) {
        Get get = new Get(Bytes.toBytes(hostAndPort + "." + Long.toString(queryTimestamp)));
        get.setTimeStamp(queryTimestamp);
        try {
            long commitTimestamp = -1;
            Result r = table.get(get);
            if (r != null && !r.isEmpty()) {
                commitTimestamp = Bytes.toLong(r.getValue(FAMILY, COMMIT_TIMESTAMP));
                String serverStr = Bytes.toString(r.getValue(FAMILY, SERVER));
                String[] servers = serverStr.split(",");
                for (String server : servers) {
                    get = new Get(Bytes.toBytes(server));
                    r = table.get(get);
                    if (r == null || r.isEmpty()) {
                        commitTimestamp = -1;
                        break;
                    }
                }
            }
            return commitTimestamp;
        } catch (IOException e) {
            throw DbException.convert(e);
        }
    }
}