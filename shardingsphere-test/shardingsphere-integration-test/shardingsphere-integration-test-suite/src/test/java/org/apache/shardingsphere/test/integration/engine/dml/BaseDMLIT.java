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

package org.apache.shardingsphere.test.integration.engine.dml;

import org.apache.shardingsphere.infra.database.type.dialect.PostgreSQLDatabaseType;
import org.apache.shardingsphere.infra.datanode.DataNode;
import org.apache.shardingsphere.sharding.support.InlineExpressionParser;
import org.apache.shardingsphere.test.integration.cases.dataset.metadata.DataSetColumn;
import org.apache.shardingsphere.test.integration.cases.dataset.metadata.DataSetMetaData;
import org.apache.shardingsphere.test.integration.cases.dataset.row.DataSetRow;
import org.apache.shardingsphere.test.integration.engine.SingleITCase;
import org.apache.shardingsphere.test.integration.env.EnvironmentPath;
import org.apache.shardingsphere.test.integration.env.dataset.DataSetEnvironmentManager;
import org.apache.shardingsphere.test.integration.framework.container.compose.mode.ClusterComposedContainer;
import org.apache.shardingsphere.test.integration.framework.param.model.AssertionParameterizedArray;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public abstract class BaseDMLIT extends SingleITCase {
    
    private DataSetEnvironmentManager dataSetEnvironmentManager;
    
    public BaseDMLIT(final AssertionParameterizedArray parameterizedArray) {
        super(parameterizedArray);
    }
    
    @Override
    public final void init() throws Exception {
        super.init();
        dataSetEnvironmentManager = new DataSetEnvironmentManager(
                EnvironmentPath.getDataSetFile(getScenario()),
                getStorageContainer().getDataSourceMap()
        );
        dataSetEnvironmentManager.fillData();
    }
    
    @Override
    public final void tearDown() throws Exception {
        dataSetEnvironmentManager.clearData();
        super.tearDown();
    }
    
    protected final void assertDataSet(final int actualUpdateCount) throws SQLException {
        assertThat("Only support single table for DML.", getDataSet().getMetaDataList().size(), is(1));
        assertThat(actualUpdateCount, is(getDataSet().getUpdateCount()));
        DataSetMetaData expectedDataSetMetaData = getDataSet().getMetaDataList().get(0);
        for (String each : new InlineExpressionParser(expectedDataSetMetaData.getDataNodes()).splitAndEvaluate()) {
            DataNode dataNode = new DataNode(each);
            DataSource dataSource = getComposedContainer() instanceof ClusterComposedContainer
                    ? getDataSourceForReader() : getStorageContainer().getDataSourceMap().get(dataNode.getDataSourceName());
            try (
                    Connection connection = dataSource.getConnection();
                    PreparedStatement preparedStatement = connection.prepareStatement(generateFetchActualDataSQL(dataNode))) {
                assertDataSet(preparedStatement, expectedDataSetMetaData, getDataSet().findRows(dataNode));
            }
        }
    }
    
    private void assertDataSet(final PreparedStatement actualPreparedStatement, final DataSetMetaData expectedDataSetMetaData, final List<DataSetRow> expectedDataSetRows) throws SQLException {
        try (ResultSet actualResultSet = actualPreparedStatement.executeQuery()) {
            assertMetaData(actualResultSet.getMetaData(), expectedDataSetMetaData.getColumns());
            assertRows(actualResultSet, expectedDataSetRows);
        }
    }
    
    private String generateFetchActualDataSQL(final DataNode dataNode) throws SQLException {
        if (getStorageContainer().getDatabaseType() instanceof PostgreSQLDatabaseType) {
            String primaryKeyColumnName = getPrimaryKeyColumnNameForPostgreSQL(dataNode);
            return String.format("SELECT * FROM %s ORDER BY %s ASC", dataNode.getTableName(), primaryKeyColumnName);
        }
        return String.format("SELECT * FROM %s", dataNode.getTableName());
    }
    
    private String getPrimaryKeyColumnNameForPostgreSQL(final DataNode dataNode) throws SQLException {
        String sql = String.format("SELECT a.attname, format_type(a.atttypid, a.atttypmod) AS data_type "
                + "FROM pg_index i JOIN pg_attribute a ON a.attrelid = i.indrelid AND a.attnum = ANY(i.indkey) WHERE i.indrelid = '%s'::regclass AND i.indisprimary", dataNode.getTableName());
        DataSource dataSource = getComposedContainer() instanceof ClusterComposedContainer
                ? getDataSourceForReader() : getStorageContainer().getDataSourceMap().get(dataNode.getDataSourceName());
        try (
                Connection connection = dataSource.getConnection();
                Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery(sql)) {
            if (resultSet.next()) {
                return resultSet.getString("attname");
            }
            throw new SQLException(String.format("Can not get primary key of `%s`", dataNode.getTableName()));
        }
    }
    
    private void assertMetaData(final ResultSetMetaData actual, final Collection<DataSetColumn> expected) throws SQLException {
        assertThat(actual.getColumnCount(), is(expected.size()));
        int index = 1;
        for (DataSetColumn each : expected) {
            assertThat(actual.getColumnLabel(index++), is(each.getName()));
        }
    }
    
    private void assertRows(final ResultSet actual, final List<DataSetRow> expected) throws SQLException {
        int rowCount = 0;
        while (actual.next()) {
            int columnIndex = 1;
            for (String each : expected.get(rowCount).splitValues(",")) {
                assertValue(actual, columnIndex, each);
                columnIndex++;
            }
            rowCount++;
        }
        assertThat("Size of actual result set is different with size of expected dat set rows.", rowCount, is(expected.size()));
    }
    
    private void assertValue(final ResultSet actual, final int columnIndex, final String expected) throws SQLException {
        if (Types.DATE == actual.getMetaData().getColumnType(columnIndex)) {
            if (!NOT_VERIFY_FLAG.equals(expected)) {
                assertThat(new SimpleDateFormat("yyyy-MM-dd").format(actual.getDate(columnIndex)), is(expected));
            }
        } else {
            assertThat(String.valueOf(actual.getObject(columnIndex)), is(expected));
        }
    }
}
