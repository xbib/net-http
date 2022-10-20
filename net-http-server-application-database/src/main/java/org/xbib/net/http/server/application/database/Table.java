package org.xbib.net.http.server.application.database;

import org.xbib.datastructures.tiny.TinyMap;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@SuppressWarnings("serial")
public class Table extends ArrayList<List<Object>> implements List<List<Object>> {

    private String statement;

    private Map<String, Object> params;

    private String search;

    private LocalDateTime from;

    private LocalDateTime to;

    private Integer offset;

    private Integer size;

    private List<String> where;

    private String whereClause;

    private String groupByClause;

    private final List<Map.Entry<String, Boolean>> sort = new ArrayList<>();

    private long total;

    public void setStatement(String statement) {
        this.statement = statement;
    }

    public String getStatement() {
        return statement;
    }

    public void setParams(Map<String, Object> params) {
        this.params = params;
    }

    public Map<String, Object> getParams() {
        return params;
    }

    public void setSearch(String search) {
        this.search = search;
    }

    public String getSearch() {
        return search;
    }

    public void setFromDate(LocalDateTime from) {
        this.from = from;
    }

    public LocalDateTime getFromDate() {
        return from;
    }

    public void setToDate(LocalDateTime to) {
        this.to = to;
    }

    public LocalDateTime getToDate() {
        return to;
    }

    public void setOffset(Integer offset) {
        this.offset = offset;
    }

    public Integer getOffset() {
        return offset;
    }

    public void setSize(Integer size) {
        this.size = size;
    }

    public Integer getSize() {
        return size;
    }

    public void setWhere(List<String> where) {
        this.where = where;
    }

    public List<String> getWhere() {
        return where;
    }

    public void setWhereClause(String whereClause) {
        this.whereClause = whereClause;
    }

    public String getWhereClause() {
        return whereClause;
    }

    public void setGroupByClause(String groupByClause) {
        this.groupByClause = groupByClause;
    }

    public String getGroupByClause() {
        return groupByClause;
    }

    public void addSort(String sort) {
        addSort(sort, true);
    }

    public void addSort(String sort, Boolean asc) {
        this.sort.add(Map.entry(sort, asc));
    }

    public String getSort() {
        return sort.stream().map(e -> "\"" + e.getKey() + "\"" + " " + (e.getValue() ? "asc" : "desc"))
                .collect(Collectors.joining(","));
    }

    public List<String> getColumnNames() {
        return get(0).stream().map(Object::toString).collect(Collectors.toList());
    }

    public int getColumn(String columnName) {
        return get(0).indexOf(columnName);
    }

    public List<String> getColumnClassNames() {
        return get(1).stream().map(Object::toString).collect(Collectors.toList());
    }

    public String getColumnName(int i) {
        return (String) get(0).get(i);
    }

    public String getColumnClassName(int i) {
        return (String) get(1).get(i);
    }

    public int getRowCount() {
        return size() - 2;
    }

    public int getColumnCount() {
        return get(0).size();
    }

    public List<Object> getRow(int i) {
        return get(i + 2);
    }

    public Map<String, Object> getRowAsMap(int i) {
        TinyMap.Builder<String, Object> map = TinyMap.builder();
        List<Object> row = getRow(i);
        for (int c = 0; c < getColumnCount(); c++) {
            map.put(getColumnName(c), row.get(c));
        }
        return map.build();
    }

    public Object getObject(int row, int col) {
        return get(row + 2).get(col);
    }

    public Object getObject(int row, String columnName) {
        int col = getColumn(columnName);
        return col >= 0 ? get(row + 2).get(col) : null;
    }

    @SuppressWarnings("unchecked")
    public <T> T getValue(int row, int col) {
        return (T) getObject(row, col);
    }

    @SuppressWarnings("unchecked")
    public <T> T getValue(int row, String columnName) {
        return (T) getObject(row, columnName);
    }

    public void setTotal(long total) {
        this.total = total;
    }

    public long getTotal() {
        return total;
    }

    @Override
    public boolean isEmpty() {
        return super.isEmpty() || size() <= 2;
    }
}
