package adt.impl;

import peer.core.ActorMessageType;

public class DistributedMapBucketGetRequest extends DistributedMapBucketRequest {
    
    public DistributedMapBucketGetRequest(int requestNum, int index, Object k) {
        super(requestNum, index, k, ActorMessageType.DistributedMapBucketGetRequest);
    }
}
