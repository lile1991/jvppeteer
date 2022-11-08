package com.ruiyun.jvppeteer.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

public class ListExt<T> extends ArrayList<T> {
    public final static ListExt<?> EMPTY_LIST = new ListExt<>(Collections.emptyList());

    public ListExt(Collection<T> collection) {
        super(collection);
    }

    public T first() {
        return isEmpty() ? null : get(0);
    }
    public T last() {
        return isEmpty() ? null : get(size() - 1);
    }

    public static <T> ListExt<T> emptyList() {
        return (ListExt<T>) EMPTY_LIST;
    }
}
