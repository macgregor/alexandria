package com.github.macgregor.alexandria.remotes;

import org.junit.Test;

public abstract class RemoteTest {

    private Remote remote;

    public RemoteTest(Remote remote){
        this.remote = remote;
    }

    @Test
    public void testSyncMetadata404(){

    }
}
