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

package org.apache.shardingsphere.test.integration.engine.dcl;

import org.apache.shardingsphere.test.integration.cases.SQLCommandType;
import org.apache.shardingsphere.test.integration.cases.SQLExecuteType;
import org.apache.shardingsphere.test.integration.framework.container.compose.ComposedContainerManager;
import org.apache.shardingsphere.test.integration.framework.param.ParameterizedArrayFactory;
import org.apache.shardingsphere.test.integration.framework.param.model.AssertionParameterizedArray;
import org.apache.shardingsphere.test.integration.framework.param.model.ParameterizedArray;
import org.apache.shardingsphere.test.integration.framework.runner.parallel.annotaion.ParallelLevel;
import org.apache.shardingsphere.test.integration.framework.runner.parallel.annotaion.ParallelRuntimeStrategy;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

import java.sql.Connection;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.Collection;
import java.util.stream.Collectors;

@ParallelRuntimeStrategy(ParallelLevel.SCENARIO)
public final class GeneralDCLIT extends BaseDCLIT {
    
    @ClassRule
    public static final ComposedContainerManager COMPOSED_CONTAINER_MANAGER = new ComposedContainerManager("GeneralDCLIT");
    
    public GeneralDCLIT(final AssertionParameterizedArray parameterizedArray) {
        super(parameterizedArray);
    }
    
    @Parameters(name = "{0}")
    public static Collection<ParameterizedArray> getParameters() {
        return ParameterizedArrayFactory.getAssertionParameterized(SQLCommandType.DCL)
                .stream()
                .peek(each -> each.setCompose(COMPOSED_CONTAINER_MANAGER.getComposedContainer(each)))
                .collect(Collectors.toList());
    }
    
    @Test
    public void assertExecuteUpdate() throws SQLException, ParseException {
        String sql = getSQL();
        try (Connection connection = getTargetDataSource().getConnection()) {
            if (SQLExecuteType.Literal == getSqlExecuteType()) {
                executeUpdateForStatement(connection, sql);
            } else {
                executeUpdateForPrepareStatement(connection, sql);
            }
        }
    }
    
    @Test
    public void assertExecute() throws SQLException, ParseException {
        String sql = getSQL();
        try (Connection connection = getTargetDataSource().getConnection()) {
            if (SQLExecuteType.Literal == getSqlExecuteType()) {
                executeForStatement(connection, sql);
            } else {
                executeForPrepareStatement(connection, sql);
            }
        }
    }
}
