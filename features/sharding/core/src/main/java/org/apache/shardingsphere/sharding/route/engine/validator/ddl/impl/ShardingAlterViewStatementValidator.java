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

package org.apache.shardingsphere.sharding.route.engine.validator.ddl.impl;

import org.apache.shardingsphere.infra.binder.context.statement.SQLStatementContext;
import org.apache.shardingsphere.infra.config.props.ConfigurationProperties;
import org.apache.shardingsphere.infra.exception.core.ShardingSpherePreconditions;
import org.apache.shardingsphere.infra.hint.HintValueContext;
import org.apache.shardingsphere.infra.metadata.database.ShardingSphereDatabase;
import org.apache.shardingsphere.infra.route.context.RouteContext;
import org.apache.shardingsphere.sharding.exception.metadata.EngagedViewException;
import org.apache.shardingsphere.sharding.exception.syntax.RenamedViewWithoutSameConfigurationException;
import org.apache.shardingsphere.sharding.route.engine.validator.ddl.ShardingDDLStatementValidator;
import org.apache.shardingsphere.sharding.rule.ShardingRule;
import org.apache.shardingsphere.sql.parser.statement.core.segment.generic.table.SimpleTableSegment;
import org.apache.shardingsphere.sql.parser.statement.core.statement.ddl.AlterViewStatement;
import org.apache.shardingsphere.sql.parser.statement.core.statement.dml.SelectStatement;
import org.apache.shardingsphere.sql.parser.statement.core.extractor.TableExtractor;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Sharding alter view statement validator.
 */
public final class ShardingAlterViewStatementValidator extends ShardingDDLStatementValidator {
    
    @Override
    public void preValidate(final ShardingRule shardingRule, final SQLStatementContext sqlStatementContext, final HintValueContext hintValueContext,
                            final List<Object> params, final ShardingSphereDatabase database, final ConfigurationProperties props) {
        AlterViewStatement alterViewStatement = (AlterViewStatement) sqlStatementContext.getSqlStatement();
        Optional<SelectStatement> selectStatement = alterViewStatement.getSelectStatement();
        String originView = alterViewStatement.getView().getTableName().getIdentifier().getValue();
        selectStatement.ifPresent(optional -> validateAlterViewShardingTables(shardingRule, optional, originView));
        Optional<SimpleTableSegment> renamedView = alterViewStatement.getRenameView();
        if (renamedView.isPresent()) {
            String targetView = renamedView.get().getTableName().getIdentifier().getValue();
            validateBroadcastShardingView(shardingRule, originView, targetView);
        }
    }
    
    private void validateAlterViewShardingTables(final ShardingRule shardingRule, final SelectStatement selectStatement, final String viewName) {
        TableExtractor extractor = new TableExtractor();
        extractor.extractTablesFromSelect(selectStatement);
        if (isShardingTablesNotBindingWithView(extractor.getRewriteTables(), shardingRule, viewName)) {
            throw new EngagedViewException("sharding");
        }
    }
    
    private void validateBroadcastShardingView(final ShardingRule shardingRule, final String originView, final String targetView) {
        ShardingSpherePreconditions.checkState(!shardingRule.isShardingTable(originView) && !shardingRule.isShardingTable(targetView)
                || shardingRule.isAllBindingTables(Arrays.asList(originView, targetView)), () -> new RenamedViewWithoutSameConfigurationException(originView, targetView));
    }
    
    @Override
    public void postValidate(final ShardingRule shardingRule, final SQLStatementContext sqlStatementContext, final HintValueContext hintValueContext, final List<Object> params,
                             final ShardingSphereDatabase database, final ConfigurationProperties props, final RouteContext routeContext) {
    }
}
