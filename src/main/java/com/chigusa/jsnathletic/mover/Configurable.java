package com.chigusa.jsnathletic.mover;

/** 設定画面から変えられる仕掛け */
public interface Configurable {
    void apply(String key, double value);
}
