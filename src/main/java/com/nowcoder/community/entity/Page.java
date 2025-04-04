package com.nowcoder.community.entity;

public class Page {

    private int current = 1;
    private int limit = 10;
    private int rows;
    private String path;

    public int getCurrent() {
        return current;
    }

    public void setCurrent(int current) {
        if(current >= 1)
            this.current = current;
    }

    public int getLimit() {
        return limit;
    }

    public void setLimit(int limit) {
        if(limit >= 1 && limit <=50)
            this.limit = limit;
    }

    public int getRows() {
        return rows;
    }

    public void setRows(int row) {
        this.rows = row;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public int getTotal(){
        return rows % limit == 0 ? rows / limit : rows / limit + 1;
    }

    public int getOffset(){
        return (current - 1) * limit;
    }

    public int getFrom(){
        return Math.max(current - 2, 1);
    }

    public int getTo(){
        return  Math.min(current + 2, getTotal());
    }
}
