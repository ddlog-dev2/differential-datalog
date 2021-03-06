/*
 * Copyright 2018-2020 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2
 */

package com.vmware.ddlog;

import com.facebook.presto.sql.tree.AstVisitor;
import com.facebook.presto.sql.tree.ColumnDefinition;
import com.facebook.presto.sql.tree.CreateTable;
import com.facebook.presto.sql.tree.CreateView;
import com.facebook.presto.sql.tree.TableElement;

import java.util.ArrayList;
import java.util.List;

class TranslateCreateTableDialect extends AstVisitor<String, String> {
    @Override
    protected String visitCreateTable(final CreateTable node, final String sql) {
        final List<String> primaryKeyColumns = new ArrayList<>();
        final List<String> columnsToCreate = new ArrayList<>();
        for (final TableElement element : node.getElements()) {
            if (element instanceof ColumnDefinition) {
                final ColumnDefinition cd = (ColumnDefinition) element;
                if (cd.getProperties().size() == 1) {
                    final String propertyName = cd.getProperties().get(0).getName().getValue();
                    final String propertyValue = cd.getProperties().get(0).getValue().toString();
                    if (propertyName.equals("primary_key") && propertyValue.equals("true")) {
                        primaryKeyColumns.add(cd.getName().getValue());
                    } else {
                        throw new RuntimeException(String.format("Unsupported property %s in sql: %s",
                                propertyName, sql));
                    }
                }
                if (cd.getProperties().size() > 1) {
                    throw new RuntimeException(String.format("Unsupported properties %s in sql: %s",
                            cd.getProperties(), sql));
                }
                columnsToCreate.add(String.format("%s %s", cd.getName().getValue(), cd.getType()));
            }
        }

        final String columns = String.join(", ", columnsToCreate);
        if (primaryKeyColumns.size() > 0) {
            final String primaryKeyColumnsStr = String.join(", ", primaryKeyColumns);
            return String.format("create table %s (%s, primary key (%s))", node.getName().toString(), columns,
                    primaryKeyColumnsStr);
        } else {
            return String.format("create table %s (%s)", node.getName().toString(), columns);
        }
    }

    @Override
    protected String visitCreateView(final CreateView node, final String sql) {
        return sql;
    }
}
