package adt.impl;

import peer.core.ActorMessageType;

/**
 * Requests a bucket to check if it contains a key
 *
 */
public class DistributedMapBucketContainsRequest extends DistributedMapBucketRequest {
    
    public DistributedMapBucketContainsRequest(int requestNum, int index, Object k) {
        super(requestNum, index, k, ActorMessageType.DistributedMapBucketContainsRequest);
    }
}
