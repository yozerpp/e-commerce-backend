package com.yusuf.simpleecommercesite.network.dtos;

import java.util.ArrayList;
import java.util.List;

public class SearchResult<T> {
    private int count;
    private List<T> data;
    public SearchResult() {
        data = new ArrayList<T>();
    }
    public SearchResult(int count, List<T> data) {
        this.count = count;;
        this.data = data;
    }
    public int getCount() {
        return count;
    }

    public List<T> getData() {
        return data;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public void setData(List<T> data) {
        this.data = data;
    }
}
