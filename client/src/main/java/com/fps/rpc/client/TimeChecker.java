package com.fps.rpc.client;

import java.io.Closeable;

/**
 * Created by fperez on 7/9/16.
 */
public interface TimeChecker extends Closeable {

    void checkTime();

    void checkTimeAsync() throws Exception;

}
