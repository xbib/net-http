package org.xbib.net.http.server.application.database;

import org.xbib.jdbc.query.DatabaseException;
import org.xbib.jdbc.query.Rows;
import org.xbib.jdbc.query.SqlInsert;
import org.xbib.jdbc.query.SqlSelect;
import org.xbib.jdbc.query.SqlUpdate;

import java.math.BigDecimal;
import java.sql.ResultSetMetaData;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class BaseDatabase implements Database {

    private static final Logger logger = Logger.getLogger(BaseDatabase.class.getName());

    private final org.xbib.jdbc.query.Database db;

    private final int fetchSize;

    private final int timeoutSeconds;

    public BaseDatabase(org.xbib.jdbc.query.Database db, int fetchSize, int timeoutSeconds) throws Exception {
        this.db = db;
        this.fetchSize = fetchSize;
        this.timeoutSeconds = timeoutSeconds;
    }

    @Override
    public Table getPagedRows(Table table) {
        if (table == null) {
            return table;
        }
        Map<String, Object> params = table.getParams() != null ?
                new HashMap<>(table.getParams()) : new HashMap<>();
        if (table.getOffset() != null && table.getSize() != null) {
            params.put("offset", table.getOffset());
            params.put("limit", table.getSize());
        }
        String where = table.getWhereClause() != null ? table.getWhereClause() : "";
        String groupby = table.getGroupByClause() != null ? table.getGroupByClause() : "";
        String orderby = !table.getSort().isEmpty() ? "order by " + table.getSort() : "";
        String statement = table.getStatement() + " " + where + " " + groupby + " " + orderby;
        if (table.getOffset() != null && table.getSize() != null) {
            statement = statement + " offset :offset rows fetch next :limit rows only";
        }
        return getUnlimitedRows(statement, params);
    }

    @Override
    public long getRowsCount(Table table) {
        Map<String, Object> params = new HashMap<>(table.getParams());
        String statement = table.getStatement() + " " + table.getWhereClause();
        return countRows(statement, params);
    }

    @Override
    public long countRows(String statement, Map<String, Object> params) {
        String countStatament = "select count(*) as \"cnt\" from (" + statement + ")";
        Table table = getSingleRow(countStatament, params);
        if (!table.isEmpty()) {
            BigDecimal bigDecimal = table.getValue(0,"cnt");
            return bigDecimal.longValue();
        } else {
            return -1L;
        }
    }

    @Override
    public Table getSingleRow(String statement, Map<String, Object> params) {
        return getLimitedRows(statement, params, 1, fetchSize, timeoutSeconds);
    }

    @Override
    public Table getUnlimitedRows(String statement, Map<String, Object> params) {
        return getLimitedRows(statement, params, 0, fetchSize, timeoutSeconds);
    }

    @Override
    public Table getLimitedRows(String statement, Map<String, Object> params,
                                int limit, int fetchSize, int timeoutSeconds) {
        SqlSelect sql = db.toSelect(statement).fetchSize(fetchSize).withTimeoutSeconds(timeoutSeconds);
        selectParams(sql, params);
        Table table = new Table();
        sql.query(rows -> {
            ResultSetMetaData md = rows.getMetadata();
            List<Object> columnNames = new ArrayList<>();
            List<Object> classNames = new ArrayList<>();
            for (int i = 1; i <= md.getColumnCount(); i++) {
                columnNames.add(md.getColumnName(i).toLowerCase(Locale.ROOT));
                classNames.add(md.getColumnClassName(i));
            }
            table.add(columnNames);
            table.add(classNames);
            int i = 0;
            while (rows.next() && (limit <= 0 || i++ < limit)) {
                table.add(getRow(rows, classNames));
            }
            table.setTotal(rows.rowCount());
            return true;
        });
        return table;
    }

    @Override
    public void insert(String statement, Map<String, Object> params) {
        SqlInsert sql = db.toInsert(statement);
        insertParams(sql, params);
        sql.insert(1);
    }

    @Override
    public void insert(String statement, List<Map<String, Object>> params) {
        SqlInsert sqlInsert = db.toInsert(statement);
        for (Map<String, Object> param : params) {
            insertParams(sqlInsert, param);
            sqlInsert.batch();
        }
        sqlInsert.insertBatch();
    }

    @Override
    public void update(String statement, Map<String, Object> params) {
        SqlUpdate sql = db.toUpdate(statement);
        updateParams(sql, params);
        sql.update();
    }

    @Override
    public void update(String statement, List<Map<String, Object>> params) {
        SqlUpdate sqlUpdate = db.toUpdate(statement);
        for (Map<String, Object> param : params) {
            updateParams(sqlUpdate, param);
            sqlUpdate.update();
        }
    }

    @Override
    public void upsert(String insertStatement, String updateStatement, Map<String, Object> params) {
        // try insert then update if error
        try {
            SqlInsert sql = db.toInsert(insertStatement);
            insertParams(sql, params);
            sql.insert(1);
        } catch (Exception e) {
            logger.log(Level.WARNING, e.getMessage(), e);
            SqlUpdate sql = db.toUpdate(updateStatement);
            updateParams(sql, params);
            sql.update();
        }
    }

    @Override
    public void delete(String statement, Map<String, Object> params) {
        SqlUpdate sql = db.toDelete(statement);
        updateParams(sql, params);
        sql.update();
    }

    @Override
    public void delete(String statement, List<Map<String, Object>> params) {
        SqlUpdate sqlUpdate = db.toDelete(statement);
        for (Map<String, Object> param : params) {
            updateParams(sqlUpdate, param);
            sqlUpdate.update();
        }
    }

    private void selectParams(SqlSelect sql, Map<String, Object> params) {
        if (params == null) {
            return;
        }
        params.forEach((k, v) -> {
            if (v instanceof String) {
                sql.argString(k, (String) v);
            } else if (v instanceof Integer) {
                sql.argInteger(k, (Integer) v);
            } else if (v instanceof Long) {
                sql.argLong(k, (Long) v);
            } else if (v instanceof Boolean) {
                sql.argBoolean(k, (Boolean) v);
            } else if (v instanceof LocalDate) {
                sql.argLocalDate(k, (LocalDate) v);
            } else if (v instanceof LocalDateTime) {
                sql.argDate(k, (LocalDateTime) v);
            } else {
                throw new DatabaseException("unknown type for param: " + (v != null ? v.getClass() : "null"));
            }
        });
    }

    private void insertParams(SqlInsert sql, Map<String, Object> params) {
        if (params == null) {
            return;
        }
        params.forEach((k, v) -> {
            if (v instanceof String) {
                sql.argString(k, (String) v);
            } else if (v instanceof Integer) {
                sql.argInteger(k, (Integer) v);
            } else if (v instanceof Long) {
                sql.argLong(k, (Long) v);
            } else if (v instanceof Boolean) {
                sql.argBoolean(k, (Boolean) v);
            } else if (v instanceof LocalDate) {
                sql.argLocalDate(k, (LocalDate) v);
            } else if (v instanceof LocalDateTime) {
                sql.argDate(k, (LocalDateTime) v);
            } else {
                throw new DatabaseException("unknown type for param: " + (v != null ? v.getClass() : "null"));
            }
        });
    }

    private void updateParams(SqlUpdate sql, Map<String, Object> params) {
        if (params == null) {
            return;
        }
        params.forEach((k, v) -> {
            if (v instanceof String) {
                sql.argString(k, (String) v);
            } else if (v instanceof Integer) {
                sql.argInteger(k, (Integer) v);
            } else if (v instanceof Long) {
                sql.argLong(k, (Long) v);
            } else if (v instanceof Boolean) {
                sql.argBoolean(k, (Boolean) v);
            } else if (v instanceof LocalDate) {
                sql.argLocalDate(k, (LocalDate) v);
            } else if (v instanceof LocalDateTime) {
                sql.argDate(k, (LocalDateTime) v);
            } else {
                throw new DatabaseException("unknown type for param: " + (v != null ? v.getClass() : "null"));
            }
        });
    }

    private List<Object> getRow(Rows rows, List<Object> classNames) {
        List<Object> row = new ArrayList<>();
        for (int i = 0; i < classNames.size(); i++) {
            String className = classNames.get(i).toString();
            switch (className) {
                case "java.lang.String":
                    row.add(rows.getStringOrEmpty(i + 1));
                    break;
                case "java.lang.Integer":
                    row.add(rows.getIntegerOrNull(i + 1));
                    break;
                case "java.lang.Long":
                    row.add(rows.getLongOrNull(i + 1));
                    break;
                case "java.lang.Boolean":
                    row.add(rows.getBooleanOrFalse(i + 1));
                    break;
                case "java.sql.Clob":
                case "oracle.jdbc.OracleClob":
                    row.add(rows.getClobStringOrEmpty(i + 1));
                    break;
                case "java.sql.Date":
                    row.add(rows.getLocalDateOrNull(i + 1));
                    break;
                case "java.sql.Timestamp":
                case "oracle.sql.TIMESTAMP":
                    row.add(rows.getLocalDateTimeOrNull(i + 1));
                    break;
                case "java.math.BigDecimal":
                    row.add(rows.getBigDecimalOrNull(i + 1));
                    break;
                default:
                    throw new DatabaseException("unexpected column class name: " + className);
            }
        }
        return row;
    }
}
