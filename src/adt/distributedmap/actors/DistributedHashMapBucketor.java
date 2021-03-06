package adt.distributedmap.actors;

import adt.distributedmap.messages.DistributedHashMapBucketInit;
import adt.distributedmap.messages.DistributedMapAdditionResponse;
import adt.distributedmap.messages.DistributedMapBucketAdditionRequest;
import adt.distributedmap.messages.DistributedMapBucketContainsRequest;
import adt.distributedmap.messages.DistributedMapBucketGetRequest;
import adt.distributedmap.messages.DistributedMapBucketRemoveRequest;
import adt.distributedmap.messages.DistributedMapContainsResponse;
import adt.distributedmap.messages.DistributedMapGetResponse;
import adt.distributedmap.messages.DistributedMapIterationRequest;
import adt.distributedmap.messages.DistributedMapIterationResponse;
import adt.distributedmap.messages.DistributedMapRefactorAddRequest;
import adt.distributedmap.messages.DistributedMapRefactorAddResponse;
import adt.distributedmap.messages.DistributedMapRefactorGetRequest;
import adt.distributedmap.messages.DistributedMapRefactorGetResponse;
import adt.distributedmap.messages.DistributedMapRemoveResponse;
import akka.actor.ActorRef;
import peer.frame.actors.PeerToPeerActor;
import peer.frame.messages.PeerToPeerActorInit;

/**
 * Actor that contains a bucket for a Distributed Hash Map
 *
 */
public class DistributedHashMapBucketor extends PeerToPeerActor {
    private ActorRef owner;
    private Class<?> kClass;
    private int bucketNum;
    private int bucketSize;
    private Object[] keyArray;
    private Object[] valueArray;
    private int entryCount;
    private Object availableSlot;
    
    /**
     * Actor message processing
     */
    @Override
    public void onReceive(Object message) {
        if (message instanceof PeerToPeerActorInit) {
            PeerToPeerActorInit init = (PeerToPeerActorInit) message;
            this.initialisePeerToPeerActor(init);
        }
        else if (message instanceof DistributedHashMapBucketInit) {
            DistributedHashMapBucketInit init = (DistributedHashMapBucketInit) message;
            this.initialise(init);
        }
        else if (message instanceof DistributedMapBucketAdditionRequest) {
            DistributedMapBucketAdditionRequest additionRequest = (DistributedMapBucketAdditionRequest) message;
            this.processAdditionRequest(additionRequest);
        }
        else if (message instanceof DistributedMapBucketContainsRequest) {
            DistributedMapBucketContainsRequest containsRequest = (DistributedMapBucketContainsRequest) message;
            this.processContainsRequest(containsRequest);
        }
        else if (message instanceof DistributedMapBucketGetRequest) {
            DistributedMapBucketGetRequest getRequest = (DistributedMapBucketGetRequest) message;
            this.processGetRequest(getRequest);
        }
        else if (message instanceof DistributedMapBucketRemoveRequest) {
            DistributedMapBucketRemoveRequest removeRequest = (DistributedMapBucketRemoveRequest) message;
            this.processRemoveRequest(removeRequest);
        }
        else if (message instanceof DistributedMapRefactorGetRequest) {
            DistributedMapRefactorGetRequest refactorGetRequest = (DistributedMapRefactorGetRequest) message;
            this.processRefactorGetRequest(refactorGetRequest);
        }
        else if (message instanceof DistributedMapRefactorAddRequest) {
            DistributedMapRefactorAddRequest refactorAddRequest = (DistributedMapRefactorAddRequest) message;
            this.processRefactorAddRequest(refactorAddRequest);
        }
        else if (message instanceof DistributedMapIterationRequest) {
            DistributedMapIterationRequest request = (DistributedMapIterationRequest) message;
            this.processIterationRequest(request);
        }
    }
    
    /**
     * Initialise the Distributed Hash Map Bucket
     * @param init
     */
    protected void initialise(DistributedHashMapBucketInit init) {
        this.owner = init.getOwner();
        this.kClass = init.getKeyClass();
        this.bucketNum = init.getBucketNum();
        this.bucketSize = init.getBucketSize();
        this.keyArray = new Object[this.bucketSize];
        this.valueArray = new Object[this.bucketSize];
        this.entryCount = 0;
        this.availableSlot = new Object();
    }
    
    /**
     * Attempts to place it at the bucketIndex if empty
     * Uses linear probing to find another part of the bucket that is empty if bucketIndex is full
     * If no empty space is available then it calls for a refactor and responses with an addition failure response
     * @param additionRequest
     */
    protected void processAdditionRequest(DistributedMapBucketAdditionRequest additionRequest) {
        boolean success = false;
        Object value = null;
        if (this.bucketSize > this.entryCount) {
            int bucketIndex = additionRequest.getIndex() % this.bucketSize;
            Object key = this.keyArray[bucketIndex];
            if (key == null || key == this.availableSlot) {
                this.keyArray[bucketIndex] = additionRequest.getKey();
                this.valueArray[bucketIndex] = additionRequest.getValue();
                this.entryCount++;
                success = true;
            }
            else if (equals(key, additionRequest.getKey())) {
                value = this.valueArray[bucketIndex];
                this.valueArray[bucketIndex] = additionRequest.getValue();
                success = true;
            }
            else {
                for (int i = 1; i < this.bucketSize && !success; i++) {
                    int linearProbeIndex = (bucketIndex + i) % this.bucketSize;
                    key = this.keyArray[linearProbeIndex];
                    if (key == null || key == this.availableSlot) {
                        this.keyArray[linearProbeIndex] = additionRequest.getKey();
                        this.valueArray[linearProbeIndex] = additionRequest.getValue();
                        this.entryCount++;
                        success = true;
                    }
                    else if (equals(key, additionRequest.getKey())) {
                        value = this.valueArray[linearProbeIndex];
                        this.valueArray[linearProbeIndex] = additionRequest.getValue();
                        success = true;
                    }
                    else {
                        success = false;
                    }
                }
            }
        }
        else {
            success = false;
            value = additionRequest.getValue();
        }
        int requestNum = additionRequest.getRequestNum();
        DistributedMapAdditionResponse response = new DistributedMapAdditionResponse(requestNum, this.bucketNum, success, additionRequest.getKey(), value);
        this.owner.tell(response, getSelf());
    }
    
    /**
     * Checks bucket if it contains a key
     * If another key is occupying the designated bucketIndex it linearly probes to check if it's contained elsewhere
     * Linearly probes up until an empty entry missing a key value pair
     * Assumes no keys to check exist beyond this empty entry as linear probing in addition would have filled it first
     * @param containsRequest
     */
    protected void processContainsRequest(DistributedMapBucketContainsRequest containsRequest) {
        boolean success = false;
        boolean contains = false;
        int bucketIndex = containsRequest.getIndex() % this.bucketSize;
        Object key = this.keyArray[bucketIndex];
        if (key == null) {
            contains = false;
            success = true;
        }
        else if (key != this.availableSlot && equals(key, containsRequest.getKey())) {
            contains = true;
            success = true;
        }
        else {
            for (int i = 1; i < this.bucketSize && !success; i++) {
                int linearProbeIndex = (bucketIndex + i) % this.bucketSize;
                key = this.keyArray[linearProbeIndex];
                if (key == null) {
                    contains = false;
                    success = true;
                }
                else if (key != this.availableSlot && equals(key, containsRequest.getKey())) {
                    contains = true;
                    success = true;
                }
                else {
                    success = false;
                }
            }
        }
        int requestNum = containsRequest.getRequestNum();
        DistributedMapContainsResponse response = new DistributedMapContainsResponse(requestNum, this.bucketNum, success, containsRequest.getKey(), contains);
        this.owner.tell(response, getSelf());
    }
    
    /**
     * Gets a value from a bucket based on a key
     * If another key value pair is occupying the designated bucketIndex it linearly probes to check if it's contained elsewhere
     * Linearly probes up until an empty entry missing a key value pair
     * Assumes no key value pairs to get exist beyond this empty entry as linear probing in addition would have filled it first
     * @param getRequest
     */
    protected void processGetRequest(DistributedMapBucketGetRequest getRequest) {
        boolean success = false;
        Object value = null;
        int bucketIndex = getRequest.getIndex() % this.bucketSize;
        Object key = this.keyArray[bucketIndex];
        if (key == null) {
            value = null;
            success = true;
        }
        else if (key != this.availableSlot && equals(key, getRequest.getKey())) {
            value = this.valueArray[bucketIndex];
            success = true;
        }
        else {
            for (int i = 1; i < this.bucketSize && !success; i++) {
                int linearProbeIndex = (bucketIndex + i) % this.bucketSize;
                key = this.keyArray[linearProbeIndex];
                if (key == null) {
                    value = null;
                    success = true;
                }
                else if (key != this.availableSlot && equals(key, getRequest.getKey())) {
                    value = this.valueArray[linearProbeIndex];
                    success = true;
                }
                else {
                    success = false;
                }
            }
        }
        int requestNum = getRequest.getRequestNum();
        DistributedMapGetResponse response = new DistributedMapGetResponse(requestNum, this.bucketNum, success, getRequest.getKey(), value);
        this.owner.tell(response, getSelf());
    }
    
    /**
     * Removes a key value pair that matches this key
     * If another key is occupying the designated bucketIndex it linearly probes to check if it's contained elsewhere
     * Linearly probes up until an empty entry missing a key value pair
     * Assumes no key value pairs to remove exist beyond this empty entry as linear probing in addition would have filled it first
     * @param removeRequest
     */
    protected void processRemoveRequest(DistributedMapBucketRemoveRequest removeRequest) {
        boolean success = false;
        Object value = null;
        int bucketIndex = removeRequest.getIndex() % this.bucketSize;
        Object key = this.keyArray[bucketIndex];
        if (key == null) {
            success = true;
        }
        else if (key != this.availableSlot && equals(key, removeRequest.getKey())) {
            value = this.valueArray[bucketIndex];
            this.keyArray[bucketIndex] = this.availableSlot;
            this.valueArray[bucketIndex] = this.availableSlot;
            this.entryCount--;
            success = true;
        }
        else {
            for (int i = 1; i < this.bucketSize && !success; i++) {
                int linearProbeIndex = (bucketIndex + i) % this.bucketSize;
                key = this.keyArray[linearProbeIndex];
                if (key == null) {
                    success = true;
                }
                else if (key != this.availableSlot && equals(key, removeRequest.getKey())) {
                    value = this.valueArray[linearProbeIndex];
                    this.keyArray[linearProbeIndex] = this.availableSlot;
                    this.valueArray[linearProbeIndex] = this.availableSlot;
                    this.entryCount--;
                    success = true;
                }
                else {
                    success = false;
                }
            }
        }
        int requestNum = removeRequest.getRequestNum();
        DistributedMapRemoveResponse response = new DistributedMapRemoveResponse(requestNum, this.bucketNum, success, removeRequest.getKey(), value);
        this.owner.tell(response, getSelf());
    }
    
    /**
     * Checks for equality between two keys
     * Handles conversion of Object to the appropriate key class
     * @param keyA
     * @param keyB
     * @return
     */
    private boolean equals(Object keyA, Object keyB) {
        boolean equals = (this.kClass.cast(keyA)).equals(this.kClass.cast(keyB));
        return equals;
    }
    
    /**
     * Sends back the contents of this bucket to the owner in an iterative fashion
     * @param request
     */
    protected void processRefactorGetRequest(DistributedMapRefactorGetRequest request) {
        for (int i = 0; i < this.bucketSize; i++) {
            Object key = this.keyArray[i];
            if (key != null && key != this.availableSlot) {
                Object value = this.valueArray[i];
                DistributedMapRefactorGetResponse response = new DistributedMapRefactorGetResponse(key, value);
                this.owner.tell(response, getSelf());
            }
        }
    }
    
    /**
     * Adds a key value pair back after refactoring
     * @param request
     */
    protected void processRefactorAddRequest(DistributedMapRefactorAddRequest additionRequest) {
        boolean success = false;
        Object value = null;
        if (this.bucketSize > this.entryCount) {
            int bucketIndex = additionRequest.getIndex() % this.bucketSize;
            Object key = this.keyArray[bucketIndex];
            if (key == null || key == this.availableSlot) {
                this.keyArray[bucketIndex] = additionRequest.getKey();
                this.valueArray[bucketIndex] = additionRequest.getValue();
                this.entryCount++;
                success = true;
            }
            else if (equals(key, additionRequest.getKey())) {
                value = this.valueArray[bucketIndex];
                this.valueArray[bucketIndex] = additionRequest.getValue();
                success = true;
            }
            else {
                for (int i = 1; i < this.bucketSize && !success; i++) {
                    int linearProbeIndex = (bucketIndex + i) % this.bucketSize;
                    key = this.keyArray[linearProbeIndex];
                    if (key == null || key == this.availableSlot) {
                        this.keyArray[linearProbeIndex] = additionRequest.getKey();
                        this.valueArray[linearProbeIndex] = additionRequest.getValue();
                        this.entryCount++;
                        success = true;
                    }
                    else if (equals(key, additionRequest.getKey())) {
                        value = this.valueArray[linearProbeIndex];
                        this.valueArray[linearProbeIndex] = additionRequest.getValue();
                        success = true;
                    }
                    else {
                        success = false;
                    }
                }
            }
        }
        else {
            success = false;
            value = additionRequest.getValue();
        }
        DistributedMapRefactorAddResponse response = new DistributedMapRefactorAddResponse(this.bucketNum, success, additionRequest.getKey(), value);
        this.owner.tell(response, getSelf());
    }
    
    /**
     * Returns the contents of this bucket as part of an iteration through the map
     */
    protected void processIterationRequest(DistributedMapIterationRequest request) {
        int requestNum = request.getRequestNum();
        for (int i = 0; i < this.bucketSize; i++) {
            Object key = this.keyArray[i];
            if (key != null && key != this.availableSlot) {
                Object value = this.valueArray[i];
                DistributedMapIterationResponse response = new DistributedMapIterationResponse(requestNum, this.bucketNum, key, value);
                this.owner.tell(response, getSelf());
            }
        }
    }
}
