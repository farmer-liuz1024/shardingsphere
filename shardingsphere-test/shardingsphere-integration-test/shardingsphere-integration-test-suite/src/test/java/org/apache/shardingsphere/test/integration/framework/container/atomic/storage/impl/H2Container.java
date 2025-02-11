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

package org.apache.shardingsphere.test.integration.framework.container.atomic.storage.impl;

import lombok.SneakyThrows;
import org.apache.shardingsphere.infra.database.type.dialect.H2DatabaseType;
import org.apache.shardingsphere.test.integration.env.DataSourceEnvironment;
import org.apache.shardingsphere.test.integration.env.EnvironmentPath;
import org.apache.shardingsphere.test.integration.framework.container.atomic.storage.StorageContainer;
import org.apache.shardingsphere.test.integration.framework.param.model.ParameterizedArray;
import org.h2.tools.RunScript;

import javax.sql.DataSource;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map.Entry;
import java.util.Objects;

/**
 * H2 container.
 */
public final class H2Container extends StorageContainer {
    
    public H2Container(final ParameterizedArray parameterizedArray) {
        super("h2-embedded", "h2:fake", new H2DatabaseType(), true, parameterizedArray);
    }
    
    @Override
    @SneakyThrows({IOException.class, SQLException.class})
    protected void execute() {
        super.execute();
        File file = new File(EnvironmentPath.getInitSQLFile(getDatabaseType(), getParameterizedArray().getScenario()));
        for (Entry<String, DataSource> each : getDataSourceMap().entrySet()) {
            String databaseFileName = "init-" + each.getKey() + ".sql";
            boolean sqlFileExist = EnvironmentPath.checkSQLFileExist(getDatabaseType(), getParameterizedArray().getScenario(), databaseFileName);
            try (Connection connection = each.getValue().getConnection(); FileReader reader = new FileReader(file)) {
                RunScript.execute(connection, reader);
                if (sqlFileExist) {
                    executeDatabaseFile(connection, databaseFileName);
                }
            }
        }
    }
    
    private void executeDatabaseFile(final Connection connection, final String databaseFileName) throws IOException, SQLException {
        File databaseFile = new File(EnvironmentPath.getInitSQLFile(getDatabaseType(), getParameterizedArray().getScenario(), databaseFileName));
        try (FileReader databaseFileReader = new FileReader(databaseFile)) {
            RunScript.execute(connection, databaseFileReader);
        }
    }
    
    @Override
    public boolean isHealthy() {
        return true;
    }
    
    @Override
    protected String getUrl(final String dataSourceName) {
        return DataSourceEnvironment.getURL("H2", null, 0, Objects.isNull(dataSourceName) ? "test_db" : dataSourceName);
    }
    
    @Override
    protected int getPort() {
        return 0;
    }
    
    @Override
    protected String getUsername() {
        return "sa";
    }
    
    @Override
    protected String getPassword() {
        return "";
    }
    
}
