package adt.frame;

import adt.impl.DistributedMapAdditionResponse;
import adt.impl.DistributedMapContainsResponse;
import adt.impl.DistributedMapGetResponse;
import adt.impl.DistributedMapIterationResponse;
import adt.impl.DistributedMapRemoveResponse;
import akka.actor.ActorRef;
import akka.actor.UntypedActorContext;
import peer.core.UniversalId;

public interface DistributedMap<K, V> {
    
    void initialise(Class<K> kClass, Class<V> vClass, UntypedActorContext actorContext, ActorRef owner, UniversalId peerId);
    
    int requestAdd(K k, V v);
    
    int requestContains(K k);
    
    int requestGet(K k);
    
    int requestRemove(K k);
    
    int requestIterator();
    
    public K getAddKey(DistributedMapAdditionResponse response);
    
    public V getAddValue(DistributedMapAdditionResponse response);
    
    public K getContainsKey(DistributedMapContainsResponse response);
    
    public boolean getContains(DistributedMapContainsResponse response);
    
    public K getGetKey(DistributedMapGetResponse response);
    
    public V getGetValue(DistributedMapGetResponse response);
    
    public K getRemoveKey(DistributedMapRemoveResponse response);
    
    public V getRemoveValue(DistributedMapRemoveResponse response);
    
    public K getIterationKey(DistributedMapIterationResponse response);
    
    public V getIterationValue(DistributedMapIterationResponse response);
}
