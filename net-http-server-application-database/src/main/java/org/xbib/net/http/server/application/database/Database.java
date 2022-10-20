package org.xbib.net.http.server.application.database;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public interface Database extends AutoCloseable {

    Table getPagedRows(Table table);

    long getRowsCount(Table table);

    long countRows(String statement, Map<String, Object> params);

    Table getSingleRow(String statement, Map<String, Object> params);

    Table getUnlimitedRows(String statement, Map<String, Object> params);

    Table getLimitedRows(String statement, Map<String, Object> params,
                         int limit, int fetchSize, int timeoutSeconds);

    void insert(String statement, Map<String, Object> params);

    void insert(String statement, List<Map<String, Object>> params);

    void update(String statement, Map<String, Object> params);

    void update(String statement, List<Map<String, Object>> params);

    void upsert(String insertStatement, String updateStatement, Map<String, Object> params);

    void delete(String statement, Map<String, Object> params);

    void delete(String statement, List<Map<String, Object>> params);

    void close() throws IOException;
}
