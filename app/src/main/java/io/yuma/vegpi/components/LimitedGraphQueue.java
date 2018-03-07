package io.yuma.vegpi.components;

import java.util.LinkedList;

/**
 * Created by bkennedy on 2/26/18.
 */

public class LimitedGraphQueue<E> extends LinkedList<E> {
    private int limit;

    public LimitedGraphQueue(int limit) {
        this.limit = limit;
    }

    @Override
    public boolean add(E o) {
        super.add(o);
        while (size() > limit) { super.remove(); }
        return true;
    }
}
