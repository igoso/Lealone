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
package org.lealone.db.expression;

import org.lealone.db.result.LocalResult;

public interface SelectUnion {

    /**
     * The type of a UNION statement.
     */
    public static final int UNION = 0;

    /**
     * The type of a UNION ALL statement.
     */
    public static final int UNION_ALL = 1;

    /**
     * The type of an EXCEPT statement.
     */
    public static final int EXCEPT = 2;

    /**
     * The type of an INTERSECT statement.
     */
    public static final int INTERSECT = 3;

    int getUnionType();

    Query getLeft();

    Query getRight();

    LocalResult getEmptyResult();

}
